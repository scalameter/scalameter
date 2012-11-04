package org.collperf



import collection._
import utils.Tree



trait DSL extends DelayedInit {

  def executor: Executor

  def reporter: Reporter

  def persistor: Persistor

  private val setupzipper = new scala.util.DynamicVariable(Tree.Zipper.root[Setup[_]])

  private def descendInScope(name: String, context: Context)(body: =>Unit) {
    setupzipper.value = setupzipper.value.descend.setContext(context)
    body
    setupzipper.value = setupzipper.value.ascend
  }

  object performance {
    def of(modulename: String) = Scope(modulename, setupzipper.value.current.context)
  }

  object measure {
    def method(methodname: String) = Scope(methodname, setupzipper.value.current.context)
  }

  protected case class Scope(name: String, context: Context) {
    def configuration(kvs: (String, Any)*) = Scope(name, context ++ Context(kvs.toMap))
    def in(block: =>Unit): Unit = {
      val oldscope = context.goe(Key.dsl.scope, List())
      descendInScope(name, context + (Key.dsl.scope -> (name :: oldscope))) {
        block
      }
    }
  }

  protected case class Using[T](benchmark: Setup[T]) {
    def setUp(block: T => Any) = Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) = Using(benchmark.copy(teardown = Some(block)))
    def warmUp(block: =>Any) = Using(benchmark.copy(customwarmup = Some(() => block)))
    def curve(name: String) = Using(benchmark.copy(context = benchmark.context + (Key.dsl.curve -> name)))
    def configuration(xs: (String, Any)*) = Using(benchmark.copy(context = benchmark.context ++ Context(xs: _*)))
    def in(block: T => Any) {
      setupzipper.value = setupzipper.value.addItem(benchmark.copy(snippet = block))
    }
  }

  def using[T](gen: Gen[T]) = Using(Setup(setupzipper.value.current.context, gen, None, None, None, null))

  /* initialization */
  
  protected def initSetupTree() {
    setupzipper.value = setupzipper.value.addContext(Key.dsl.executor -> executor.toString)
  }

  type SameType

  protected def executeTests() {
    val datestart = new java.util.Date
    val setuptree = setupzipper.value.result
    val resulttree = executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]])
    val dateend = new java.util.Date

    val datedtree = resulttree.copy(context = resulttree.context + (Key.reporting.startDate -> datestart) + (Key.reporting.endDate -> dateend))
    reporter.report(datedtree, persistor)
  }

  def delayedInit(body: =>Unit) {
    initSetupTree()
    body
    executeTests()
  }

}




