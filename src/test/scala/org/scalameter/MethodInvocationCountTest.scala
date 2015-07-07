package org.scalameter

class MethodInvocationCountTest extends InvocationCountMeasurerTest {
  test("MethodInvocationCountTest.allocations should be deterministic") {
    checkInvocationCountMeasurerTest(new org.scalameter.examples.MethodInvocationCountTest1)
  }
}
