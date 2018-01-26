package org



import scala.collection._
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.language.existentials



package object scalameter extends MeasureBuilder[Unit, Double](
  Context.inlineBenchmarking,
  Warmer.Zero,
  MeasureBuilder.timeMeasurer,
  MeasureBuilder.unitRegen,
  MeasureBuilder.doNothing,
  MeasureBuilder.doNothing,
  MeasureBuilder.average
) {

  type KeyValue = (Key[T], T) forSome { type T }

  private[scalameter] object dyn {
    val currentContext = new MonadicDynVar(Context.topLevel)
    val log = new MonadicDynVar[Log](Log.Console)
    val events = new MonadicDynVar[Events](Events.None)
  }

  def currentContext: Context = dyn.currentContext.value

  def log: Log = dyn.log.value

  def events: Events = dyn.events.value

  /* misc */

  import utils.ClassPath

  @deprecated("Use utils.ClassPath.default", "0.5")
  def defaultClasspath = ClassPath.default.mkString

  @deprecated("Use utils.ClassPath.extract", "0.5")
  def extractClasspath(classLoader: ClassLoader, default: => String): String =
    ClassPath.extract(classLoader, default).mkString

  def withTestContext[U](ctx: Context, log: Log, handler: Events)(body: =>U) = {
    var res: U = null.asInstanceOf[U]
    for {
      _ <- dyn.log.using(log)
      _ <- dyn.events.using(handler)
      _ <- dyn.currentContext.using(ctx)
    } res = body
    res
  }
}


package scalameter {

  case class MeasurementData[T](complete: Seq[T], success: Boolean) {
    def avg(implicit num: Fractional[T]): T = {
      var sum = num.zero
      for (x <- complete) sum = num.plus(sum, x)
      sum = num.div(sum, num.fromInt(complete.length))
      sum
    }

    def stdev(implicit num: Fractional[T]): Double = {
      val mean = avg
      var sum = num.zero
      for (x <- complete) {
        val diff = num.minus(x, mean)
        sum = num.plus(sum, num.times(diff, diff))
      }
      sum = num.div(sum, num.fromInt(complete.length))
      math.sqrt(num.toDouble(sum))
    }
  }
  
}
