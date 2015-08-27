package org.scalameter



import org.scalameter.picklers.noPickler._



/** Object containing keys specific to the ScalaMeter benchmarking framework.
 */
object FrameworkKey extends KeyContainer("", null) {

  object dsl extends KeyContainer("dsl", FrameworkKey.this) {
    lazy val executor = Key[Executor[_]]("executor")
    lazy val reporter = Key[Reporter[_]]("reporter")
  }

}
