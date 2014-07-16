package org.scalameter






class MeasureBuilder[T, U](
  val ctx: Context,
  val warmer: Warmer,
  val measurer: Measurer,
  val regen: () => T,
  val setup: T => Unit,
  val teardown: T => Unit,
  val resultFunction: Seq[Double] => U
) {
  def config(kvs: KeyValue*) = new MeasureBuilder(ctx ++ Context(kvs: _*), warmer, measurer, regen, setup, teardown, resultFunction)

  def withWarmer(w: Warmer) = new MeasureBuilder(ctx, w, measurer, regen, setup, teardown, resultFunction)

  def withMeasurer(m: Measurer) = new MeasureBuilder(ctx, warmer, m, regen, setup, teardown, resultFunction)

  def setUp(b: T => Unit) = new MeasureBuilder(ctx, warmer, measurer, regen, b, teardown, resultFunction)

  def tearDown(b: T => Unit) = new MeasureBuilder(ctx, warmer, measurer, regen, setup, b, resultFunction)

  def measureWith[S](b: T => S): U = {
    val oldctx = dyn.currentContext.value
    try {
      dyn.currentContext.value = ctx

      val x = regen()
      warmer match {
        case Warmer.Zero => // do nothing
        case _ =>
          for (i <- warmer.warming(ctx, () => setup(x), () => teardown(x))) b(x)
      }
  
      if (ctx(Key.exec.requireGC)) compat.Platform.collectGarbage()
  
      val measurements = measurer.measure[T, U](
        ctx, ctx(Key.exec.benchRuns),
        setup, teardown, regen, b
      )
  
      resultFunction(measurements)
    } finally {
      dyn.currentContext.value = oldctx
    }
  }

  def measure[S](b: =>S): U = {
    measureWith(_ => b)
  }
}


object MeasureBuilder {
  val doNothing = (u: Unit) => {}
  val unitRegen = () => {}
  val average: Seq[Double] => Double = ms => {
    var i = 0
    var sum = 0.0
    while (i < ms.length) {
      sum += ms(i)
      i += 1
    }
    sum / ms.length
  }

  val timeMeasurer = new Measurer.Default
}
