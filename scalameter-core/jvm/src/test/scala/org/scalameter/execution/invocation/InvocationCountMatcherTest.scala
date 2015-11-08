package org.scalameter.execution.invocation

import org.objectweb.asm.Type
import org.scalatest.{FunSuite, Matchers}
import org.scalameter.execution.invocation.InvocationCountMatcher._


class InvocationCountMatcherTest extends FunSuite with Matchers {
  test("ClassMatcher.ClassName should match classes") {
    val matcher = ClassMatcher.ClassName(classOf[java.lang.Integer])
    matcher.matches("java.lang.Integer") should === (true)
    matcher.matches("java/lang/Integer") should === (false)
    matcher.matches("java.lang.Long") should === (false)
    matcher.matches("java/lang/Long") should === (false)
  }

  test("ClassMatcher.Regex should match classes") {
    val matcher =  ClassMatcher.Regex("(java\\.lang\\.Integer)|(java\\.lang\\.Long)".r.pattern)
    matcher.matches("java.lang.Integer") should === (true)
    matcher.matches("java.lang.Long") should === (true)
    matcher.matches("java/lang/Integer") should === (false)
    matcher.matches("java/lang/Long") should === (false)
    matcher.matches("java/lang/String") should === (false)
    matcher.matches("java.lang.String") should === (false)
  }

  test("ClassMatcher.Descendants should match classes") {
    val matcher1 = ClassMatcher.Descendants(classOf[collection.immutable.LinearSeq[_]], direct = false, withSelf = false)
    matcher1.matches("scala.collection.immutable.LinearSeq") should === (false)
    matcher1.matches("scala.collection.immutable.List") should === (true)
    matcher1.matches("scala.collection.immutable.Nil$") should === (true)
    matcher1.matches("scala.collection.immutable.$colon$colon") should === (true)

    val matcher2 = ClassMatcher.Descendants(classOf[collection.immutable.LinearSeq[_]], direct = true, withSelf = false)
    matcher2.matches("scala.collection.immutable.LinearSeq") should === (false)
    matcher2.matches("scala.collection.immutable.List") should === (true)
    matcher2.matches("scala.collection.immutable.Nil$") should === (false)
    matcher2.matches("scala.collection.immutable.$colon$colon") should === (false)

    val matcher3 = ClassMatcher.Descendants(classOf[collection.immutable.LinearSeq[_]], direct = false, withSelf = true)
    matcher3.matches("scala.collection.immutable.LinearSeq") should === (true)
    matcher3.matches("scala.collection.immutable.List") should === (true)
    matcher3.matches("scala.collection.immutable.Nil$") should === (true)
    matcher3.matches("scala.collection.immutable.$colon$colon") should === (true)

    val matcher4 = ClassMatcher.Descendants(classOf[collection.immutable.LinearSeq[_]], direct = true, withSelf = true)
    matcher4.matches("scala.collection.immutable.LinearSeq") should === (true)
    matcher4.matches("scala.collection.immutable.List") should === (true)
    matcher4.matches("scala.collection.immutable.Nil$") should === (false)
    matcher4.matches("scala.collection.immutable.$colon$colon") should === (false)
  }

  test("MethodMatcher.MethodName should match methods") {
    val matcher = MethodMatcher.MethodName("valueOf")
    matcher.matches("valueOf", "") should === (true)
    matcher.matches("valueOf", "") should === (true)
    matcher.matches("toString", "") should === (false)
  }

  test("MethodMatcher.Regex should match methods") {
    val matcher =  MethodMatcher.Regex("valueOf|toString".r.pattern)
    matcher.matches("valueOf", "") should === (true)
    matcher.matches("toString", "") should === (true)
    matcher.matches("", "") should === (false)
  }

  test("MethodMatcher.Full should match methods") {
    val matcher =  MethodMatcher.Full(classOf[String].getMethod("valueOf", classOf[Int]))
    matcher.matches("valueOf", Type.getMethodDescriptor(classOf[String].getMethod("valueOf", classOf[Int]))) should === (true)
    matcher.matches("valueOf", Type.getMethodDescriptor(classOf[String].getMethod("valueOf", classOf[Long]))) should === (false)
    matcher.matches("toString", Type.getMethodDescriptor(classOf[String].getMethod("valueOf", classOf[Long]))) should === (false)
  }
}
