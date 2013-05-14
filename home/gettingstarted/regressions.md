---
layout: default
title: Performance regression testing
permalink: /regressions/index.html

partof: getting-started
num: 6
---



This section describes how to write performance regression tests in ScalaMeter.
In particular, it focuses on how to do *performance test tuning*.
This encompasses two things.
First, the test should have an appropriate running time.
This means that it should be big enough to be observable,
and not too long, so that it completes relatively quickly.
Second, the test results should be reproducible.
This means that you might have to tweak the configuration
so that the running time can be reproduced across different
test invocations.

Test tuning is something we do with unit tests as well -- we have to at least run them
in order to ensure they pass the first time.
With performance tests the time you invest to tune the test becomes more noticeable.

<div class="remark">
<p class="remarktitle">Note</p>
<p>
This is not a holy grail of performance testing.
<br/>
Just like normal unit testing does not test your code against your specification,
and inevidably leaves some of the cases left out, the performance regression testing
is likely to overlook certain kinds of performance regressions, depending on how you
write your tests.
The goal of this section is to show how you can achieve reasonable test coverage, while
having repeatable and reliable tests, with as few false positives as possible.
</p>
</div>

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
This kind of a test has several smart facilities already in place to
ensure more stable and reproducible testing.
One of them is garbage collection cycle detection -- measurements during
which a GC occurs are discarded.
For those benchmarks where a GC is inherent, once there are more
discarded measurements than the desired number of measurements, all
of them become viable measurements (this is to ensure termination).
We discuss other facilities in more depth below.

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

    :::Summary of regression test results - OverlapIntervals():::
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

    :::Summary of regression test results - OverlapIntervals():::
    Test group: Array.foreach
    - Array.foreach.Test-0 measurements:
      - at size -> 1000000, 1 alternatives: passed
        (ci = <3.00, 3.00>, significance = 1.0E-10)
      - at size -> 3000000, 1 alternatives: passed
        (ci = <8.84, 9.89>, significance = 1.0E-10)
      - at size -> 5000000, 1 alternatives: passed
        (ci = <14.98, 16.07>, significance = 1.0E-10)

However, after a few runs of the test on our machine, the test fails!

    :::Summary of regression test results - OverlapIntervals():::
    Test group: Array.foreach
    - Array.foreach.Test-0 measurements:
      - at size -> 1000000, 5 alternatives: passed
        (ci = <2.88, 4.28>, significance = 1.0E-10)
      - at size -> 3000000, 5 alternatives: failed
        (ci = <10.00, 11.77>, significance = 1.0E-10)
          Failed confidence interval test: <-2.43, -0.35>
          Previous (mean = 9.50, stdev = 0.51, ci = <8.95, 10.05>): 9, 9, 9,
             9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 10, 10, 10, ...
          Latest   (mean = 10.89, stdev = 0.82, ci = <10.00, 11.77>): 10, 10, 10,
             10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, ...
    ...

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

So, how do we fight this?
There are several things we can do.

1. We can run several JVM instances per each test, instead of running just a single one.
The probability that each of them will perform badly greatly decreases.

2. We can reinstantiate the data for the test (in this case the array), multiple times.
Different allocation patterns can yield very different results.

3. We can add artificial Gaussian noise to each measurement in the series (this is
controlled by the `exec.noise.magnitude` parameter).
As a result, the confidence interval of each test will increase, making the tests less
sensitive to random variations.

4. We can artifically increase confidence intervals while doing a regression check (this
is controlled by the `reports.regression.noiseMagnitude` parameter).
This again results in the tests being less sensitive to random variations.

We can experiment easily with the first option.
After setting the number of independent samples to `6` we are no longer able to reproduce
the effect by running the benchmark many times.
We also note that the confidence intervals now became larger, since there is a higher
degree of variance in the tests.

Before we move on to the next example, try to experiment with different `foreach` bodies
to see how the code in the body affects the observed performance, and which point you can
trigger a performance regression failure.

Next, lets try to benchmark the `map` method on `List`s.
The nature of this benchmark is very different than the previous one, because it will
allocate many small objects during the course of its run.

    val sizes = Gen.range("size")(1000000, 2000000, 500000)
  
    val lists = for (sz <- sizes) yield (0 until sz).toList
  
    performance of "List" in {
      measure method "map" in {
        using(lists) config (
          exec.benchRuns -> 20,
          exec.independentSamples -> 1
        ) in { xs =>
          xs.map(_ + 1)
        }
      }
    }

