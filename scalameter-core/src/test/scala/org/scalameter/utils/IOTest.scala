package org.scalameter.utils

import java.io.{File, FileOutputStream}

import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, FunSuite}


class IOTest extends FunSuite with PropertyChecks with Matchers {
  test("IO.readFromFile should slurp all bytes") {
    forAll { o: Array[Byte] =>
      val file = File.createTempFile("scalameter-io-readAllBytes-", ".dat")
      file.deleteOnExit()

      val fos = new FileOutputStream(file)
      fos.write(o)
      fos.close()

      val bytes = IO.readFromFile(file, chunkSize = 8)
      bytes should === (o)
    }
  }
}
