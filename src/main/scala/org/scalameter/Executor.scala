package org.scalameter



import org.scalameter.picklers.Pickler
import org.scalameter.utils.Tree
import scala.language.higherKinds



/** Executor is a class that executes benchmarks.
 *
 *  It uses a warmer to get the VM to a state where benchmarks can be executed.
 *  It then uses a measurer to do the actual measurements.
 */
trait Executor[V] {

  def measurer: Measurer[V]

  def warmer: Warmer

  def run[T](setuptree: Tree[Setup[T]], reporter: Reporter[V],
    persistor: Persistor): Tree[CurveData[V]] = {
    for (setup <- setuptree) yield {
      val cd = runSetup(setup)
      reporter.report(cd, persistor)
      cd
    }
  }

  def runSetup[T](setup: Setup[T]): CurveData[V]

}


/** Companion object with default implementations.
 */
object Executor {

  type Warmer = org.scalameter.Warmer
  type Measurer[T] = org.scalameter.Measurer[T]

  val Warmer = org.scalameter.Warmer
  val Measurer = org.scalameter.Measurer

  def None[U] = new Executor[U] {
    def measurer: Measurer[U] = ???

    def warmer: Warmer = ???

    def runSetup[T](setup: Setup[T]): CurveData[U] = ???
  }

  /** Creates an executor from a warmer, printer, aggregator and measurer.
   *
   * Implemented by companion objects of `Executor` implementations.
   */
  trait Factory[E[_] <: Executor[_]] {
    def apply[T: Pickler : PrettyPrinter](warmer: Warmer, aggregator: Aggregator[T],
      m: Measurer[T]): E[T]
  }

}






