Note that we now do `20` repetitions within `1` JVM instance.

Running this test yields:

    :::Summary of regression test results - OverlapIntervals():::
    Test group: List.map
    - List.map.Test-0 measurements:
      - at size -> 1000000, 1 alternatives: passed
        (ci = <1.60, 27.60>, significance = 1.0E-10)
      - at size -> 1500000, 1 alternatives: passed
        (ci = <12.59, 29.51>, significance = 1.0E-10)
      - at size -> 2000000, 1 alternatives: passed
        (ci = <17.85, 26.95>, significance = 1.0E-10)

The running times obviously have a high degree of variance, since the confidence
intervals range up to `30 ms`!

We use the `-verbose` option to see a bit more details about what happened.
In particular, the running times from the verbose output look like this:

    size -> 1000000: 9, 9, 9, 9, 9, 9, 10, 10, 17, 17, 17,
      17, 18, 18, 18, 18, 19, 19, 19, 21
    size -> 1500000: 14, 17, 18, 19, 19, 19, 19, 20, 21, 21,
      21, 21, 23, 23, 23, 24, 24, 25, 25, 25
    size -> 2000000: 20, 20, 20, 20, 21, 21, 22, 22, 22, 23,
      23, 23, 23, 23, 24, 24, 24, 24, 24, 25

There's our problem.
The running times differ by a great deal.
This may be due to a number of factors, but typically it's due to the fact that
garbage collection cycles have been occurring during repetitions and/or the
list on which a `map` is called has been allocated in a particularly bad memory
pattern, making its traversal slow.

Each `Executor` has a `Measurer` component which is responsible for instantiating
the test input and running the tests.
This component decides how often it should call the generator to reinstantiate the
value for the test, in this case the list.
Doing this too often slows down your test, and never doing it may result in always
measuring the badly allocated list.
In the `Regression` benchmarks it does this every `12` repetitions by default,
but you can customize this:

    exec.reinstantiation.frequency -> 2

By instantiating the list more often running times stabilize, as we can see from
the confidence intervals:

    :::Summary of regression test results - OverlapIntervals():::
    Test group: List.map
    - List.map.Test-0 measurements:
      - at size -> 1000000, 1 alternatives: passed
        (ci = <7.74, 12.26>, significance = 1.0E-10)
      - at size -> 1500000, 1 alternatives: passed
        (ci = <13.35, 18.95>, significance = 1.0E-10)
      - at size -> 2000000, 1 alternatives: passed
        (ci = <16.93, 23.17>, significance = 1.0E-10)

We can also see this from the verbose output:

    size -> 1000000: 9, 9, 9, 9, 9, 9, 10, 10, 10, 10, 10,
      10, 10, 10, 11, 11, 11, 11, 11, 11
    size -> 1500000: 15, 15, 15, 15, 15, 16, 16, 16, 16,
      16, 16, 16, 16, 16, 17, 17, 17, 17, 17, 19
    size -> 2000000: 18, 19, 19, 19, 19, 19, 20, 20, 20,
      20, 20, 20, 20, 20, 20, 21, 21, 22, 22, 22

<div class="remark">
<p class="remarktitle">Note</p>
<p>
You may conclude from the verbose output that the running times were increasing
for some strange reason.
This is not the case at all -- the reason they are sorted is that it was done by
the previously mentioned `Measurer` component to eliminate outliers.
</p>
</div>

Lets push the envelope a little bit this time -- lets test a `groupBy` method which
should be very allocation intensive, and lets increase the input test size:

    val sizes = Gen.single("size")(5000000)
  
    val lists = for (sz <- sizes) yield (0 until sz).toList
  
    performance of "List" in {
      measure method "groupBy" in {
        using(lists) config (
          exec.benchRuns -> 20,
          exec.independentSamples -> 1
        ) in { xs =>
          xs.groupBy(_ % 10)
        }
      }
    }

The output is:

    :::Summary of regression test results - OverlapIntervals():::
    Test group: List.groupBy
    - List.groupBy.Test-0 measurements:
      - at size -> 5000000, 1 alternatives: passed
        (ci = <29.48, 352.02>, significance = 1.0E-10)

A confidence interval of this size is almost useless -- we should try to stabilize the
running times which differ by nearly `200%`:

    size -> 5000000: 125, 142, 144, 145, 146, 148, 149, 155,
      162, 164, 164, 164, 205, 210, 218, 224, 277, 281, 282, 310

