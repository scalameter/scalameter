package org.collperf



import collection._
import utils.Tree



trait DSL extends DelayedInit {

  def executor: Executor

  def reporter: Reporter

  private val setupzipper = new scala.util.DynamicVariable(Tree.Zipper.root[Setup[_]])

  private def descendInScope(name: String)(body: =>Unit) {
    setupzipper.value = setupzipper.value.descend.transformContext(Key.scope, {
      scope: List[String] => name :: scope
    })
    body
    setupzipper.value = setupzipper.value.ascend
  }

  object performance {
    def of(modulename: String) = new {
      def in(block: =>Unit): Unit = descendInScope(modulename) {
        block
      }
    }
  }

  type SameType

  object measure {
    def method(methodname: String) = new {
      def in(block: =>Unit): Unit = descendInScope(methodname) {
        block
      }
    }
  }

  protected case class Using[T](benchmark: Setup[T]) {
    def setUp(block: T => Any) = Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) = Using(benchmark.copy(teardown = Some(block)))
    def warmUp(block: =>Any) = Using(benchmark.copy(customwarmup = Some(() => block)))
    def curve(name: String) = Using(benchmark.copy(context = benchmark.context + (Key.curve -> name)))
    def apply(block: T => Any) {
      setupzipper.value = setupzipper.value.addItem(benchmark.copy(snippet = block))
    }
  }

  def using[T](gen: Gen[T]) = Using(Setup(setupzipper.value.current.context, gen, None, None, None, null))

  /* initialization */
  
  protected def initSetupTree() {
    setupzipper.value = setupzipper.value.addContext(Key.executor -> executor.toString)
  }

  protected def executeTests() {
    val persistor = initialContext.goe(Key.persistor, Persistor.None)
    val setuptree = setupzipper.value.result
    val resulttree = executor.run(setuptree.asInstanceOf[Tree[Setup[SameType]]])
    reporter.report(resulttree, persistor)
  }

  def delayedInit(body: =>Unit) {
    initSetupTree()
    body
    executeTests()
  }

}




