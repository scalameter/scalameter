package org.scalameter
import org.scalameter.javaApi.Group
import org.scalameter.execution.LocalExecutor
import org.scalameter.Executor.Measurer
import org.scalameter.reporting.LoggingReporter
import utils.Tree
import java.util.Date
import scala.util.DynamicVariable
import java.util.Arrays
import java.util.HashMap
import Key._
import org.scalameter.javaApi.JavaGenerator
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

abstract class JavaPerformanceTest extends DSL with Serializable{

  //usefull for match on class interface
  val Group = classOf[org.scalameter.javaApi.Group]
  val UsingInterface = classOf[org.scalameter.javaApi.Using[java.lang.Object, java.lang.Object]]
  def defaultConfig: Context = Context.empty

  def executor: Executor = javaExecutor.get
  def measurer: Measurer = javaMeasurer.get
  def reporter: Reporter = javaReporter.get
  def persistor = javaPersistor.get

  def javaExecutor: org.scalameter.javaApi.Executor
  def javaMeasurer: org.scalameter.javaApi.Measurer
  def javaPersistor: org.scalameter.javaApi.Persistor
  def javaReporter: org.scalameter.javaApi.Reporter
  type SameType

  constructScope(this.getClass)

  def getClassInstance(s: String): Object = {
    val clss = Class.forName(s)
    var outerClasses = clss.getEnclosingClass()
    if (outerClasses == null) {
      Class.forName(s).newInstance.asInstanceOf[Object]
    } else {
      val obj = getClassInstance(outerClasses.getName)
      val c = Class.forName(s).getDeclaredConstructors()(0)
      c.newInstance(obj.asInstanceOf[Object]).asInstanceOf[Object]
    }
  }

  def config(c: Class[_]): List[KeyValue] = {
    val methods = c.getMethods
    val instance = getClassInstance(c.getName)
    for (m <- methods) {
      if (m.getName.equals("config")) {
        val config = m.invoke(instance).asInstanceOf[HashMap[javaApi.exec, Any]]
        val keys = config.keySet
        var kv: List[KeyValue] = List()
        for (k <- keys.toArray) {
          k match {
            case javaApi.exec.benchRuns => {
              config.get(k) match {
                case d: Double => kv = (exec.benchRuns -> d.toInt) :: kv
                case i: Int => kv = (exec.benchRuns -> i) :: kv
                case _ =>
              }
            }
            case javaApi.exec.independentSamples => {
              config.get(k) match {
                case d: Double => kv = (exec.independentSamples -> d.toInt) :: kv
                case i: Int => kv = (exec.independentSamples -> i) :: kv
                case _ =>
              }
            }
            case javaApi.exec.maxWarmupRuns => {
              config.get(k) match {
                case d: Double => kv = (exec.maxWarmupRuns -> d.toInt) :: kv
                case i: Int => kv = (exec.maxWarmupRuns -> i) :: kv
                case _ =>
              }
            }
            case javaApi.exec.minWarmupRuns => {
              config.get(k) match {
                case d: Double => kv = (exec.minWarmupRuns -> d.toInt) :: kv
                case i: Int => kv = (exec.minWarmupRuns -> i) :: kv
                case _ =>
              }
            }
            case javaApi.exec.warmupCovThreshold => {
              config.get(k) match {
                case d: Double => kv = (exec.warmupCovThreshold -> d) :: kv
                case i: Int => kv = (exec.warmupCovThreshold -> i.toDouble) :: kv
                case _ =>
              }
            }
            case javaApi.exec.jvmflags => {
              config.get(k) match {
                case s: String => kv = (exec.jvmflags -> s) :: kv
                case _ =>
              }
            }
            case javaApi.exec.jvmcmd => config.get(k) match {
              case s: String => kv = (exec.jvmcmd -> s) :: kv
              case _ =>
            }
            case javaApi.exec.outliersCovMultiplier => config.get(k) match {
              case i: Int => kv = (exec.outliers.covMultiplier -> i.toDouble) :: kv
              case d: Double => kv = (exec.outliers.covMultiplier -> d) :: kv
              case _ => 
            }
            case javaApi.exec.outliersSuspectPercent => config.get(k) match {
              case i: Int => kv = (exec.outliers.suspectPercent -> i) :: kv
              case d: Double => kv = (exec.outliers.suspectPercent -> d.toInt) :: kv
              case _ => 
            }
            case _ =>
          }
        }
        return kv
      }
    }
    return List()
  }

  def constructScope(c: Class[_]): Unit = {
    for (clzz <- c.getClasses()) {
      classScope(clzz)
    }
  }

