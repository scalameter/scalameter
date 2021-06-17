package org.scalameter


import scala.collection.Seq



class MeasureBuilder[T, U](
  val ctx: Context,
  val warmer: Warmer,
  val measurer: Measurer[U],
  val regen: () => T,
  val setup: T => Unit,
  val teardown: T => Unit,
  val resultFunction: Seq[Quantity[U]] => Quantity[U]
) {
  def config(kvs: KeyValue[_]*) = new MeasureBuilder(ctx ++ Context(kvs: _*), warmer,
    measurer, regen, setup, teardown, resultFunction)

  def withWarmer(w: Warmer) =
    new MeasureBuilder(ctx, w, measurer, regen, setup, teardown, resultFunction)

  def withMeasurer(m: Measurer[U]) =
    new MeasureBuilder(ctx, warmer, m, regen, setup, teardown, resultFunction)

  def withMeasurer[V](m: Measurer[V], a: Seq[Quantity[V]] => Quantity[V]) =
    new MeasureBuilder(ctx, warmer, m, regen, setup, teardown, a)

  def setUp(b: T => Unit) =
    new MeasureBuilder(ctx, warmer, measurer, regen, b, teardown, resultFunction)

  def tearDown(b: T => Unit) =
    new MeasureBuilder(ctx, warmer, measurer, regen, setup, b, resultFunction)

  def measureWith[S](b: T => S): Quantity[U] = measuredWith[S](b)._2

  def measuredWith[S](b: T => S): (S, Quantity[U]) = {
    val oldctx = dyn.currentContext.value
    try {
      dyn.currentContext.value = ctx

      val x = regen()
      var result = null.asInstanceOf[S]
      warmer match {
        case Warmer.Zero => // do nothing
        case _ =>
          for (i <- warmer.warming(ctx, () => setup(x), () => teardown(x))) b(x)
      }
  
      if (ctx(Key.exec.requireGC)) compat.Platform.collectGarbage()
  
      val measurements = measurer.measure[T](
        ctx, ctx(Key.exec.benchRuns),
        setup, teardown, regen, { t => result = b(t); result }
      )
  
      (result, resultFunction(measurements))
    } finally {
      dyn.currentContext.value = oldctx
    }
  }

  def measure[S](b: =>S): Quantity[U] = measured(b)._2

  def measured[S](b: =>S): (S, Quantity[U]) = {
    measuredWith(_ => b)
  }
}


object MeasureBuilder {
  val doNothing = (u: Unit) => {}
  val unitRegen = () => {}
  val average = Aggregator.average
  val timeMeasurer = new Measurer.Default
}
