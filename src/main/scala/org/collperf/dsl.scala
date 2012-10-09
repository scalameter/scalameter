package org.collperf



import collection._



trait DSL extends DelayedInit {

  private val curves = new scala.util.DynamicVariable(mutable.ArrayBuffer[CurveData]())
  private val setups = new scala.util.DynamicVariable(mutable.ArrayBuffer[Setup[_]]())

  def executor: Executor

  def reporter: Reporter

  object performance {
    def of(modulename: String) = new {
      def in(block: =>Unit): Unit = currentContext.withAttribute(Key.module, modulename) {
        block

        val persistor = currentContext.value.goe(Key.persistor, Persistor.None)
        val cs = curves.value
        reporter.report(ResultData(cs, cs.head.context), persistor)
        curves.value.clear()
      }
    }
  }

  type SameType

  object measure {
    def method(methodname: String) = new {
      def in(block: =>Unit): Unit = currentContext.withAttribute(Key.method, methodname) {
        block

        curves.value ++= executor.run(setups.value.asInstanceOf[Seq[Setup[SameType]]])
        setups.value.clear()
      }
    }
  }

  protected case class Using[T](benchmark: Setup[T]) {
    def setUp(block: T => Any) = Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) = Using(benchmark.copy(teardown = Some(block)))
    def warmUp(block: =>Any) = Using(benchmark.copy(customwarmup = Some(() => block)))
    def curve(name: String) = Using(benchmark.copy(context = benchmark.context + (Key.curve -> name)))
    def apply(block: T => Any) {
      setups.value += benchmark.copy(snippet = block)
    }
  }

  def using[T](gen: Gen[T]) = Using(Setup(currentContext.value, gen, None, None, None, null))

  def delayedInit(body: =>Unit) {
    // TODO refactor dsl to make a tree, run all warmups
    body
  }

}




