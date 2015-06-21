package org.scalameter.persistence

import org.scalatest.{Matchers, FunSuite}


class JSONSerializationPersistorTest extends FunSuite with PersistorTest with Matchers {
  test("Should correctly serialize and deserialize history") {
    afterBenchmark(new JSONSerializationPersistor) { persistor =>
      for ((ctx, expected) <- persistor.intercepted) {
        val actual = persistor.load(ctx)
        compareHistory(actual = actual, expected = expected)
      }
    }
  }
}
