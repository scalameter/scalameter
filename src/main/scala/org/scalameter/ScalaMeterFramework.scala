package org.scalameter



import org.scalatools.testing._



class ScalaMeterFramework extends Framework {

  def name = "ScalaMeter"

  def tests = Array[Fingerprint](
    PerformanceTestClassFingerprint,
    PerformanceTestModuleFingerprint
  )

  def testRunner(testClassLoader: ClassLoader, loggers: Array[Logger]) = new Runner2 {
    case class TestInterfaceEvents(eventHandler: EventHandler) extends Events {
      def emit(e: org.scalameter.Event) = eventHandler.handle(
        new org.scalatools.testing.Event {
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

    class TestInterfaceLog(l: Logger) extends Log {
      def error(msg: String) = l.error(msg)
      def warn(msg: String) = l.warn(msg)
      def info(msg: String) = l.info(msg)
      def debug(msg: String) = if (currentContext(Key.verbose)) {
        // if verbose is on, treat this as a normal message
        info(msg)
      } else l.debug(msg)
    }

    case class JLineTestInterfaceLog(jline: Log.JLine, logger: Logger)
    extends Log.Proxy(jline) {
      override def report(msg: String): Unit = {
        jline.clear()
        logger.info(msg)
      }
    }

    def computeClasspath = {
      utils.ClassPath.extract(
        testClassLoader,
        sys.error(
          s"Cannot recognize classloader (not URLClassLoader): $testClassLoader"))
    }

    def run(
      testClassName: String, fingerprint: Fingerprint, eventHandler: EventHandler,
      args: Array[String]
    ): Unit = {
      // Special case when there is only one logger, and it belongs to SBT.
      val isSbt =
        loggers.size == 1 && loggers(0).getClass.getName.startsWith("sbt.")
      val complog = Log.default match {
        case log: Log.JLine if isSbt =>
          new JLineTestInterfaceLog(log, loggers(0))
        case _ =>
          Log.Composite(loggers.map(new TestInterfaceLog(_)).toSeq: _*)
      }
      val tievents = TestInterfaceEvents(eventHandler)
      val testcp = computeClasspath
      val ctx = currentContext ++
        Main.Configuration.fromCommandLineArgs(args).context + (Key.classpath -> testcp)

      withTestContext(ctx, complog, tievents) {
        try fingerprint match {
          case fp: SubclassFingerprint =>
            if (!fp.isModule) {
              val ptest = testClassLoader.loadClass(testClassName)
                .newInstance.asInstanceOf[BasePerformanceTest[_]]
              ptest.executeTests()
            } else {
              val module = Class.forName(testClassName + "$", true, testClassLoader)
              val ptest = utils.Reflect.singletonInstance(module)
              ptest.executeTests()
            }
        } catch {
          case e: Exception =>
            println("Test threw exception: " + e)
            e.printStackTrace()
            throw e
        }
      }
    }
  }

  private case object PerformanceTestClassFingerprint extends SubclassFingerprint {
    def isModule = false
    def superClassName = classOf[AbstractPerformanceTest].getName
  }

  private case object PerformanceTestModuleFingerprint extends SubclassFingerprint {
    def isModule = true
    def superClassName = classOf[AbstractPerformanceTest].getName
  }

}
