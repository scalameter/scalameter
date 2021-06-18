package org.scalameter.japi

import org.scalameter.api._
import org.scalameter.utils.ClassPath
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


class ContextBuilderTest extends AnyFunSuite with Matchers {
  test("ContextBuilder should create the same context as direct context creation") {
    val default = ClassPath.default
    val expected = Context(
      exec.benchRuns := 30,
      verbose := false,
      classpath := default
    )
    val actual = new ContextBuilder()
      .put("exec.benchRuns", 30)
      .put("verbose", false)
      .put("classpath", default)
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
