package org.scalameter.utils

import java.io.{File, FileOutputStream}

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks


class IOTest extends AnyFunSuite with ScalaCheckDrivenPropertyChecks with Matchers {
  test("IO.readFromFile should slurp all bytes") {
    forAll { (o: Array[Byte]) =>
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
