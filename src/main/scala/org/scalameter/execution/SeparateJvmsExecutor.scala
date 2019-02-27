package org.scalameter
package execution



import org.scalameter.picklers.Pickler
import scala.collection._
import scala.util.Try
import org.scalameter.PrettyPrinter.Implicits._
import org.scalameter.utils.Tree



/** Runs multiple JVM instances per each setup and aggregates all the results together.
 *
 *  This produces more stable results, as the performance related effects of each JVM
 *  instantiation are averaged.
 */
class SeparateJvmsExecutor[V: Pickler : PrettyPrinter](
  val warmer: Warmer,
  val aggregator: Aggregator[V],
  val measurer: Measurer[V]
) extends Executor[V] {
  import Key._

  val runner = new JvmRunner

  def createJvmContext(ctx: Context) = {
    ctx ++ Seq(
      exec.overallBegin -> currentContext(exec.overallBegin)
    )
  }

  override def run[T](
    setuptree: Tree[Setup[T]],
    reporter: Reporter[V],
    persistor: Persistor
  ): Tree[CurveData[V]] = {
    var count = 0
    for (setup <- setuptree) {
      count += 1
    }
    var result: Tree[CurveData[V]] = null
    val newContext = currentContext ++ Seq(
      exec.setupCount -> count,
      exec.setupIndex -> 0
    )
    for (_ <- dyn.currentContext.using(newContext)) {
      result = super.run(setuptree, reporter, persistor)
    }
    log.clear()
    result
  }

  def runSetup[T](setup: Setup[T]): CurveData[V] = {
    import setup._
    import SeparateJvmsExecutor.computeOverallProgress

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

    log.currentTotalForks(independentSamples)

    def sample(idx: Int, reps: Int): Try[Seq[(Parameters, Seq[(V, String)])]] = {
      val forkCommand = runner.commandFor(jvmContext).mkString(" ")
      val setupIndex = currentContext(exec.setupIndex)
      val setupCount = currentContext(exec.setupCount)
      runner.run(jvmContext) {
        dyn.currentContext.value = jvmContext
        val progress = computeOverallProgress(
          idx, independentSamples, setupIndex, setupCount, 0.0)
        log.overallBegin(jvmContext(exec.overallBegin))
        log.overallScope(jvmContext.scope + " \ud83e\udc7a " + jvmContext(dsl.curve))
        log.overallProgress(progress)
        log.currentForkIndex(idx)
        log.currentTotalForks(independentSamples)
        log.currentForkCommand(forkCommand)

        log.info(
          s"Sampling $reps measurements in separate JVM invocation $idx - " +
            s"${jvmContext.scope}, ${jvmContext(dsl.curve)}."
        )

        // warmup
        setupBeforeAll()
        try {
          log.timer(true)
          customwarmup match {
            case Some(warmup) =>
              log.currentInput("Running custom warmup ...")
              for (i <- 0 until warmups) warmup()
            case _ =>
              log.currentInput("Running warmup ...")
              for (x <- gen.warmupset) {
                for (i <- w.warming(jvmContext, setupFor(x), teardownFor(x))) snippet(x)
              }
          }
          log.currentProgress(10.0)

          // perform GC
          compat.Platform.collectGarbage()

          // measure
          val observations = new mutable.ArrayBuffer[(Parameters, Seq[(V, String)])]()
          val totalDatasets = gen.cardinality
          var datasetIndex = 0
          for (params <- gen.dataset) {
            datasetIndex += 1
            val set = setupFor()
            val tear = teardownFor()
            val regen = regenerateFor(params)
            val currentProgress = 10.0 + 80.0 * datasetIndex / totalDatasets
            val overallProgress = computeOverallProgress(
              idx, independentSamples, setupIndex, setupCount, currentProgress)
            log.currentInput(params.toString)
            log.currentProgress(currentProgress)
            log.overallProgress(overallProgress)
            val results = m.measure(context, reps, set, tear, regen, snippet)
            observations += ((params, results.map(q => q.value -> q.units)))
            // FIXME: `java.lang.ClassNotFoundException: org.scalameter.Quantity`
          }
          observations
        } finally {
          teardownAfterAll()
          log.timer(false)
          log.clear()
        }
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
    log.verbose(
      s"Starting $totalreps measurements across " +
        s"$independentSamples independent JVM runs."
    )

    val valueseqs = for {
      idx <- 0 until independentSamples
      reps = repetitions(idx)
    } yield {
      val setupIndex = currentContext(exec.setupIndex)
      val setupCount = currentContext(exec.setupCount)
      log.currentForkIndex(idx)
      log.overallProgress(
        computeOverallProgress(idx, independentSamples, setupIndex, setupCount, 0.0))
      sampleReport(idx, reps)
    }

    val valueseq = valueseqs.reduceLeft { (accseq, vs) =>
      accseq zip vs map {
        case ((k1, x), (k2, y)) => (k1, x ++ y)
      }
    }

    def nice(vs: Seq[(Parameters, Seq[Quantity[V]])]) = vs map { case (params, seq) =>
      params.axisData.mkString(", ") + ": " +
        seq.map(t => s"${t.value.prettyPrint}").mkString(", ")
    } mkString ("\n")

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

    dyn.currentContext.value = currentContext ++ Seq(
      exec.setupIndex -> (currentContext(exec.setupIndex) + 1)
    )

    CurveData(measurements.toSeq, Map.empty, context)
  }

  override def toString =
    s"SeparateJvmsExecutor(${aggregator.name}, ${measurer.name})"

}


object SeparateJvmsExecutor extends Executor.Factory[SeparateJvmsExecutor] {

  def apply[T: Pickler : PrettyPrinter](w: Warmer, agg: Aggregator[T], m: Measurer[T]) =
    new SeparateJvmsExecutor(w, agg, m)

  private[execution] def computeOverallProgress(
    forkIndex: Int, totalForks: Int, setupIndex: Int, setupCount: Int, currentProgress: Double
  ): Double = {
    val fraction = math.max(0.0, math.min(1.0, currentProgress / 100.0))
    (setupIndex + (forkIndex + fraction).toDouble / totalForks) / setupCount * 100
  }
}
