package org.scalameter



/** Object containing keys specific to the ScalaMeter benchmarking framework.
 */
object FrameworkKey extends KeyContainer("", null) {

  object dsl extends KeyContainer("dsl", FrameworkKey.this) {
    lazy val executor = Key[Executor]("executor")
  }

}
