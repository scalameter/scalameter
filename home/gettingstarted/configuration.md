---
layout: default
title: Test configuration
permalink: /configuration/index.html
---


In this section we will see how to configure how performance tests are executed,
how results are reported as well as tweak a range of test parameters
selectively.
Before we start, we note that we will be running all the tests from within
an SBT project from now on.
This might be a good idea to read up on [SBT integration](/home/gettingstarted/sbt/)
if you haven't already.

## Execution

To have a better grasp on how ScalaMeter works, it's important to understand
its test execution pipeline.
This pipeline conceptually consists of 4 major parts which are executed in the
following order:

1. Defining tests with a **DSL**
2. Executing tests with an **executor**
3. Reporting test results with a **reporter**
4. Persisting test results with a **persistor**

To explore each of these parts in more depth, we will modify the `RangeBenchmark`
from the <a href="/home/gettingstarted/simplemicrobenchmark">Simple benchmark</a> section.
We will no longer inherit the `PerformanceTest.Quickbenchmark` class,
but `PerformanceTest` directly.
Doing this requires that we manually define three parts of the testing pipeline,
namely, the members `executor`, `reporter` and `persistor`.

    import org.scalameter.api._
    
    object RangeBenchmark extends PerformanceTest {
    
      /* configuration */
    
      lazy val executor = LocalExecutor(Aggregator.min, new Measurer.Default)
      lazy val reporter = new LoggingReporter
      lazy val persistor = Persistor.None
    
      /* inputs */
    
      val sizes = Gen.range("size")(300000, 1500000, 300000)
    
      val ranges = for {
        size <- sizes
      } yield 0 until size
    
      /* tests */
    
      performance of "Range" in {
        measure method "map" in {
          using(ranges) in {
            r => r.map(_ + 1)
          }
        }
      }
    }

We've configured these three parts in exactly the same way as they are defined in the previously
inherited `PerformanceTest.Quickbenchmark` class.
The `executor` decides how the tests are executed, how the measurements are done and how the results
are interpreted.
We intend to run the tests in the same JVM instance as ScalaMeter, so we instantiate a `LocalExecutor`.
We want to take the minimum running time of all the benchmarks run for each size, so we set the
`Aggregator` for the executor to `Aggregator.min`.
We just want to do a fixed number of measurements for each size, so we set
the `Measurer` to `Measurer.Default`.

The `reporter` creates reports based on the results -- we want to output results to the terminal,
so we instantiate the `LoggingReporter`.
The `persistor` saves the results of the test.
We don't need this functionality right now, so we just use `Persistor.None`, which does absolutely
nothing.

We can now run the benchmark in SBT as follows:

    > test-only sctest.RangeBenchmark
    [info] Compiling 1 Scala source to /tmp/test/target/scala-2.10/test-classes
    [info] ::Benchmark Range.map::
    [info] jvm-name: Java HotSpot(TM) 64-Bit Server VM
    [info] jvm-vendor: Oracle Corporation
    [info] jvm-version: 23.0-b16
    [info] os-arch: amd64
    [info] os-name: Mac OS X
    [info] Parameters(size -> 300000): 5.0
    [info] Parameters(size -> 600000): 10.0
    [info] Parameters(size -> 900000): 15.0
    [info] Parameters(size -> 1200000): 20.0
    [info] Parameters(size -> 1500000): 25.0

Whoa!
What happened just now?
We are still running the benchmark on exactly the same machine and environment as before, but it seems
that the running times have changed by a factor of more than `2` with respect to the run from the
previous section:

    Parameters(size -> 300000): 2.0
    Parameters(size -> 600000): 4.0
    Parameters(size -> 900000): 7.0
    Parameters(size -> 1200000): 16.0
    Parameters(size -> 1500000): 30.0

Could it be that we somehow incorrectly configured the test?
A quick refactoring of our recent changes would reveal that's not the case.
<br/>
So what could it be?

An answer might sound surprising.
The difference between the two runs is that in the previous section we've run the test directly
from the command-line, and now we run them in the same JVM instance in which SBT is running.
JVM loads a lot of different classes into the JVM, many of which may change how our benchmark
code is compiled, whereas running the benchmark through command-line loads only the bare
essentials needed to run ScalaMeter.
Code in some of the SBT classes may be preventing the JIT compiler to apply some optimizations
to our code.<br/>
Orthogonal to that, the two JVM instances probably have entirely different heap sizes -- notice
how the performance was linear when running from SBT, while degraded in the previous benchmark
for larger `Range`s, indicating that memory allocations took more time on average.

To ensure that the tests are run in a separate JVM, we only need to change the `executor`
to a special `MultipleJvmPerSetupExecutor`. This executor will start at least one new JVM instance
for each test and run the tests from the new JVM.
The new JVM has the default heap size set to 2GB.
This way:

    lazy val executor = MultipleJvmPerSetupExecutor(
      Aggregator.min,
      new Measurer.Default
    )

Running the test from SBT now yields the following output:

    > test-only sctest.RangeBenchmark
    [info] ::Benchmark Range.map::
    [info] jvm-name: Java HotSpot(TM) 64-Bit Server VM
    [info] jvm-vendor: Oracle Corporation
    [info] jvm-version: 23.0-b16
    [info] os-arch: amd64
    [info] os-name: Mac OS X
    [info] Parameters(size -> 300000): 2.0
    [info] Parameters(size -> 600000): 4.0
    [info] Parameters(size -> 900000): 6.0
    [info] Parameters(size -> 1200000): 9.0
    [info] Parameters(size -> 1500000): 11.0

We've just eliminated both effects seen earlier.
First, the `Range` code is optimized better just like in the earlier command-line example.
We can notice this with the smaller ranges.
Second, the scaling is now linear just like in the SBT example.
This becomes apparent with the larger ranges.

<div class="remark">
<p class="remarktitle">Note</p>
<p>
Again, these tests were taken on a 4-core 3.4 GHz i7 iMac, Mac OS X 10.7.5,
JRE 7 update 9 and Scala 2.10-RC2.
The SBT heap size was set to 4GB, and the <code>scala</code> runner script default heap size
is 256MB for Scala 2.10-RC2.
Depending on your machine and environment, you might get very different results
than the ones above.
</p>
</div>

The example above serves to demonstrate just how fragile benchmarking on the JVM is,
even with relatively simple code.
An additional observation is that the `Range` class behaves very differently depending
on the program in which it is used.

We will use the `MultipleJvmPerSetupExecutor` from now on, since the results it gives us
are most easily reproducible.
You maybe noticed above that each executor takes additional 2 parameters, namely, the
`Aggregator` and a `Measurer`.
We will get back to them later in this overview.


## Reporting


## Configuring parameters


<div class="imagenoframe">
  <img src="/resources/images/logo-yellow-small.png"></img>
</div>


























