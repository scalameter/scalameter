package org.scalameter



import org.scalameter.examples.BoxingCountBench
import org.scalameter.examples.MethodInvocationCountBench



class BoxingCountBenchTest extends InvocationCountMeasurerTest {
  test("BoxingCountTest.all should be deterministic") {
    checkInvocationCountMeasurerTest(new BoxingCountBench)
  }
}

class MethodInvocationCountBenchTest extends InvocationCountMeasurerTest {
  test("MethodInvocationCountTest.allocations should be deterministic") {
    checkInvocationCountMeasurerTest(new MethodInvocationCountBench)
  }
}
