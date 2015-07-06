package org.scalameter.execution.invocation.instrumentation

import java.io.File
import java.util.zip.ZipFile
import org.scalatest.{Matchers, FunSuite}
import scala.collection.convert.decorateAsScala._
import scala.collection.mutable
import org.scalameter.Context
import org.scalameter.execution.invocation.InvocationCountMatcher


class InstrumentationTest extends FunSuite with Matchers {
  class Test {
    def test1() = "test1"
    def test2(i: Int, b: String) = ()
    def ttest1() = "ttest1"
    def est2(i: Int, b: String) = "est2"
  }

  def withInstrumentation()(f: (File, mutable.ArrayBuffer[MethodSignature]) => Any): Any = {
    import InvocationCountMatcher._

    val jar = File.createTempFile("InstrumentationTest-", ".jar")
    jar.deleteOnExit()
    val matcher = InvocationCountMatcher(ClassMatcher.ClassName(classOf[Test]), MethodMatcher.Regex("^test.*".r.pattern))
    val lookupTable = Instrumentation.writeInstrumentedClasses(ctx = Context.topLevel, matcher = matcher, to = jar)

    f(jar, lookupTable)
  }

  test("Instrumenting Test class should only match test1 and test2 methods") {
    withInstrumentation() { (_, actual) =>
      val expected = List(MethodSignature(classOf[Test].getName, "test1"), MethodSignature(classOf[Test].getName, "test2", "int", "java.lang.String"))
      actual should contain theSameElementsAs expected
    }
  }

  test("Instrumented jar should contain only Test class and MANIFEST.MF") {
    withInstrumentation() { (jar, _) =>
      val zip = new ZipFile(jar)

      zip.entries().asScala.toVector.length should === (2)
      zip.getEntry("META-INF/MANIFEST.MF") should !== (null)
      zip.getEntry(classOf[Test].getName.replace('.', '/') + ".class") should !== (null)
    }
  }
}
