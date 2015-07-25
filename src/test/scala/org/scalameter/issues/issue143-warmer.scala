package org.scalameter
package issues



import java.io._
import org.scalatest.FunSuite
import org.scalameter.api._



class WarmerZeroTest extends FunSuite {

  test("Warmer.Zero should be serializable") {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)
    oos.writeObject(Warmer.Zero)
    oos.close()
  }

}