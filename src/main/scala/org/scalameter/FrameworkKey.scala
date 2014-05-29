package org.scalameter

/** Object containing keys specific to the ScalaMeter benchmarking framework.
 */
object FrameworkKey {

  object dsl {
    lazy val executor = Key[Executor]("executor")
  }

}