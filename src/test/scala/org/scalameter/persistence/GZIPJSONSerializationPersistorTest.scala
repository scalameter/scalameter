package org.scalameter.persistence

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


class GZIPJSONSerializationPersistorTest extends AnyFunSuite with PersistorTest with Matchers {
  val persistor = new GZIPJSONSerializationPersistor

  test("Should correctly serialize and deserialize history") {
    executeBenchmark() { persistor =>
      for ((ctx, expected) <- persistor.intercepted) {
        val actual = persistor.load(ctx)
        compareHistory(actual = actual, expected = expected)
      }
    }
  }

  test("Should produce data which occupies less than 8.0kB") {
    executeBenchmark() { persistor =>
      for ((ctx, _) <- persistor.intercepted) {
        val file = persistor.fileFor(ctx)
        compareSpaceConsumption(file, 8.0)
      }
    }
  }
}
