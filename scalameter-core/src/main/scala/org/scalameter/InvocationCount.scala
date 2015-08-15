package org.scalameter

import java.io.File
import org.scalameter.Key._
import org.scalameter.execution.invocation.InvocationCountMatcher
import org.scalameter.execution.invocation.instrumentation.{Instrumentation, MethodInvocationCounter, MethodSignature}
import scala.collection.{Seq, mutable}


/** Mixin for all [[org.scalameter.Measurer]] implementations that perform any kind of
 *  method invocation counting.
 */
trait InvocationCount extends Measurer[Map[String, Long]] {
  def matcher: InvocationCountMatcher

  def measure[T](context: Context, measurements: Int, setup: (T) => Any,
    tear: (T) => Any, regen: () => T, snippet: (T) => Any):
  Seq[Quantity[Map[String, Long]]] = {
    val invocations = mutable.ListBuffer.empty[Quantity[Map[String, Long]]]
    var obj: Any = null.asInstanceOf[Any]
    val methodTable = context.goe(exec.measurers.methodInvocationLookupTable,
      sys.error("Measurer.prepareContext should be called before Measurer.measure"))
    val numMethods = methodTable.length

    def measureSnippet(value: T): Any = {
      MethodInvocationCounter.setup(numMethods)
      setup(value)

      MethodInvocationCounter.start()
      val obj = snippet(value)
      MethodInvocationCounter.stop()
      tear(value)

      invocations += Quantity(
        methodTable.map(_.toString).zip(MethodInvocationCounter.counts())
          .groupBy(_._1).map { case (k, v) =>
          k -> v.iterator.map(_._2).sum
        }(collection.breakOut), "#"
      )
      obj
    }

    if (context(exec.assumeDeterministicRun)) {
      obj = measureSnippet(regen())
      val count = invocations.head
      invocations ++= List.fill(measurements - 1)(count)
    } else {
      var iteration = 0
      while (iteration < measurements) {
        obj = measureSnippet(regen())
        iteration += 1
      }
    }

    log.verbose("Measurements: " + invocations.mkString(", "))
    invocations.result()
  }

  override def usesInstrumentedClasspath: Boolean = true

  /** Creates the [[Key.exec.measurers.instrumentedJarPath]] with an abstract temporary
   *  file, the [[Key.exec.measurers.methodInvocationLookupTable]] with an empty
   *  [[scala.collection.mutable.AbstractBuffer]], and the [[Key.finalClasspath]]
   *  with a classpath that consists of an instrumented jar and the [[Key.classpath]].
   *
   *  @param context [[org.scalameter.Context]] that should the setup tree context
   */

  override def prepareContext(context: Context): Context = {
    val cl = context(classpath)
    val jar = File.createTempFile(s"scalameter-bench-", "-instrumented.jar")
    jar.deleteOnExit()

    context ++ Context(
      exec.measurers.methodInvocationLookupTable ->
        mutable.ArrayBuffer.empty[MethodSignature],
      exec.measurers.instrumentedJarPath -> jar,
      finalClasspath -> (jar +: cl)
    )
  }

  /** Creates a jar with instrumented classes under the location pointed by
   *  [[Key.exec.measurers.instrumentedJarPath]], and saves the internal method lookup
   *  table under the [[Key.exec.measurers.methodInvocationLookupTable]].
   *
   *  @param context [[org.scalameter.Context]] that should be a result of the
   *                 [[prepareContext]]
   */
  override def beforeExecution(context: Context) = {
    val jar = context.goe(exec.measurers.instrumentedJarPath,
      sys.error(
        "Measurer.beforeExecution should be called after Measurer.prepareContext"))
    val lookupTable = context.goe(exec.measurers.methodInvocationLookupTable,
      sys.error(
        "Measurer.beforeExecution should be called after Measurer.prepareContext"))

    lookupTable ++= Instrumentation.writeInstrumentedClasses(
      ctx = context, matcher = matcher, to = jar)
  }

  /** Removes instrumented jar from filesystem.
   *
   *  @param context [[org.scalameter.Context]] that should be a result of the
   *                 [[prepareContext]]
   */
  override def afterExecution(context: Context) = {
    val jar = context.goe(exec.measurers.instrumentedJarPath,
      sys.error(
        "Measurer.afterExecution should be called after Measurer.prepareContext"))
    jar.delete()
  }
}
