package org.scalameter.persistence

import org.scalatest.{FunSuite, Matchers}


class GZIPJSONSerializationPersistorTest extends FunSuite with PersistorTest with Matchers {
  val persistor = new GZIPJSONSerializationPersistor

  test("Should correctly serialize and deserialize history") {
    executeBenchmark() { persistor =>
      for ((ctx, expected) <- persistor.intercepted) {
        val actual = persistor.load(ctx)
        compareHistory(actual = actual, expected = expected)
      }
    }
  }

  test("Should produce data which occupies less than 3.5kB") {
    executeBenchmark() { persistor =>
      for ((ctx, _) <- persistor.intercepted) {
        val file = persistor.fileFor(ctx)
        compareSpaceConsumption(file, 3.5)
      }
    }
  }
}
