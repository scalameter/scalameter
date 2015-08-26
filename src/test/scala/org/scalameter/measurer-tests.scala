package org.scalameter

import org.scalameter.examples._
import org.scalatest.FunSuite

class DefaultQuickBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new DefaultQuickBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class DefaultMicroBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new DefaultMicroBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class IgnoringGCQuickBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new IgnoringGCQuickBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class IgnoringGCMicroBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new IgnoringGCMicroBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class MemoryQuickBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new MemoryQuickBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class MemoryMicroBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new MemoryMicroBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class GCCountQuickBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new GCCountQuickBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}

class GCCountMicroBenchTest extends FunSuite {
  test("Measurer.Default on LocalExecutor should execute correctly") {
    try {
      new GCCountMicroBench executeTests()
    } catch { case t: Throwable =>
      t.printStackTrace()
      throw t
    }
  }
}
