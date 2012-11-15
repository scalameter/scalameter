---
layout: default
title: Reporters
permalink: /reporters/index.html
---


There are several predefined `Reporter`s available in ScalaMeter.
An abstract `Reporter` trait defines two overloaded methods called `report`:

    trait Reporter {
      def report(result: CurveData, persistor: Persistor): Unit
      def report(results: Tree[CurveData], persistor: Persistor): Unit
    }

The first `report` method is invoked by ScalaMeter whenever an `Executor` completes
any single test.
The second `report` method is invoked at the end when all of the tests are finished
executing.
If you plan to implement your own `Reporter` from scratch, then you should take
a look at the [ScalaMeter API](/home/api) to better understand what the parameters
are, as well as how other important ScalaMeter datatypes work.
<br/>
Otherwise, there are plenty of predefined reporters to choose from.


## Logging reporter

Outputs the results of the tests into the terminal.
This reporter is a fine choice when you need to output the results
of your benchmark somewhere quickly.


### Constructor arguments: (none)


### Configuration: (none)


## Chart reporter

Creates a chart for each test group.
The chart can be a simple XY line chart, a histogram, a comparison of confidence
intervals, a 3D chart or something else.
A PNG file is created for each chart.


Constructor arguments:

- `drawer` -- a `ChartFactory` determines the type of a chart to render


Configuration:

- `reports.resultDir` -- the directory in which the PNG files are generated


### Chart factories

`ChartFactory` is a common supertrait for objects that create charts.
You can define your own chart factories by implementing this
trait.
There are also several predefined types of chart factories.

`ChartFactory.XYLine` renders a line chart for each curve in the test group.
This factory only works correctly for data which depends on a single
input axis which represents numeric value.
For example, it works for the following generator:

    for {
      size <- Gen.range("sizes")(200, 1000, 100)
    } yield Array(0 until size: _*)

where there is a single input axis `"size"`, but it does not work for:

    for {
      size <- Gen.range("sizes")(200, 1000, 100)
      offset <- Gen.range("offset")(0, 1000, 100)
    } yield Array((0 until size).map(_ + offset): _*)

where there are two input axes `"size"` and `"offset"`.

`ChartFactory.ConfidenceIntervals` renders a confidence interval for each curve
and the cummulative confidence interval of the previous runs of the same benchmarks.
This is very useful when visually comparing the two alternatives since it
gives insight not only in the mean value of each run, but also in the
variance in the measurements.

This factory also only works correctly only for 2D data.


## HTML reporter



## Regression reporter



In the next section we examine different [executors](/home/gettingstarted/executors/) in more detail.





<div class="imagenoframe">
  <img src="/resources/images/logo-yellow-small.png"></img>
</div>

















