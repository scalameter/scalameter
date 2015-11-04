package org.scalameter
package utils



import java.io.File
import java.net._
import org.apache.commons.lang3.SystemUtils
import org.scalatest.{FunSuite, Matchers}



class ClassPathTest extends FunSuite with Matchers {

  test("File paths with spaces should be correctly extracted") {
    val urls = Array[URL](
      new URL("file:/home/enya/fav.jar"),
      new URL("file:/C:/Users/John%20Wayne/aim.jar"),
      new URL("file:/localhome/Deanna Troi/classes/"),
      new URL("http://domain.com/some.jar")
    )
    val strings = ClassPath.extractFileClasspaths(urls)
    val expected = Seq(
      new File("/home/enya/fav.jar"),
      new File("/C:/Users/John Wayne/aim.jar"),
      new File("/localhome/Deanna Troi/classes/")
    )
    assert(strings == expected)
  }

  test("Classpath construct should not allow for creation from strings containing classpath separator") {
    intercept[IllegalArgumentException] {
      ClassPath(new File(s"/home/test/${File.pathSeparatorChar}invalid") :: Nil)
    }
  }

}
