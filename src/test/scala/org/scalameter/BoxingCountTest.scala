package org.scalameter

class BoxingCountTest extends InvocationCountMeasurerTest {
  test("BoxingCountTest.all should be deterministic") {
    checkInvocationCountMeasurerTest(new org.scalameter.examples.BoxingCountTest)
  }
}
