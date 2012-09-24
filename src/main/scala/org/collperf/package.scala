package org



import java.util.Date
import collection._



package object collperf {

}


package collperf {

  object Key {
    val module = "module"
    val method = "method"
  }

  case class Context(properties: immutable.Map[String, Any]) {
    def +(t: (String, Any)) = Context(properties + t)
  }

  object Context {
    val empty = Context(immutable.Map())
  }

  case class Parameters(axisData: immutable.Map[String, Any]) {
    def ++(that: Parameters) = Parameters(this.axisData ++ that.axisData)
  }

  object Parameters {
    def apply(xs: (String, Any)*) = new Parameters(immutable.Map(xs: _*))
  }

  case class Measurement(time: Long, params: Parameters)

  case class Result(measurements: Seq[Measurement], context: Context)

  case class History(results: Seq[(Date, Result)])

  case class Benchmark[T](context: Context, gen: Gen[T], setup: Option[T => Any], teardown: Option[T => Any], customwarmup: Option[() => Any], snippet: T => Any)

  trait Reporter {

    def report(result: Result, persistor: Persistor): Unit

  }

  trait Persistor {

    def load(context: Context): History

    def save(result: Result): Unit

  }

}