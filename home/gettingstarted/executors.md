---
layout: default
title: Executors
permalink: /executors/index.html

partof: getting-started
num: 7
---


This section describes executors in ScalaMeter in more detail.

Every executor is responsible for executing tests and obtaining the running times
for those tests.
Also, every executor is parametrized by three components, namely:

1. `Warmer` -- warms up the JVM for the test(s)

2. `Aggregator` -- applied to a series of measurements to obtain the definitive result (e.g. a mean value, a minimum value, a median, etc.)

3. `Measurer` -- performs the actual measurements


## Local executor

The local executor executes performance tests in the same JVM from which ScalaMeter was invoked.
This is usually faster and can be useful if tests are needed quickly to compare several alternatives
only roughly.
The downside is that different, seemingly unrelated classes loaded in the same JVM may have influence
performance in unpredictable ways, mainly due to the way that the JIT compiler and dynamic optimizations
work.
This means that adding a new test to the test suite, or invoking the test from a different program
may seemingly change running times.

To warm up the JVM, warmups for all the tests are run first.
This decreases the effect of changing the order of the tests on the performance.
Then, for each test, its warmup is run again and the measurements are performed.

This executor gives results which are hardly reproducible and cannot be used for absolute
performance regression testing.

Configuration:

- `exec.minWarmupRuns` -- the minimum number of warmup runs to perform
- `exec.maxWarmupRuns` -- the maximum number of warmup runs to perform
- `exec.benchRuns` -- the desired number of measurements


## Separate JVMs executor

This executor executes tests in a separate JVM.
Each JVM is first warmed up for the test, then the tests are executed.
<br/>
This is the preferred way of executing performance tests, because the results are reproducible.

Configuration:

- all the parameters seen in the local executor
- `exec.independentSamples` -- the number of separate JVM instances between which the benchmark repetitions are divided


## Measurers

A `Measurer` is at the core of every executor, and it does all the work of running, measuring the running times
of your benchmarks, analyzing them and possibly even changing the environment and inputs for them.
The executor simply tells the measurer how many measurements it wants, and it's the measurer's job to provide
that many measurements, possibly by doing even more repetitions.

There are several predefined measurers.

- `Measurer.Default` simply does as many measurements as the user requests.

- `Measurer.IgnoringGC` does the same thing, but ignores those measurements with a GC cycle.
If the number of measurements with a GC cycle exceeds the number of desired measurements, it stops ignoring them.
This ensures termination.

- `Measurer.PeriodicInstantiation` is a mixin measurer, which can be mixed in with the previous measurers.
It reinstantiates the value for the benchmark every `exec.reinstantiation.frequency` repetitions and will additionaly
perform a GC cycle if `exec.reinstantiation.fullGC` is set to `true`.

- `Measurer.OutlierElimination` is another mixin measurer, which analyzes the measurements returned by other
measurers and possibly discards and repeats measurements.
It is described in more detail in the [regression testing section](/scalameter/home/gettingstarted/regressions/)

- `Measurer.AbsoluteNoise` is a mixin measurer which adds absolute noise to measurements.

- `Measurer.RelativeNoise` is a mixin measurer which adds relative noise to measurements.
The magnitude of the Gaussian noise is determined by the absolute running time multiplied by the
`exec.noise.magnitude / 10`.
Adding noise can make a performance regression test less sensitive.

You can take a look at the ScalaMeter API or source code to figure out how different measurers work in more detail.



<div class="imagenoframe">
  <img src="/scalameter/resources/images/logo-yellow-small.png"></img>
</div>



















