package org.collperf



import scala.util.DynamicVariable



trait DSL {

  private class DynamicContext extends DynamicVariable(Context.topLevel) {
    def withAttribute[T](name: String, v: Any)(block: =>T) = withValue(value + (name -> v))(block)
  }

  private val currentContext = new DynamicContext

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

  protected case class Using[T](benchmark: Benchmark[T]) {
    def setUp(block: T => Any) = Using(benchmark.copy(setup = Some(block)))
    def tearDown(block: T => Any) = Using(benchmark.copy(teardown = Some(block)))
    def warmUp(block: =>Any) = Using(benchmark.copy(customwarmup = Some(() => block)))
    def apply(block: T => Any) {
      Runner.schedule(benchmark.copy(snippet = block))
    }
  }

  def using[T](gen: Gen[T]) = Using(Benchmark(currentContext.value, gen, None, None, None, null))

}


object TestDSL extends DSL {

  performance of "ParRange" in {

    val ranges = for {
      size <- Gen.range("size")(100, 10000, 250)
      parlevel <- Gen.enumeration("parallelism")(1, 2, 4, 8)
    } yield {
      val pr = (0 until size).par
      pr.tasksupport = new collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(parlevel))
      pr
    }

    measure method "filter" in {
      using(ranges) {
        _.filter(_ % 2 == 0)
      }
    }

    measure method "reduce" in {
      using(ranges) {
        _.reduce(_ + _)
      }
    }

    measure method "foreach" in {
      var sum = 0
      using(ranges) tearDown {
        _ => sum = 0
      } apply {
        for (n <- _) sum += n
      }
    }

  }

}


















