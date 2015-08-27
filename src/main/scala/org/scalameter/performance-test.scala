package org.scalameter



import java.util.Date
import org.scalameter.picklers.Pickler
import org.scalameter.utils.Tree
import scala.language.existentials
import scala.collection._
import scala.reflect.ClassTag
import scala.util.DynamicVariable



/** Abstract required for the [[org.scalameter.ScalaMeterFramework]] to find all
 *  performance tests.
 */
sealed trait AbstractPerformanceTest {
  def executeTests(): Boolean
}


abstract class BasePerformanceTest[U] extends AbstractPerformanceTest {
  import BasePerformanceTest._

  setupzipper.value =
    Tree.Zipper.root[Setup[_]](measurer.prepareContext(currentContext ++ defaultConfig))

  protected case class Scope(name: String, context: Context) {
    def config(kvs: KeyValue*): Scope = config(context ++ Context(kvs: _*))
    def config(ctx: Context): Scope = Scope(name, context ++ ctx)
    def in(block: =>Unit): Unit = {
      val oldscope = context(Key.dsl.scope)
      descendInScope(name, context + (Key.dsl.scope -> (name :: oldscope))) {
        block
      }
    }
  }

  protected case class Using[T](benchmark: Setup[T]) {
    def beforeTests(block: =>Any) =
      Using(benchmark.copy(setupbeforeall = Some(() => block)))
    def setUp(block: T => Any) =
      Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) =
      Using(benchmark.copy(teardown = Some(block)))
    def afterTests(block: =>Any) =
      Using(benchmark.copy(teardownafterall = Some(() => block)))
    def warmUp(block: =>Any) =
      Using(benchmark.copy(customwarmup = Some(() => block)))
    def curve(name: String) =
      Using(benchmark.copy(context = benchmark.context + (Key.dsl.curve -> name)))
    def config(kvs: KeyValue*): Using[T] =
      config(Context(kvs: _*))
    def config(ctx: Context): Using[T] =
      Using(benchmark.copy(context = benchmark.context ++ ctx))
    def in(block: T => Any) {
      setupzipper.value = setupzipper.value.addItem(benchmark.copy(snippet = block))
    }
  }

  private def setupFilter(setup: Setup[_]): Boolean = {
    val sf = currentContext(Key.scopeFilter)
    val fullname = setup.context.scope + "." + setup.context.curve
    val regex = sf.r
    regex.findFirstIn(fullname) != None
  }

  type SameType

  def isModule = this.getClass.getSimpleName.endsWith("$")

  def defaultConfig: Context = Context.empty

  /** Allows rebuilding of setup zipper after test initialization.
   *
   *  Default implementation is empty.
   */
  def rebuildSetupZipper(): Unit = {}

  /** Runs all the tests in this test class or singleton object.
   */
  def executeTests(): Boolean = {
    rebuildSetupZipper()

    val datestart: Option[Date] = Some(new Date)
    val rawsetuptree = BasePerformanceTest.setupzipper.value.result
    val setuptree = rawsetuptree.filter(setupFilter)

    measurer.beforeExecution(setuptree.context)
    val resulttree =
      executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]], reporter, persistor)
    measurer.afterExecution(setuptree.context)

    val dateend: Option[Date] = Some(new Date)
    val datedtree = resulttree.copy(context = resulttree.context +
      (Key.reports.startDate -> datestart) + (Key.reports.endDate -> dateend))
    reporter.report(datedtree, persistor)
  }

  /** The optional executor assigned to a particular body of DSL code.
   */
  def executor: Executor[U]

  def measurer: Measurer[U]

  def reporter: Reporter[U]

  def persistor: Persistor
}


object BasePerformanceTest {

  private[scalameter] val setupzipper =
    new DynamicVariable(Tree.Zipper.root[Setup[_]](currentContext))

  private[scalameter] def descendInScope(name: String, context: Context)(body: =>Unit) {
    setupzipper.value = setupzipper.value.descend.setContext(context)
    body
    setupzipper.value = setupzipper.value.ascend
  }

  private[scalameter] val curveNameCount =
    new java.util.concurrent.atomic.AtomicInteger(0)

  private[scalameter] def freshCurveName(): String =
    "Test-" + curveNameCount.getAndIncrement()

}


trait GroupedPerformanceTest extends BasePerformanceTest[Nothing] {
  private[scalameter] val includes =
    mutable.Set[(BasePerformanceTest[_], Tree.Zipper[Setup[_]])]()

  def include[T <: BasePerformanceTest[_]: ClassTag](newBenchmark: =>T) = {
    val cls = implicitly[ClassTag[T]].runtimeClass
    if (cls.getSimpleName.endsWith("$") || !cls.isInterface) {
      log.error(
        s"Can only use `include` with anonymous classes instantiated from traits -- " +
        s"please make ${cls.getName} a trait and " +
        s"call include(new ${cls.getSimpleName} {}).")
      events.emit(Event(
        cls.getName,
        s"Can only use `include` with anonymous classes instantiated from traits -- " +
        s"please make ${cls.getName} a trait and " +
        s"call include(new ${cls.getSimpleName} {}).",
        Events.Error, new Exception("Cannot use non-anonymous benchmark class.")))
    } else {
      val oldvalue = BasePerformanceTest.setupzipper.value
      for (_ <- dyn.currentContext.using(oldvalue.current.context)) {
        val bench = newBenchmark
        includes += ((bench, BasePerformanceTest.setupzipper.value))
      }
      BasePerformanceTest.setupzipper.value = oldvalue
    }
  }

  override def executeTests(): Boolean = {
    val results = for ((b, z) <- includes) yield {
      BasePerformanceTest.setupzipper.value = z
      b.executeTests()
    }
    results.forall(_ == true)
  }
}
