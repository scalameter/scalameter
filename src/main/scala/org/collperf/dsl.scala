package org.collperf



import collection._



trait DSL {

  private val curves = new scala.util.DynamicVariable(List[CurveData]())

  def executor: Executor

  def reporter: Reporter

  object performance {
    def of(modulename: String) = new {
      def in(block: =>Unit): Unit = currentContext.withAttribute(Key.module, modulename) {
        block

        val persistor = currentContext.value.goe(Key.persistor, Persistor.None)
        val cs = curves.value.reverse
        reporter.report(ResultData(cs, cs.head.context), persistor)
        curves.value = Nil
      }
    }
  }

  object measure {
    def method(methodname: String) = new {
      def in(block: =>Unit): Unit = currentContext.withAttribute(Key.method, methodname) {
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
      val curve = benchmark.copy(snippet = block).run()
      curves.value ::= curve
    }
  }

  def using[T](gen: Gen[T]) = Using(Setup(executor, reporter, currentContext.value, gen, None, None, None, null))

}




