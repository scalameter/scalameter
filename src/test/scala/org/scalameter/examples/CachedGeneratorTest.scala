package org.scalameter
package examples



import reporting._
import Key._



class CachedGeneratorTest extends PerformanceTest.Regression {

  def persistor = new persistence.SerializationPersistor

  val sizes = Gen.range("size")(100000000, 500000000, 200000000)
  val parallelismLevels = Gen.enumeration("parallelismLevel")(1, 2, 4, 8)
  val pools = (for (par <- parallelismLevels) yield new collection.parallel.ForkJoinTaskSupport(new concurrent.forkjoin.ForkJoinPool(par))).cached
  val inputs = Gen.tupled(sizes, pools)

  performance of "foreach" in {
    performance of "ParRange" in {
      using(inputs) config (
        exec.benchRuns -> 10,
        exec.independentSamples -> 2
      ) in { case (sz, p) =>
        val pr = (0 until sz).par
        pr.tasksupport = p
        pr.foreach(x => ())
      }
    }
  }

}















