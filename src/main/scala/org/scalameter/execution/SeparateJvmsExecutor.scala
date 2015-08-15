package org.scalameter
package execution



import org.scalameter.picklers.Pickler
import scala.collection._
import scala.util.Try
import org.scalameter.PrettyPrinter.Implicits._



/** Runs multiple JVM instances per each setup and aggregates all the results together.
 *
 *  This produces more stable results, as the performance related effects of each JVM instantiation
 *  are averaged.
 */
class SeparateJvmsExecutor[V: Pickler: PrettyPrinter](val warmer: Warmer, val aggregator: Aggregator[V],
  val measurer: Measurer[V]) extends Executor[V] {

  import Key._

  val runner = new JvmRunner

  def createJvmContext(ctx: Context) = {
    val existingFlags = ctx(exec.jvmflags)
    val flags = if (currentContext(Key.verbose)) "-verbose:gc" :: existingFlags else existingFlags
    ctx + (exec.jvmflags -> flags)
  }

  def runSetup[T](setup: Setup[T]): CurveData[V] = {
    import setup._

    val warmups = context(exec.maxWarmupRuns)
    val totalreps = context(exec.benchRuns)
    val independentSamples = context(exec.independentSamples)
    def repetitions(idx: Int): Int = {
      val is = independentSamples
      totalreps / is + (if (idx < totalreps % is) 1 else 0)
    }

    val m = measurer
    val w = warmer
    val jvmContext = createJvmContext(context)

    def sample(idx: Int, reps: Int): Try[Seq[(Parameters, Seq[(V, String)])]] = runner.run(jvmContext) {
      dyn.currentContext.value = context
      
      log.verbose(s"Sampling $reps measurements in separate JVM invocation $idx - ${context.scope}, ${context(dsl.curve)}.")

      // warmup
      setupBeforeAll()
      try {
        customwarmup match {
          case Some(warmup) =>
            for (i <- 0 until warmups) warmup()
          case _ =>
            for (x <- gen.warmupset) {
              for (i <- w.warming(context, setupFor(x), teardownFor(x))) snippet(x)
            }
        }

        // perform GC
        compat.Platform.collectGarbage()

        // measure
        val observations = new mutable.ArrayBuffer[(Parameters, Seq[(V, String)])]()
        for (params <- gen.dataset) {
          val set = setupFor()
          val tear = teardownFor()
          val regen = regenerateFor(params)
          val results = m.measure(context, reps, set, tear, regen, snippet)
          observations += ((params, results.map(q => q.value -> q.units))) // FIXME: workaround to `java.lang.ClassNotFoundException: org.scalameter.Quantity`
        }
        observations
      } finally {
        teardownAfterAll()
      }
    }

    def sampleReport(idx: Int, reps: Int): Seq[(Parameters, Seq[Quantity[V]])] = try {
      sample(idx, reps).get.map(v => (v._1, v._2.map(v => Quantity(v._1, v._2))))
    } catch {
      case t: Throwable =>
        log.error(s"Error running separate JVM: $t")
        log.error(s"Classpath: ${sys.props("java.class.path")}")
        throw t
    }

    log.verbose(s"Running test set for ${context.scope}, curve ${context(dsl.curve)}")
    log.verbose(s"Starting $totalreps measurements across $independentSamples independent JVM runs.")

    val valueseqs = for {
      idx <- 0 until independentSamples
      reps = repetitions(idx)
    } yield sampleReport(idx, reps)

    val valueseq = valueseqs.reduceLeft { (accseq, vs) =>
      accseq zip vs map {
        case ((k1, x), (k2, y)) => (k1, x ++ y)
      }
    }

    def nice(vs: Seq[(Parameters, Seq[Quantity[V]])]) = vs map { case (params, seq) =>
      params.axisData.mkString(", ") + ": " + seq.map(t => s"${t.value.prettyPrint}").mkString(", ")
    } mkString("\n")

    log.verbose(s"Obtained measurements:\n${nice(valueseq)}")

    val measurements = valueseq map {
      case (params, values) =>
        val single = aggregator(values)
        Measurement(
          single.value,
          params,
          aggregator.data(values),
          single.units
        )
    }

    CurveData(measurements.toSeq, Map.empty, context)
  }

  override def toString = s"MultipleJvmPerSetupExecutor(${aggregator.name}, ${measurer.name})"

}


object SeparateJvmsExecutor extends Executor.Factory[SeparateJvmsExecutor] {

  def apply[T: Pickler: PrettyPrinter](w: Warmer, agg: Aggregator[T], m: Measurer[T]) =
    new SeparateJvmsExecutor(w, agg, m)

}




























