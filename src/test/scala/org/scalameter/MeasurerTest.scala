package org.scalameter

import org.scalatest.{FunSuite, Matchers}
import org.scalameter.execution.JvmRunner


abstract class MeasurerTest[M <: Measurer] extends FunSuite with Matchers {
  def measureWith(measurer: M)(snippet: => Any)(f: Double => Any): Any = {
    val ctx = measurer.prepareContext(Context.topLevel)
    val runner = new JvmRunner
    val dummy: Unit => Any = _ => ()

    measurer.beforeExecution(ctx)
    val result = runner.run(ctx) {
      measurer.measure(ctx, 1, dummy, dummy, () => (), (_: Unit) => snippet)
    }
    measurer.afterExecution(ctx)

    result.isSuccess should === (true)
    result.get.length should === (1)
    f(result.get.head)
  }
}
