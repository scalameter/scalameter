---
layout: default
title: Performance regression testing
permalink: /regressions/index.html

partof: getting-started
num: 6
---


Lets write a performance regression test the `foreach` method on the `Array` class.

    import org.scalameter.api._
    
    class RegressionTest extends PerformanceTest.Regression {
      def persistor = new SerializationPersistor
      val sizes = Gen.range("size")(1000000, 5000000, 2000000)
      val arrays = for (sz <- sizes) yield (0 until sz).toArray

      performance of "Array" in {
        measure method "foreach" in {
          using(arrays) config (
            exec.independentSamples -> 1
          ) in { xs =>
            var sum = 0
            xs.foreach(x => sum += x)
          }
        }
      }    
    }

We extend the `PerformanceTest.Regression` class this time, which is
a preconfigured template for doing regression testing.
Since a regression test needs a basis for comparison, it will continually
persist running time results each time it is run.
This persistence is done by the `persistor` part of the testing pipeline
we've mentioned earlier.
We instantiate a simple `SerializationPersistor` which will serialize the
results and store them into files in the `reports.resultDir` directory.

Next, we define our input data, specifically, the `arrays` generator.
We then define a test group `Array` and a nested test group `foreach`,
within which we put our test.

    using(arrays) config (
      exec.independentSamples -> 1
    ) in { xs =>
      var sum = 0
      xs.foreach(x => sum += x)
    }

The test itself uses the `arrays` generator in a snippet in a way we've seen this before.
A novelty here is a new parameter in the `config` block.
The `Regression` class uses the `SeparateJvmsExecutor` -- this executor invokes
multiple instances of the JVM, warms up each one and runs the tests.
The tests are by default divided between `9` JVM instances.
You can set the number of JVM instances with the `exec.independentSamples` parameter.

    :::Summary of regression test results - ConfidenceIntervals(true):::
    Test group: Array.foreach
    - Array.foreach.Test-0 measurements:
      - at size -> 1000000, 1 alternatives: passed
        (ci = <3.00, 3.00>, significance = 1.0E-10)
      - at size -> 3000000, 1 alternatives: passed
        (ci = <8.95, 10.05>, significance = 1.0E-10)
      - at size -> 5000000, 1 alternatives: passed
        (ci = <14.90, 16.21>, significance = 1.0E-10)

The first time we run the test, there is not history for it yet.
This means that the test will be successful irrespective of the running time.
The next time we run the test, we might get entirely different running times:

    :::Summary of regression test results - ConfidenceIntervals(true):::
    Test group: Array.foreach
    - Array.foreach.Test-0 measurements:
      - at size -> 1000000, 1 alternatives: passed
        (ci = <3.00, 3.00>, significance = 1.0E-10)
      - at size -> 3000000, 1 alternatives: passed
        (ci = <8.84, 9.89>, significance = 1.0E-10)
      - at size -> 5000000, 1 alternatives: passed
        (ci = <14.98, 16.07>, significance = 1.0E-10)

However, after a few runs of the test on our machine, the test fails!

    :::Summary of regression test results - ConfidenceIntervals(true):::
    Test group: Array.foreach
    - Array.foreach.Test-0 measurements:
      - at size -> 1000000, 5 alternatives: passed
        (ci = <2.88, 4.28>, significance = 1.0E-10)
      - at size -> 3000000, 5 alternatives: failed
        (ci = <10.00, 11.77>, significance = 1.0E-10)
          Failed confidence interval test: <-2.43, -0.35>
          Previous (mean = 9.50, stdev = 0.51, ci = <8.95, 10.05>): 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10
          Latest   (mean = 10.89, stdev = 0.82, ci = <10.00, 11.77>): 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 15

It appears that our latest run of the benchmark is consistently slightly slower in that JVM instance.
Effects like this are hard to predict -- they may be due to a memory allocation pattern, decisions
taken by the JIT compiler when optimizing the code or something entirely different.

The result times of failed tests are not saved into history and running the test again ends
successfully on our machine.

<div class="remark">
<p class="remarktitle">Note</p>
<p>
Reproducing this effect really depends on your environment.
It may happen every single time you run the test, or it may not happen at all.
Make sure that you have nothing in your environment that affects CPU performances -- check for
background processes or that your laptop is not running on battery power.
<br/>
If it does not happen at all for this example, you might have a different JVM or OS version.
</p>
</div>

In the next section we examine different [executors](/scalameter/home/gettingstarted/executors/) in more detail.


<div class="imagenoframe">
  <img src="/scalameter/resources/images/logo-yellow-small.png"></img>
</div>






