package org.scalameter.persistence

import org.scalatest.{Matchers, FunSuite}


class GZIPJSONSerializationPersistorTest extends FunSuite with PersistorTest with Matchers {
  test("Should correctly serialize and deserialize history") {
    afterBenchmark(new GZIPJSONSerializationPersistor) { persistor =>
      for ((ctx, expected) <- persistor.cached) {
        val actual = persistor.load(ctx)
        compareHistory(actual = actual, expected = expected)
      }
    }
  }
}
