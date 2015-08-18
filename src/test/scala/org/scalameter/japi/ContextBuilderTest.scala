package org.scalameter.japi

import org.scalameter.api._
import org.scalameter.utils.ClassPath
import org.scalatest.{FunSuite, Matchers}


class ContextBuilderTest extends FunSuite with Matchers {
  test("ContextBuilder should create the same context as direct context creation") {
    val expected = Context(
      exec.benchRuns -> 30,
      verbose -> false,
      classpath -> ClassPath.default
    )
    val actual = new ContextBuilder()
      .put("exec.benchRuns", 30)
      .put("verbose", false)
      .put("classpath", ClassPath.default)
      .build()

    actual should === (expected)
  }

  test("ContextBuilder should throw error on invalid key insertion") {
    intercept[RuntimeException] {
      new ContextBuilder()
        .put("vebrose", false)
    }
  }
}
