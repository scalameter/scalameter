package org.scalameter
package utils

import java.net.URL
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
      "/home/enya/fav.jar",
      "/C:/Users/John Wayne/aim.jar",
      "/localhome/Deanna Troi/classes/"
    )
    assert(strings == expected)
  }

}