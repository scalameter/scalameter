package org.scalameter

import org.scalameter.Measurer._
import org.scalameter.execution.invocation.InvocationCountMatcher
import org.scalameter.execution.invocation.InvocationCountMatcher.{MethodMatcher, ClassMatcher}
import org.scalameter.execution.invocation.instrumentation.MethodSignature

import scala.util.Try


class InvocationCountTest extends MeasurerTest[Map[String, Long], InvocationCount] {
  test("BoxingCount with single type should count only autoboxing of this type") {
    measureWith(BoxingCount(classOf[Double])) {
      List(1.0, 2f, true, 6.0, "aaaa", Nil)
    } (_.valuesIterator.sum should === (2))

    measureWith(BoxingCount(classOf[Double])) {
      List(1.0, 2f, true, 6.0, "aaaa", Nil, 56.7d)
    } (_.valuesIterator.sum should === (3))
  }

  test("BoxingCount.all() should count autoboxing of all primitive types") {
    measureWith(BoxingCount.all()) {
      List(1.0d, 2f, true, 5: Byte, -125: Short, 5754, 432523l, 'a', "aaaa", Nil)
    } (_.valuesIterator.sum should === (8))

    measureWith(BoxingCount.all()) {
      List(1.0d, 2f, true, 5: Byte, -125: Short, 5754, 432523l, 'a', "aaaa", Nil, 'b')
    } (_.valuesIterator.sum should === (9))
  }

  test("MethodInvocationCount with matcher without any method pattern should count specific class allocations") {
    measureWith(MethodInvocationCount(InvocationCountMatcher.allocations(classOf[Range]))) {
      val r = 0 until 10
      r.map(_ + 1)
      1 to 10
      new Range(0, 9, 1)
      new Range(-1, 1, 1) ++ List(1, 2, 3)
      List(5, 6, 7, 8, 9)
    } (_.valuesIterator.sum should === (4))

    measureWith(MethodInvocationCount(InvocationCountMatcher.allocations(classOf[Range]))) {
      val r = 0 until 10
      r.map(_ + 1)
      1 to 10
      new Range(0, 9, 1)
      new Range(-1, 1, 1) ++ List(1, 2, 3)
      List(5, 6, 7, 8, 9)
      11 to 20
    } (_.valuesIterator.sum should === (5))
  }

  test("MethodInvocationCount with ClassMatcher.Descendants should match all descendants") {
    measureWith(new MethodInvocationCount(InvocationCountMatcher(
      classMatcher = ClassMatcher.Descendants(classOf[List[_]], direct = false, withSelf = false),
      methodMatcher = MethodMatcher.MethodName("head")
    ))) {
      List(1, 2, 3).head
      Vector(5, 6).head
      Set(1, 4).head
      Try(Nil.head)
    } (_.valuesIterator.sum should === (2))

    measureWith(new MethodInvocationCount(InvocationCountMatcher(
      classMatcher = ClassMatcher.Descendants(classOf[collection.Seq[_]], direct = false, withSelf = false),
      methodMatcher = MethodMatcher.MethodName("head")
    ))) {
      List(1, 2, 3).head
      Vector(5, 6).head
      Set(1, 4).head
      Try(Nil.head)
    } (_.valuesIterator.sum should === (3))

    measureWith(new MethodInvocationCount(InvocationCountMatcher(
      classMatcher = ClassMatcher.Descendants(classOf[collection.Seq[_]], direct = false, withSelf = false),
      methodMatcher = MethodMatcher.MethodName("head")
    ))) {
      List(1, 2, 3).head
      Vector(5, 6).head
      Set(1, 4).head
      Try(Nil.head)
      Stream(7, 8, 9).head
    } (_.valuesIterator.sum should === (4))
  }
}
