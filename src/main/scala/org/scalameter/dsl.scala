package org.scalameter



import scala.util.DynamicVariable
import utils.Tree



trait DSL {

  import DSL._

  protected[scalameter] var testbodySet = false
  private[scalameter] val testbody = new DynamicVariable[() => Any]({ () =>
      if (!testbodySet)
        ???
      else
        ()
    })

  object performance {
    def of(modulename: String) = Scope(modulename, setupzipper.value.current.context)
  }

  object measure {
    def method(methodname: String) = Scope(methodname, setupzipper.value.current.context)
  }

  protected case class Scope(name: String, context: Context) {
    def config(kvs: (String, Any)*) = Scope(name, context ++ Context(kvs.toMap))
    def in(block: =>Unit): Unit = {
      val oldscope = context.goe(Key.dsl.scope, List())
      descendInScope(name, context + (Key.dsl.scope -> (name :: oldscope))) {
        block
      }
    }
  }

  protected case class Using[T](benchmark: Setup[T]) {
    def beforeTests(block: =>Any) = Using(benchmark.copy(setupbeforeall = Some(() => block)))
    def setUp(block: T => Any) = Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) = Using(benchmark.copy(teardown = Some(block)))
    def afterTests(block: =>Any) = Using(benchmark.copy(teardownafterall = Some(() => block)))
    def warmUp(block: =>Any) = Using(benchmark.copy(customwarmup = Some(() => block)))
    def curve(name: String) = Using(benchmark.copy(context = benchmark.context + (Key.dsl.curve -> name)))
    def config(xs: (String, Any)*) = Using(benchmark.copy(context = benchmark.context ++ Context(xs: _*)))
    def in(block: T => Any) {
      setupzipper.value = setupzipper.value.addItem(benchmark.copy(snippet = block))
    }
  }

  def using[T](gen: Gen[T]) = Using(Setup(setupzipper.value.current.context + (Key.dsl.curve -> freshCurveName()), gen, None, None, None, None, None, null, executor))

  def isModule = this.getClass.getSimpleName.endsWith("$")

  def include[T <: PerformanceTest.Initialization: Manifest] = {
    if (isModule) singletonInstance(manifest[T].runtimeClass).testbody.value.apply()
    else manifest[T].runtimeClass.newInstance.asInstanceOf[PerformanceTest].testbody.value.apply()
  }

  /** Runs all the tests in this test class or singleton object.
   */
  def executeTests(): Boolean

  /** The optional executor assigned to a particular body of DSL code.
   */
  def executor: Executor

}


object DSL {

  private[scalameter] val setupzipper = new DynamicVariable(Tree.Zipper.root[Setup[_]])

  private[scalameter] def descendInScope(name: String, context: Context)(body: =>Unit) {
    setupzipper.value = setupzipper.value.descend.setContext(context)
    body
    setupzipper.value = setupzipper.value.ascend
  }

  private[scalameter] val curveNameCount = new java.util.concurrent.atomic.AtomicInteger(0)

  private[scalameter] def freshCurveName(): String = "Test-" + curveNameCount.getAndIncrement()

}





