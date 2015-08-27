package org.scalameter.persistence



import org.scalatest.{FunSuite, Matchers}



class SerializationPersistorTest extends FunSuite with PersistorTest with Matchers {
  val persistor = new SerializationPersistor

  test("Should correctly serialize and deserialize history") {
    executeBenchmark() { persistor =>
      for ((ctx, expected) <- persistor.intercepted) {
        val actual = persistor.load(ctx)
        compareHistory(actual = actual, expected = expected)
      }
    }
  }

  test("Should produce data which occupies less than 11kB") {
    executeBenchmark() { persistor =>
      for ((ctx, _) <- persistor.intercepted) {
        val file = persistor.fileFor(ctx)
        compareSpaceConsumption(file, 11.0)
      }
    }
  }
}
