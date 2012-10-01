package org.collperf






trait DSL extends HasExecutor {

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
      scheduleBenchmark(benchmark.copy(snippet = block))
    }
  }

  def using[T](gen: Gen[T]) = Using(Benchmark(executor, currentContext.value, gen, None, None, None, null))

}


class TestDSL extends PerformanceTest.Default {

  performance of "ParRange" in {

    val ranges = for {
      parlevel <- Gen.enumeration("parallelism")(1, 2, 4, 8)
      size <- Gen.range("size")(2000000, 2500000, 100000)
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


class TestList extends PerformanceTest.Default {

  performance of "List" in {

    val lists = for {
      size <- Gen.range("size")(1000000, 5000000, 500000)
    } yield (0 until size).toList

    measure method "filter" in {
      using(lists) {
        _.filter(_ % 2 == 0)
      }
    }

  }

}
















