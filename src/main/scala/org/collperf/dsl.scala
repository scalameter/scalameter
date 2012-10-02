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
    def curve(name: String) = Using(benchmark.copy(context = benchmark.context + (Key.curve -> name)))
    def apply(block: T => Any) {
      scheduleBenchmark(benchmark.copy(snippet = block))
    }
  }

  def using[T](gen: Gen[T]) = Using(Benchmark(executor, currentContext.value, gen, None, None, None, null))

}


class TestSeq extends PerformanceTest.LeastTime {

  performance of "Seq" in {

    val lists = for {
      size <- Gen.range("size")(500000, 5000000, 200000)
    } yield (0 until size).toList

    val arrays = for {
      size <- Gen.range("size")(500000, 5000000, 200000)
    } yield (0 until size).toArray

    val vectors = for {
      size <- Gen.range("size")(500000, 5000000, 200000)
    } yield (0 until size).toVector

    measure method "filter" in {
      using(lists) curve("List") apply {
        _.filter(_ % 2 == 0)
      }

      using(arrays) curve("Array") apply {
        _.filter(_ % 2 == 0)
      }

      using(vectors) curve("Vector") apply {
        _.filter(_ % 2 == 0)
      }
    }

  }

}
