  def classScope(c: Class[_]): Unit = {
    val cl = c.getInterfaces
    if (!cl.isEmpty) {
      cl.head match {
        case Group => {
          val classGroupName = c.getName
          var s = Scope(classGroupName, DSL.setupzipper.value.current.context)
          val configuration = config(c)
          if (!configuration.isEmpty) {

            s = s.config(configuration: _*)
          }

          val oldscope = s.context(Key.dsl.scope)

          val ct = s.context + (Key.dsl.scope -> (c.getSimpleName() :: oldscope))
          DSL.setupzipper.value = DSL.setupzipper.value.descend.setContext(ct)
          for (clzz <- c.getClasses) classScope(clzz)
          DSL.setupzipper.value = DSL.setupzipper.value.ascend
        }

        case UsingInterface => {
          val classGroupName = c.getName
          var s = Scope(classGroupName, DSL.setupzipper.value.current.context)
          val configuration = config(c)
          if (!configuration.isEmpty)
            s = s.config(configuration: _*)

          val oldscope = s.context(Key.dsl.scope)

          val method = new SerializableMethod(c.getMethod("snippet", classOf[Object]))
          val context = DSL.setupzipper.value.current.context + (Key.dsl.scope -> (c.getSimpleName() :: oldscope))
          DSL.setupzipper.value = DSL.setupzipper.value.descend.setContext(context)

          val instance = getClassInstance(c.getName)
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
                if (classOf[org.scalameter.javaApi.VoidGen] isAssignableFrom gen.getClass) {
                  setp = Some((v: Object) => { m.invokeA(instance, null) })
                } else {
                  setp = Some((v: Object) => { m.invokeA(instance, v) })
                }
              }
              case "teardown" => {
                if (classOf[org.scalameter.javaApi.VoidGen] isAssignableFrom gen.getClass) {
                  teardown = Some((v: Object) => { m.invokeA(instance, null) })
                } else {
                  teardown = Some((v: Object) => { m.invokeA(instance, v) })
                }
              }
              case _ =>
            }
          }
          var snippet = (s: Object) => { method.invokeA(instance, s) }
          if (classOf[org.scalameter.javaApi.VoidGen] isAssignableFrom gen.getClass) {
            snippet = (s: Object) => { method.invokeA(instance, null) }
          }
          val generator = gen.get

          val setup = Setup(context, generator.asInstanceOf[Gen[Object]], setupbeforeall, teardownafterall, setp, teardown, None, snippet, executor)
          DSL.setupzipper.value = DSL.setupzipper.value.addItem(setup)
          DSL.setupzipper.value = DSL.setupzipper.value.ascend
        }

        case _ =>
      }
    }
  }

  // copy/paste from PerformanceTest
  def executeTests(): Boolean = {
    val datestart: Option[Date] = Some(new Date)
//    DSL.setupzipper.value = Tree.Zipper.root[Setup[_]].modifyContext(_ ++ defaultConfig)
//    testbody.value.apply()
    val rawsetuptree = DSL.setupzipper.value.result
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
  def javaReporter: org.scalameter.javaApi.Reporter = new org.scalameter.javaApi.LoggingReporter

  def javaPersistor: org.scalameter.javaApi.Persistor = new org.scalameter.javaApi.NonePersistor

  def javaExecutor: org.scalameter.javaApi.Executor = new org.scalameter.javaApi.LocalExecutor(
    new org.scalameter.Executor.Warmer.Default(),
    new org.scalameter.javaApi.MinAggregator,
    new org.scalameter.javaApi.DefaultMeasurer)
  def javaMeasurer: org.scalameter.javaApi.Measurer = new org.scalameter.javaApi.DefaultMeasurer
}

abstract class Microbenchmark extends JavaPerformanceTest {
  import Executor.Measurer
  def warmer = org.scalameter.Executor.Warmer.Default()
  def aggregator = Aggregator.min
  override def measurer = javaMeasurer match {
    case null => new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation {

      override val defaultFrequency = 12
      override val defaultFullGC = true
    }
    case _ => javaMeasurer.get
  }
  def javaMeasurer: org.scalameter.javaApi.Measurer = null
  override def executor = javaExecutor match{
    case null => execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    case _ => javaExecutor.get
  }
  def javaExecutor: org.scalameter.javaApi.Executor = null
  def javaReporter: org.scalameter.javaApi.Reporter = new org.scalameter.javaApi.LoggingReporter
  def javaPersistor: org.scalameter.javaApi.Persistor = new org.scalameter.javaApi.NonePersistor
}

abstract class HTMLReport extends JavaPerformanceTest {
  import Executor.Measurer
  import reporting._
  def javaPersistor: org.scalameter.javaApi.Persistor = new org.scalameter.javaApi.SerializationPersistor
  def warmer = Executor.Warmer.Default()
  def aggregator = Aggregator.average
  override def measurer: Measurer = javaMeasurer match {
    case null => new Measurer.IgnoringGC with Measurer.PeriodicReinstantiation with Measurer.OutlierElimination with Measurer.RelativeNoise
    case _ => javaMeasurer.get
  }
  def javaMeasurer: org.scalameter.javaApi.Measurer = null
  override def executor: Executor = javaExecutor match {
    case null => new execution.SeparateJvmsExecutor(warmer, aggregator, measurer)
    case _ => javaExecutor.get
  }
  def javaExecutor: org.scalameter.javaApi.Executor = null

//  def tester: RegressionReporter.Tester = javaTester.get
//  def historian: RegressionReporter.Historian = javaHistorian.get
  def online: Boolean
  def javaTester: org.scalameter.javaApi.RegressionReporterTester
  def javaHistorian: org.scalameter.javaApi.RegressionReporterHistorian
  def javaReporter: org.scalameter.javaApi.Reporter = new org.scalameter.javaApi.CompositeReporter(javaTester, javaHistorian, online)
}

abstract class OnlineRegressionReport extends HTMLReport {
  import reporting._
  def javaTester: org.scalameter.javaApi.RegressionReporterTester = new org.scalameter.javaApi.OverlapIntervals()
  def javaHistorian: org.scalameter.javaApi.RegressionReporterHistorian = new org.scalameter.javaApi.ExponentialBackoff()
  def online = true
}

abstract class OfflineRegressionReport extends HTMLReport {
  import reporting._
  def javaTester: org.scalameter.javaApi.RegressionReporterTester = new org.scalameter.javaApi.OverlapIntervals()
  def javaHistorian: org.scalameter.javaApi.RegressionReporterHistorian = new org.scalameter.javaApi.ExponentialBackoff()
  def online = false
}

abstract class OfflineReport extends HTMLReport {
  import reporting._
  def javaTester: org.scalameter.javaApi.RegressionReporterTester = new org.scalameter.javaApi.Accepter()
  def javaHistorian: org.scalameter.javaApi.RegressionReporterHistorian = new org.scalameter.javaApi.ExponentialBackoff()
  def online = false
}
