package org.scalameter
package deprecatedjapi


import org.scalameter.japi.SerializableMethod
import scala.collection.JavaConverters._



abstract class JavaPerformanceTest[U] extends BasePerformanceTest[U] with Serializable {
  import BasePerformanceTest._

  private val Group = classOf[org.scalameter.deprecatedjapi.Group]
  private val UsingInterface = classOf[org.scalameter.deprecatedjapi.Using[Object, Object]]

  final def warmer: org.scalameter.Warmer = javaWarmer.get

  final def aggregator: org.scalameter.Aggregator[U] = javaAggregator.get

  final def executor: org.scalameter.Executor[U] = javaExecutor.get

  final def measurer: org.scalameter.Measurer[U] = javaMeasurer.get

  final def reporter: org.scalameter.Reporter[U] = javaReporter.get

  final def persistor: org.scalameter.Persistor = javaPersistor.get

  def javaWarmer: org.scalameter.deprecatedjapi.Warmer

  def javaAggregator: org.scalameter.deprecatedjapi.Aggregator[U]

  def javaExecutor: org.scalameter.deprecatedjapi.Executor[U]

  def javaMeasurer: org.scalameter.deprecatedjapi.Measurer[U]

  def javaPersistor: org.scalameter.deprecatedjapi.Persistor

  def javaReporter: org.scalameter.deprecatedjapi.Reporter[U]

  type SameType

  constructScope(this, this.getClass)

  def getClassInstance(enclosing: Object, s: String): Object = {
    val clss = Class.forName(s)
    var outerClasses = clss.getEnclosingClass()
    if (outerClasses == null) {
      Class.forName(s).newInstance.asInstanceOf[Object]
    } else {
      val ctor = Class.forName(s).getDeclaredConstructors()(0)
      ctor.newInstance(enclosing).asInstanceOf[Object]
    }
  }

  def config(instance: Object, c: Class[_]): List[KeyValue] = {
    val fields = c.getDeclaredFields
    fields.find(_.getName == "config") match {
      case None =>
        // println(s"no config found in $c")
        List()
      case Some(f) =>
        val jcontext = f.get(instance).asInstanceOf[JContext]
        val kvs = for ((kname, value) <- jcontext.getKeyMap.asScala) yield {
          val key = org.scalameter.Key.parseKey(kname)
          (key, value).asInstanceOf[KeyValue]
        }
        kvs.toList
    }
  }

  def isGroupOrUsing(c: Class[_]) = Group.isAssignableFrom(c) || UsingInterface.isAssignableFrom(c)

  def constructScope(instance: Object, c: Class[_]): Unit = {
    for (clzz <- c.getClasses() if isGroupOrUsing(clzz)) {
      classScope(getClassInstance(this, clzz.getName), clzz)
    }
  }

  def classScope(instance: Object, c: Class[_]): Unit = {
    for (interface <- c.getInterfaces) {
      interface match {
        case Group =>
          val classGroupName = c.getName
          val kvs = config(instance, c)
          val s = Scope(classGroupName, setupzipper.value.current.context).config(kvs: _*)
          val oldscope = s.context(Key.dsl.scope)
          val ct = s.context + (Key.dsl.scope -> (c.getSimpleName() :: oldscope))

          setupzipper.value = setupzipper.value.descend.setContext(ct)
          for (clzz <- c.getClasses if isGroupOrUsing(clzz)) {
            classScope(getClassInstance(instance, clzz.getName), clzz)
          }
          setupzipper.value = setupzipper.value.ascend
        case UsingInterface =>
          val kvs = config(instance, c)
          val snippetMethod = new SerializableMethod(c.getMethod("snippet", classOf[Object]))

          var setupbeforeall: Option[() => Unit] = None
          var teardownafterall: Option[() => Unit] = None
          var setp: Option[Object => Any] = None
          var teardown: Option[Object => Any] = None
          val gen = c.getMethod("generator").invoke(instance).asInstanceOf[JavaGenerator[Any]]
          for (ms <- c.getMethods) {
            val m = new SerializableMethod(ms)
            ms.getName match {
              case "beforeTests" => {
                setupbeforeall = Some(() => { m.invoke(instance) })
              }
              case "afterTests" => teardownafterall = Some(() => { m.invoke(instance) })
              case "setup" => {
                if (classOf[org.scalameter.deprecatedjapi.VoidGen] isAssignableFrom gen.getClass) {
                  setp = Some((v: Object) => { m.invokeA(instance, null) })
                } else {
                  setp = Some((v: Object) => { m.invokeA(instance, v) })
                }
              }
              case "teardown" => {
                if (classOf[org.scalameter.deprecatedjapi.VoidGen] isAssignableFrom gen.getClass) {
                  teardown = Some((v: Object) => { m.invokeA(instance, null) })
                } else {
                  teardown = Some((v: Object) => { m.invokeA(instance, v) })
                }
              }
              case _ =>
            }
          }

          var snippet = (s: Object) => { snippetMethod.invokeA(instance, s) }
          if (classOf[org.scalameter.deprecatedjapi.VoidGen] isAssignableFrom gen.getClass) {
            snippet = (s: Object) => { snippetMethod.invokeA(instance, null) }
          }
          val generator = gen.get
          val context = setupzipper.value.current.context ++ kvs
          val setup = Setup(context, generator.asInstanceOf[Gen[Object]], setupbeforeall, teardownafterall, setp, teardown, None, snippet)
          setupzipper.value = setupzipper.value.addItem(setup)
        case _ =>
          // ignore, does not contain any benchmark-related information
      }
    }
  }

}


