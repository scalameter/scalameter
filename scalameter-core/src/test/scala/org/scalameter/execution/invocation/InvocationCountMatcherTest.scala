package org.scalameter.execution.invocation

import org.objectweb.asm.Type
import org.scalatest.{FunSuite, Matchers}
import org.scalameter.execution.invocation.InvocationCountMatcher._


class InvocationCountMatcherTest extends FunSuite with Matchers {
  test("StringClassMatcher should match classes") {
    val matcher = ClassMatcher.ClassName(classOf[java.lang.Integer])
    matcher.matches("java.lang.Integer") should === (true)
    matcher.matches("java/lang/Integer") should === (false)
    matcher.matches("java.lang.Long") should === (false)
    matcher.matches("java/lang/Long") should === (false)
  }

  test("RegexClassMatcher should match classes") {
    val matcher =  ClassMatcher.Regex("(java\\.lang\\.Integer)|(java\\.lang\\.Long)".r.pattern)
    matcher.matches("java.lang.Integer") should === (true)
    matcher.matches("java.lang.Long") should === (true)
    matcher.matches("java/lang/Integer") should === (false)
    matcher.matches("java/lang/Long") should === (false)
    matcher.matches("java/lang/String") should === (false)
    matcher.matches("java.lang.String") should === (false)
  }

  test("StringMethodMatcher should match methods") {
    val matcher = MethodMatcher.MethodName("valueOf")
    matcher.matches("valueOf", "") should === (true)
    matcher.matches("valueOf", "") should === (true)
    matcher.matches("toString", "") should === (false)
  }

  test("RegexMethodMatcher should match methods") {
    val matcher =  MethodMatcher.Regex("valueOf|toString".r.pattern)
    matcher.matches("valueOf", "") should === (true)
    matcher.matches("toString", "") should === (true)
    matcher.matches("", "") should === (false)
  }

  test("FullMethodMatcher should match methods") {
    val matcher =  MethodMatcher.Full(classOf[String].getMethod("valueOf", classOf[Int]))
    matcher.matches("valueOf", Type.getMethodDescriptor(classOf[String].getMethod("valueOf", classOf[Int]))) should === (true)
    matcher.matches("valueOf", Type.getMethodDescriptor(classOf[String].getMethod("valueOf", classOf[Long]))) should === (false)
    matcher.matches("toString", Type.getMethodDescriptor(classOf[String].getMethod("valueOf", classOf[Long]))) should === (false)
  }
}
