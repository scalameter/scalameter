package org.collperf



import org.scalatools.testing._
import collection._



class ScalaMeterFramework extends Framework {

  def name = "ScalaMeter"

  def tests = Array[Fingerprint](
    PerformanceTestFingerprint
  )

  def testRunner(testClassLoader: ClassLoader, loggers: Array[Logger]) = new Runner2 {
    case class TestInterfaceEvents(eventHandler: EventHandler) extends Events {
      def emit(e: org.collperf.Event) = eventHandler.handle(new org.scalatools.testing.Event {
        def testName = e.testName
        def description = e.description
        def error = e.throwable
        def result = e.result match {
          case Events.Success => Result.Success
          case Events.Failure => Result.Failure
          case Events.Error => Result.Error
          case Events.Skipped => Result.Skipped
        }
      })
    }

    case class TestInterfaceLog(l: Logger) extends Log {
      def error(msg: String) = l.error(msg)
      def warn(msg: String) = l.warn(msg)
      def trace(t: Throwable) = l.trace(t)
      def info(msg: String) = l.info(msg)
      def debug(msg: String) = l.debug(msg)
    }

    def run(testClassName: String, fingerprint: Fingerprint, eventHandler: EventHandler, args: Array[String]) {
      val complog = Log.Composite(loggers.map(TestInterfaceLog): _*)
      val tievents = TestInterfaceEvents(eventHandler)
      dyn.log.withValue(complog) {
        dyn.events.withValue(tievents) {
          dyn.initialContext.withValue(Main.Configuration.fromCommandLineArgs(args).context) {
            testClassLoader.loadClass(testClassName).newInstance
            ()
          }
        }
      }
    }
  }

  private case object PerformanceTestFingerprint extends SubclassFingerprint {
    def isModule = false
    def superClassName = classOf[PerformanceTest].getName
  }

}


