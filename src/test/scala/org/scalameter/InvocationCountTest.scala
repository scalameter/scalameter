package org.scalameter

import org.scalameter.Measurer._
import org.scalameter.execution.invocation.InvocationCountMatcher


class InvocationCountTest extends MeasurerTest[InvocationCount] {
  test("BoxingCount with single type should count only autoboxing of this type") {
    measureWith(BoxingCount(classOf[Double])) {
      List(1.0, 2f, true, 6.0, "aaaa", Nil)
    } (_ should === (2.0))

    measureWith(BoxingCount(classOf[Double])) {
      List(1.0, 2f, true, 6.0, "aaaa", Nil, 56.7d)
    } (_ should === (3.0))
  }

  test("BoxingCount.all() should count autoboxing of all primitive types") {
    measureWith(BoxingCount.all()) {
      List(1.0d, 2f, true, 5: Byte, -125: Short, 5754, 432523l, 'a', "aaaa", Nil)
    } (_ should === (8.0))

    measureWith(BoxingCount.all()) {
      List(1.0d, 2f, true, 5: Byte, -125: Short, 5754, 432523l, 'a', "aaaa", Nil, 'b')
    } (_ should === (9.0))
  }

  test("MethodInvocationCounting with matcher without any method pattern should count specific class allocations") {
    measureWith(MethodInvocationCount(InvocationCountMatcher.allocations(classOf[Range]))) {
      val r = 0 until 10
      r.map(_ + 1)
      1 to 10
      new Range(0, 9, 1)
      new Range(-1, 1, 1) ++ List(1, 2, 3)
      List(5, 6, 7, 8, 9)
    } (_ should === (4.0))

    measureWith(MethodInvocationCount(InvocationCountMatcher.allocations(classOf[Range]))) {
      val r = 0 until 10
      r.map(_ + 1)
      1 to 10
      new Range(0, 9, 1)
      new Range(-1, 1, 1) ++ List(1, 2, 3)
      List(5, 6, 7, 8, 9)
      11 to 20
    } (_ should === (5.0))
  }
}
