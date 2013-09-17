---
layout: default
title: Reporters
permalink: /reporters/index.html

partof: getting-started
num: 5
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
a look at the [ScalaMeter API](http://lampwww.epfl.ch/~prokopec/scalameter/index.html#package) to better understand what the parameters
are, as well as how other important ScalaMeter datatypes work.
<br/>
Otherwise, there are plenty of predefined reporters to choose from.


## Null reporter

This reporter does not report anything at all.
Any test result it receives will be ignored.
If for whatever reason you don't want any reports, use this reporter.


## Composite reporter

This reporter is simply a composition of several reporters -- when you need to report
results with more than a single reporter, e.g. if you'd like command-line output as well
as chart images for the tests, use this reporter.

Constructor arguments:

- `rs` -- a sequence of other reporters this reporter will

Configuration depends on the test parameters of the reporters in `rs`.


## Logging reporter

Outputs the results of the tests into the terminal.
This reporter is a fine choice when you need to output the results
of your benchmark somewhere quickly.

Constructor arguments: (none)

Configuration: (none)


## Chart reporter

Creates a chart for each test group.
The chart can be a simple XY line chart, a histogram, a comparison of confidence
intervals, a 3D chart or something else.
A PNG file is created for each chart.


Constructor arguments:

- `drawer` -- a `ChartFactory` that determines the type of a chart to render


Configuration:

- `reports.resultDir` -- the directory in which the PNG files are generated


### Chart factories

`ChartReporter.ChartFactory` is a common supertrait for objects that create charts.
You can define your own chart factories by implementing this trait.
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


## DSV reporter

Produces a DSV file for each curve with results that can be used for visualization. Every row represents an average obtained for one parameter combination for a specific test date. Confidence intervals are also included.

Constructor arguments:

- `delimiter` -- the character used to delimit columns

Configuration:

- `reports.resultDir` -- the directory in which the DSV files are stored
- `reports.regression.significance` -- the significance level for the statistical test (described below)


## HTML reporter

Creates an HTML document with reports for all the tests.
This reporter creates an interactive page which gives an overview of all test groups and curves.
The page is capable of rendering charts in SVG format using the [D3.js](http://d3js.org/) library.
Performance data can be filtered by curve, date, and [Generator](/scalameter/home/gettingstarted/generators) dimensions.
All filter parameters are set directly from within the HTML UI.
Permalinks for specific filter configurations can be generated as a simple way of storing or sharing filter parameters.

`HtmlReporter` internally uses a `DsvReporter` to export performance data. It can either be exported to individual files for each curve, or embedded in the generated `data.js` file. The latter is particularly useful in cases where the JavaScript code has no access to the DSV files. This typically happens when opening the generated HTML document from the local file system. Most browsers enforce a [same origin policy](http://en.wikipedia.org/wiki/Same_origin_policy) that prevents JavaScript code from accessing local files.

The `HtmlReporter` has to be used in combination with a `RegressionReporter` (see below) in order to have access to a history of running times. In the composition, `RegressionReporter` has to precede `HtmlReporter` in order for the history to include the most recent run, e.g. like this:

    def reporter: Reporter = Reporter.Composite(
      new RegressionReporter(
        RegressionReporter.Tester.OverlapIntervals(),
        RegressionReporter.Historian.ExponentialBackoff() ),
      HtmlReporter(true)
    )


Constructor arguments:

- `embedDsv` -- when set to `true`, data is embedded in `data.js`, otherwise a separate DSV file is created for each curve

Configuration:

- `reports.resultDir` -- the directory in which the report page and its resources are generated


## Regression reporter

The `RegressionReporter` is a reporter that does performance regression testing.
It compares the running time of each snippet against the previous running times and
does some statistical analysis to decide whether the performance has changed.

If the reporter concludes there are no performance regressions, the running times of
the test are persisted, to be taken into consideration during the subsequent runs of
the test.

This reporter has to be used with a persistor different than `Persistor.None`.
Otherwise, it will have nothing to compare the results against, and it will not be able
to persist the results if the tests are successful.

We will explain this reporter in more detail in the section on [performance regression testing](/scalameter/home/gettingstarted/regressions/),
where we show how to do performance regression tests on a concrete example.
In the meanwhile, we note that a regression reporter takes two parameters, namely, the
`Tester` and the `Historian`.
The former abstracts away the testing methodology, while the latter can prune the running time
history so that it does not grow too large.

Constructor arguments:

- `test` -- the methodology used to detect performance regressions
- `historian` -- the policy used to prune old results

Configuration:

- `reports.resultDir` -- the directory in which the performance results are persisted
- `reports.regression.significance` -- the significance level for the statistical test, which
is equal to `1 - confidenceLevel`, where a confidence level is the probability that the real running
time is in the computed confidence interval (the bottomline is -- the smaller the significance
level, the less likely the test reports false regressions, but may fail to report some real
regressions)


### Testers

The `RegressionReporter.Tester` trait represents the methodology used to test the results.
There are several predefined implementations.

`Tester.Accepter` just accepts the test results each time.
If you need to build a history of running times for the test, this is a tester of choice.

`Tester.ANOVA` applies the [analysis of variance](http://en.wikipedia.org/wiki/Analysis_of_variance)
technique to detect whether there is at least one alternative in the history
which is statistically different from all the other alternatives.
This tester is very sensitive and is particularly applicable if your running time history
has a fixed size (i.e. the oldest results are thrown away -- see historians below).
For unlimited history sizes, it might produce unreliable test results.

`Tester.ConfidenceIntervals` computes the confidence interval for each of the
alternatives in the history and the most recent alternative, as well as the
confidence interval of the difference between the most recent and any previous alternative.
If the confidence interval of the difference includes zero, the performance
test is successful.
The `strict` parameter can be set to `false`, in which case this tester also
passes the test if the confidence intervals of the most recent alternative
and any previous alternative overlap.
This makes the tests more solid and reliable, but the drawback is that certain
types of regressions are harder or impossible to detect.
<br/>
This is a recommended tester, and the one we will use in this overview -- it works well
for histories of any size.


### Historians

The `RegressionReporter.Historian` trait represents the policy for removing the old
test results from the history.
Typically, the history grows every time a test is run, so we may want to limit it
in practice.

`Historian.Complete` preserves the entire history.
If you run the tests relatively rarely or you really want to preserve your entire
history, this simple historian is ideal.

`Historian.Window` maintains a sliding window of the history.
The `size` parameter determines the size of the sliding window -- the number of
previous results is always fixed.
This historian is ideal if you run your tests very often.
The downside is that it may not detect very slow performance regression trends -- if
each running time is only slightly slower than the previous one, the overall regression
trend may be undetected.

`Historian.ExponentialBackoff` prunes the history exponentially.
Assuming that the previous test runs are labeled `0` through `16`, `0` being the oldest:

         1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16

Then the actual running times history that this historian actually preserves is:

         1                       9          13    15 16

If we add an additional measurement labeled `17`, the history becomes:

       1                      9          13    15 16 17

Adding another measurement labeled `18` now eliminates the measurement labeled `16`
because there are "two many" recent measurements:

     1                     9          13    15    17 18

This historian yields histories with unlimited size, but the size of the history
is logarithmic in the number of the tests run.
This detects slow regression trends accurately, while consuming very little space
with each newly run test.
We will mostly be using this historian.

Since performance regression testing may not be completely clear after this high
level overview of the regression reporter, we show a coding example in the next
[section](/scalameter/home/gettingstarted/regressions).



<div class="imagenoframe">
  <img src="/scalameter/resources/images/logo-yellow-small.png"></img>
</div>

















