package org.collperf



import collection._



trait DSL {

  def executor: Executor

  def reporter: Reporter

  object performance {
    def of(modulename: String) = new {
      def in(block: =>Unit): Unit = currentContext.withAttribute(Key.module, modulename) {
        block
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

  protected case class Using[T](benchmark: BenchmarkSetup[T]) {
    def setUp(block: T => Any) = Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) = Using(benchmark.copy(teardown = Some(block)))
    def warmUp(block: =>Any) = Using(benchmark.copy(customwarmup = Some(() => block)))
    def curve(name: String) = Using(benchmark.copy(context = benchmark.context + (Key.curve -> name)))
    def apply(block: T => Any) {
      val result = benchmark.copy(snippet = block).run()
      val persistor = benchmark.context.goe(Key.persistor, Persistor.None)
      benchmark.reporter.report(result, persistor)
    }
  }

  def using[T](gen: Gen[T]) = Using(BenchmarkSetup(executor, reporter, currentContext.value, gen, None, None, None, null))

}




