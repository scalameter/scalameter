package org.scalameter
package windows



import java.io.File
import org.scalatest.funsuite.AnyFunSuite
import org.apache.commons.lang3.SystemUtils



class ClassPathTest extends AnyFunSuite {

  test("A classpath should not contain double quotes") {
    if (SystemUtils.IS_OS_WINDOWS) {
      intercept[IllegalArgumentException] {
        utils.ClassPath(Seq(
          new File("c:\\users\\classes"),
          new File("c:\\My\"Libraries\"\\lib.jar")
        ))
      }
    }
  }

}
