package org.scalameter






class MeasureBuilder[T, U](
  val ctx: Context,
  val warmer: Warmer,
  val regen: () => T,
  val setup: T => Unit,
  val teardown: T => Unit,
  val resultFunction: Seq[Double] => U
) {
  def config(kvs: KeyValue*) = new MeasureBuilder(ctx ++ Context(kvs: _*), warmer, regen, setup, teardown, resultFunction)

  def withWarmer(w: Warmer) = new MeasureBuilder(ctx, w, regen, setup, teardown, resultFunction)

  def setUp(b: T => Unit) = new MeasureBuilder(ctx, warmer, regen, b, teardown, resultFunction)

  def tearDown(b: T => Unit) = new MeasureBuilder(ctx, warmer, regen, setup, b, resultFunction)

  def measure[S](measurer: Measurer = MeasureBuilder.timeMeasurer)(b: T => S): U = {
    val x = regen()
    warmer match {
      case Warmer.Zero => // do nothing
      case _ => warmer.warming(ctx, () => setup(x), () => teardown(x))
    }

    if (ctx(Key.exec.requireGC)) compat.Platform.collectGarbage()

    val measurements = measurer.measure[T, U](
      ctx, ctx(Key.exec.benchRuns),
      setup, teardown, regen, b
    )

    resultFunction(measurements)
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