abstract class QuickBenchmark extends JavaPerformanceTest[Double] {
  def javaReporter: org.scalameter.deprecatedjapi.Reporter[Double] = new org.scalameter.deprecatedjapi.LoggingReporter

  def javaPersistor: org.scalameter.deprecatedjapi.Persistor = new org.scalameter.deprecatedjapi.NonePersistor

  def javaExecutor: org.scalameter.deprecatedjapi.Executor[Double] =
    new org.scalameter.deprecatedjapi.LocalExecutor(javaWarmer, javaAggregator, javaMeasurer)

  def javaMeasurer: org.scalameter.deprecatedjapi.Measurer[Double] = new DefaultMeasurer

  def javaWarmer = new org.scalameter.deprecatedjapi.Warmer {
    def get = new org.scalameter.Executor.Warmer.Default()
  }

  def javaAggregator: org.scalameter.deprecatedjapi.Aggregator[Double] = new MinAggregator
}


abstract class Microbenchmark extends JavaPerformanceTest[Double] {
  def javaWarmer = new org.scalameter.deprecatedjapi.Warmer {
    def get = new org.scalameter.Warmer.Default
  }
  def javaAggregator: org.scalameter.deprecatedjapi.Aggregator[Double] = new org.scalameter.deprecatedjapi.MinAggregator[Double]
  def javaMeasurer: org.scalameter.deprecatedjapi.Measurer[Double] = new org.scalameter.deprecatedjapi.Measurer[Double] {
    def get = new org.scalameter.Measurer.IgnoringGC with org.scalameter.Measurer.PeriodicReinstantiation[Double] {
      override val defaultFrequency = 12
      override val defaultFullGC = true
    }.asInstanceOf[Executor.Measurer[Double]]
  }
  def javaExecutor: org.scalameter.deprecatedjapi.Executor[Double] =
    new org.scalameter.deprecatedjapi.SeparateJvmsExecutor(javaWarmer, javaAggregator, javaMeasurer)
  def javaReporter: org.scalameter.deprecatedjapi.Reporter[Double] = new org.scalameter.deprecatedjapi.LoggingReporter[Double]
  def javaPersistor: org.scalameter.deprecatedjapi.Persistor = new org.scalameter.deprecatedjapi.NonePersistor
}


abstract class HTMLReport extends JavaPerformanceTest[Double] {
  import Executor.Measurer
  import reporting._
  def javaPersistor: org.scalameter.deprecatedjapi.Persistor = new org.scalameter.deprecatedjapi.GZIPJSONSerializationPersistor
  def javaWarmer = new org.scalameter.deprecatedjapi.Warmer {
    def get = new org.scalameter.Warmer.Default
  }
  def javaAggregator: org.scalameter.deprecatedjapi.Aggregator[Double] = new org.scalameter.deprecatedjapi.AverageAggregator
  def javaMeasurer: org.scalameter.deprecatedjapi.Measurer[Double] = new org.scalameter.deprecatedjapi.Measurer[Double] {
    def get = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation[Double]
      with Measurer.OutlierElimination[Double] with Measurer.RelativeNoise {
      def numeric: Numeric[Double] = implicitly[Numeric[Double]]
    }
  }
  def javaExecutor: org.scalameter.deprecatedjapi.Executor[Double] =
    new org.scalameter.deprecatedjapi.SeparateJvmsExecutor(javaWarmer, javaAggregator, javaMeasurer)
  def online: Boolean
  def javaTester: org.scalameter.deprecatedjapi.RegressionReporterTester
  def javaHistorian: org.scalameter.deprecatedjapi.RegressionReporterHistorian
  def javaReporter: org.scalameter.deprecatedjapi.Reporter[Double] = new org.scalameter.deprecatedjapi.Reporter[Double] {
    def get = new org.scalameter.Reporter.Composite(
      new RegressionReporter[Double](javaTester.get, javaHistorian.get),
      HtmlReporter(false)
    )
  }
}


abstract class OnlineRegressionReport extends HTMLReport {
  def javaTester: org.scalameter.deprecatedjapi.RegressionReporterTester = new org.scalameter.deprecatedjapi.OverlapIntervalsTester()
  def javaHistorian: org.scalameter.deprecatedjapi.RegressionReporterHistorian = new org.scalameter.deprecatedjapi.ExponentialBackoffHistorian()
  def online = true
}


abstract class OfflineRegressionReport extends HTMLReport {
  def javaTester: org.scalameter.deprecatedjapi.RegressionReporterTester = new org.scalameter.deprecatedjapi.OverlapIntervalsTester()
  def javaHistorian: org.scalameter.deprecatedjapi.RegressionReporterHistorian = new org.scalameter.deprecatedjapi.ExponentialBackoffHistorian()
  def online = false
}


abstract class OfflineReport extends HTMLReport {
  def javaTester: org.scalameter.deprecatedjapi.RegressionReporterTester = new org.scalameter.deprecatedjapi.AccepterTester()
  def javaHistorian: org.scalameter.deprecatedjapi.RegressionReporterHistorian = new org.scalameter.deprecatedjapi.ExponentialBackoffHistorian()
  def online = false
}
