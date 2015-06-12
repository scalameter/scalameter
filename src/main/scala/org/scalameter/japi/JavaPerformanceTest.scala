package org.scalameter
package japi



import utils.Tree
import java.util.Date
import scala.util.DynamicVariable
import java.util.Arrays
import Key._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer



abstract class JavaPerformanceTest extends BasePerformanceTest with Serializable {
  import BasePerformanceTest._

  private val Group = classOf[org.scalameter.japi.Group]
  private val UsingInterface = classOf[org.scalameter.japi.Using[Object, Object]]

  final def warmer: org.scalameter.Warmer = javaWarmer.get

  final def aggregator: org.scalameter.Aggregator = javaAggregator.get

  final def executor: org.scalameter.Executor = javaExecutor.get

  final def measurer: org.scalameter.Measurer = javaMeasurer.get

  final def reporter: org.scalameter.Reporter = javaReporter.get

  final def persistor: org.scalameter.Persistor = javaPersistor.get

  def javaWarmer: org.scalameter.japi.Warmer

  def javaAggregator: org.scalameter.japi.Aggregator

  def javaExecutor: org.scalameter.japi.Executor

  def javaMeasurer: org.scalameter.japi.Measurer

  def javaPersistor: org.scalameter.japi.Persistor

  def javaReporter: org.scalameter.japi.Reporter

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
          val classGroupName = c.getName
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
                if (classOf[org.scalameter.japi.VoidGen] isAssignableFrom gen.getClass) {
                  setp = Some((v: Object) => { m.invokeA(instance, null) })
                } else {
                  setp = Some((v: Object) => { m.invokeA(instance, v) })
                }
              }
              case "teardown" => {
                if (classOf[org.scalameter.japi.VoidGen] isAssignableFrom gen.getClass) {
                  teardown = Some((v: Object) => { m.invokeA(instance, null) })
                } else {
                  teardown = Some((v: Object) => { m.invokeA(instance, v) })
                }
              }
              case _ =>
            }
          }

          var snippet = (s: Object) => { snippetMethod.invokeA(instance, s) }
          if (classOf[org.scalameter.japi.VoidGen] isAssignableFrom gen.getClass) {
            snippet = (s: Object) => { snippetMethod.invokeA(instance, null) }
          }
          val generator = gen.get
          val context = setupzipper.value.current.context ++ kvs
          val setup = Setup(context, generator.asInstanceOf[Gen[Object]], setupbeforeall, teardownafterall, setp, teardown, None, snippet, executor)
          setupzipper.value = setupzipper.value.addItem(setup)
        case _ =>
          // ignore, does not contain any benchmark-related information
      }
    }
  }

  def executeTests(): Boolean = {
    val datestart: Option[Date] = Some(new Date)
    val rawsetuptree = setupzipper.value.result
    val setuptree = rawsetuptree.filter(setupFilter)
    val resulttree = executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]], reporter, persistor)
    val dateend: Option[Date] = Some(new Date)
    val datedtree = resulttree.copy(context = resulttree.context + (Key.reports.startDate -> datestart) + (Key.reports.endDate -> dateend))
    reporter.report(datedtree, persistor)
  }

  private def setupFilter(setup: Setup[_]): Boolean = {
    val sf = currentContext(Key.scopeFilter)
    val fullname = setup.context.scope + "." + setup.context.curve
    val regex = sf.r
    regex.findFirstIn(fullname) != None
  }

}


abstract class QuickBenchmark extends JavaPerformanceTest {
  def javaReporter: org.scalameter.japi.Reporter = new org.scalameter.japi.LoggingReporter

  def javaPersistor: org.scalameter.japi.Persistor = new org.scalameter.japi.NonePersistor

  def javaExecutor = new org.scalameter.japi.LocalExecutor(javaWarmer, javaAggregator, javaMeasurer)

  def javaMeasurer = new org.scalameter.japi.Measurer {
    def get = new org.scalameter.Executor.Measurer.Default()
  }

  def javaWarmer = new org.scalameter.japi.Warmer {
    def get = new org.scalameter.Executor.Warmer.Default()
  }

  def javaAggregator = new org.scalameter.japi.Aggregator {
    def get = org.scalameter.Aggregator.min
  }
}


abstract class Microbenchmark extends JavaPerformanceTest {
  import Executor.Measurer
  def javaWarmer = new org.scalameter.japi.Warmer {
    def get = new org.scalameter.Warmer.Default
  }
  def javaAggregator = new org.scalameter.japi.MinAggregator
  def javaMeasurer = new org.scalameter.japi.Measurer {
    def get = new org.scalameter.Measurer.IgnoringGC with org.scalameter.Measurer.PeriodicReinstantiation {
      override val defaultFrequency = 12
      override val defaultFullGC = true
    }
  }
  def javaExecutor = new org.scalameter.japi.Executor {
    def get = execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
  }
  def javaReporter: org.scalameter.japi.Reporter = new org.scalameter.japi.LoggingReporter
  def javaPersistor: org.scalameter.japi.Persistor = new org.scalameter.japi.NonePersistor
}


abstract class HTMLReport extends JavaPerformanceTest {
  import Executor.Measurer
  import reporting._
  def javaPersistor: org.scalameter.japi.Persistor = new org.scalameter.japi.GZIPJSONSerializationPersistor
  def javaWarmer = new org.scalameter.japi.Warmer {
    def get = new org.scalameter.Warmer.Default
  }
  def javaAggregator = new org.scalameter.japi.AverageAggregator
  def javaMeasurer = new org.scalameter.japi.Measurer {
    def get = new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise
  }
  def javaExecutor = new org.scalameter.japi.Executor {
    def get = new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
  }
  def online: Boolean
  def javaTester: org.scalameter.japi.RegressionReporterTester
  def javaHistorian: org.scalameter.japi.RegressionReporterHistorian
  def javaReporter: org.scalameter.japi.Reporter = new org.scalameter.japi.Reporter {
    def get = new org.scalameter.Reporter.Composite(
      new RegressionReporter(javaTester.get, javaHistorian.get),
      HtmlReporter(false)
    )
  }
}


abstract class OnlineRegressionReport extends HTMLReport {
  import reporting._
  def javaTester: org.scalameter.japi.RegressionReporterTester = new org.scalameter.japi.OverlapIntervalsTester()
  def javaHistorian: org.scalameter.japi.RegressionReporterHistorian = new org.scalameter.japi.ExponentialBackoffHistorian()
  def online = true
}


abstract class OfflineRegressionReport extends HTMLReport {
  import reporting._
  def javaTester: org.scalameter.japi.RegressionReporterTester = new org.scalameter.japi.OverlapIntervalsTester()
  def javaHistorian: org.scalameter.japi.RegressionReporterHistorian = new org.scalameter.japi.ExponentialBackoffHistorian()
  def online = false
}


abstract class OfflineReport extends HTMLReport {
  import reporting._
  def javaTester: org.scalameter.japi.RegressionReporterTester = new org.scalameter.japi.AccepterTester()
  def javaHistorian: org.scalameter.japi.RegressionReporterHistorian = new org.scalameter.japi.ExponentialBackoffHistorian()
  def online = false
}
