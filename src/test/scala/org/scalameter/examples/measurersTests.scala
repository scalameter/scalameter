package org.scalameter.examples

import org.scalameter.api._
import org.scalameter.execution.{SeparateJvmsExecutor, LocalExecutor}
import org.scalameter.execution.invocation.InvocationCountMatcher
import org.scalameter.persistence.InterceptingPersistor
import org.scalameter.picklers.Implicits._

trait Snippet[U] extends Bench[U] {
  val sizes = Gen.single("size")(300000)

  val ranges = for {
    size <- sizes
  } yield 0 until size
  
  performance of "Range" config(
    exec.benchRuns -> 10,
    exec.independentSamples -> 2
  ) in {
    measure method "map" in {
      using(ranges) in {
        r => r.map(_ + 1)
      }
    }
  }
}

class DefaultQuickBench extends Bench.Quick with Snippet[Double]

class DefaultMicroBench extends Bench.Micro with Snippet[Double] {
  override def measurer: Measurer[Double] = new Measurer.Default
}

class IgnoringGCQuickBench extends Bench.Quick with Snippet[Double] {
  override def measurer: Measurer[Double] = new Measurer.IgnoringGC
}

class IgnoringGCMicroBench extends Bench.Micro with Snippet[Double]

class MemoryQuickBench extends Bench.Quick with Snippet[Double] {
  override def measurer: Measurer[Double] = new Measurer.MemoryFootprint
}

class MemoryMicroBench extends Bench.Micro with Snippet[Double] {
  override def measurer: Measurer[Double] = new Measurer.MemoryFootprint
}

class GCCountQuickBench extends Bench[Int] with Snippet[Int] {
  def executor: Executor[Int] = new LocalExecutor(Warmer.Zero, Aggregator.median[Int], measurer)

  def measurer: Measurer[Int] = new Measurer.GarbageCollectionCycles

  def persistor: Persistor = Persistor.None

  def reporter: Reporter[Int] = new LoggingReporter
}

class GCCountMicroBench extends Bench[Int] with Snippet[Int] {
  def executor: Executor[Int] = new SeparateJvmsExecutor(Warmer.Zero, Aggregator.median[Int], measurer)

  def measurer: Measurer[Int] = new Measurer.GarbageCollectionCycles

  def persistor: Persistor = Persistor.None

  def reporter: Reporter[Int] = new LoggingReporter
}

class InvocationCountMeasurerBench extends Bench.Micro {
  override val persistor: InterceptingPersistor =
    new InterceptingPersistor(new GZIPJSONSerializationPersistor)

  def min: Int = 5000

  def hop: Int = 5000

  def max: Int = 50000

  val sizes = Gen.range("size")(min, max, hop)

  val lists = for (sz <- sizes) yield (0 until sz).toList
}

class BoxingCountBench extends InvocationCountMeasurerBench {
  override lazy val measurer = Measurer.BoxingCount.all().map(v =>
    v.copy(value = v.value.valuesIterator.sum.toDouble)
  )

  override def defaultConfig = Context(
    exec.independentSamples -> 1
  )

  performance of "List" in {
    measure method "map" in {
      using(lists) in { xs =>
        xs.map(_ + 1)
      }
    }
  }
}

class MethodInvocationCountBench extends InvocationCountMeasurerBench {
  override lazy val measurer = Measurer.MethodInvocationCount(
    InvocationCountMatcher.allocations(classOf[Some[_]])
  ).map(v => v.copy(value = v.value.valuesIterator.sum.toDouble))

  override def defaultConfig = Context(
    exec.independentSamples -> 1
  )

  performance of "List" in {
    measure method "map" in {
      using(lists) in { xs =>
        xs.map(Some(_))
      }
    }
  }
}