If we examine the verbose output a bit more, we might notice something like this:

    measurements: 218, 164, 281, 224, 164, 310, 818, 142, 210, 146, 277,
      205, 149, 282, 155, 148, 144, 162, 145, 164
    Detected 1 outlier(s): 142, 144, 145, 146, 148, 149, 155, 162, 164, 164,
      164, 205, 210, 218, 224, 277, 281, 282, 310, 818
    measurements: 125
    After outlier elimination: 125, 142, 144, 145, 146, 148, 149, 155, 162, 164,
      164, 164, 205, 210, 218, 224, 277, 281, 282, 310

The `Measurer` component of the executor has done some measurements above, then sorted
them and noticed that the last measurement is extremely different compared to the others.
It then eliminated that measurement and redid it.
This process is called *outlier elimination*.

We can take advantage of the outlier elimination to stabilize our tests.
What the outlier elimination does is it takes a look at the suffixes of the sorted set of
measurements.
If it observes that some suffix of measurements changes the [variance](http://en.wikipedia.org/wiki/Variance)
of the entire set of measurements by more than some factor (called *coefficient of variance multiplier*) then
it discards that suffix and redoes those measurements.
The process is repeated up to a certain number of times.

We can notice three parameters in this description:

- `exec.outliers.suspectPercent` -- the percentage size of the largest suffix to examine
- `exec.outliers.covMultiplier` -- the minimum factor by which removing the suffix changes the variance
- `exec.outliers.retries` -- the number of times to repeat the process

Lets tweak these a bit.

    exec.outliers.covMultiplier -> 1.5,
    exec.outliers.suspectPercent -> 40

We show the result below:

    Detected 1 outlier(s): 140, 141, 143, ..., 206, 211, 268, 270, 275, 288, 876
    All GC time ignored, accepted: 1, ignored: 0
    measurements: 119
    Detected 4 outlier(s): 119, 140, 141, ..., 203, 206, 211, 268, 270, 275, 288
    All GC time ignored, accepted: 4, ignored: 7
    measurements: 199, 144, 144, 140
    Detected 5 outlier(s): 119, 140, 140, ..., 162, 166, 199, 203, 203, 206, 211
    Some GC time recorded, accepted: 5, ignored: 7
    measurements: 324, 1466, 140, 250, 153
    Detected 1 outlier(s): 119, 140, 140, ..., 156, 162, 162, 166, 250, 324, 1466
    Some GC time recorded, accepted: 1, ignored: 2
    measurements: 157
    Detected 1 outlier(s): 119, 140, 140, ..., 156, 157, 162, 162, 166, 250, 324
    Some GC time recorded, accepted: 1, ignored: 2
    measurements: 239
    Detected 2 outlier(s): 119, 140, 140, ..., 156, 157, 162, 162, 166, 239, 250
    Some GC time recorded, accepted: 2, ignored: 4
    measurements: 260, 140
    Detected 1 outlier(s): 119, 140, 140, ..., 153, 156, 157, 162, 162, 166, 260
    Some GC time recorded, accepted: 1, ignored: 2
    measurements: 290
    Detected 1 outlier(s): 119, 140, 140, ..., 153, 156, 157, 162, 162, 166, 290
    All GC time ignored, accepted: 1, ignored: 1
    measurements: 147
    After outlier elimination: 119, 140, ..., 147, 153, 156, 157, 162, 162, 166

At the expense of an increased running time, we've obtained more stable results:

    :::Summary of regression test results - OverlapIntervals():::
    Test group: List.groupBy
    - List.groupBy.Test-0 measurements:
      - at size -> 5000000, 1 alternatives: passed
        (ci = <116.84, 176.46>, significance = 1.0E-10)

This should demonstrate that there are tests which are inherently unstable and that you can sometimes
work around those instabilities by configuring how you do the measurement.
Generally, understanding why the test is unstable and what is the cause of noise is very helpful in
eliminating it.
When trying to figure out the cause of an instability, always try to ask questions like:

- is my test allocation intensive?
- does my test allocate a huge chunk of memory?
- what is the memory access pattern in my test?
- does my test inherently trigger garbage collection cycles?
- does my test allocate too much memory?

In the next section we examine different [executors](/scalameter/home/gettingstarted/executors/) in more detail.


<div class="imagenoframe">
  <img src="/scalameter/resources/images/logo-yellow-small.png"></img>
</div>



























