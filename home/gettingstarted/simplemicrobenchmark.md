---
layout: default
title: Simple Microbenchmark
permalink: /simplemicrobenchmark/index.html
---


Lets assume we want to write and run a simple microbenchmark which tests the `map` method on the Scala `Range` class.
This section shows the basics of how to do this.


## Preparatory steps

ScalaMeter requires at least JRE 7 update 4 and Scala 2.10 to be run.

1. Make sure you have at least JRE 7 update 4 installed on your machine.
[Download](http://www.java.com) the latest version or update an existing one.

2. Make sure you have at least Scala 2.10 installed on your machine.
[Download](http://www.scala-lang.org/downloads) and install Scala 2.10 if you don't have a newer version.

3. Go to the [download](/home/download/) section and download the latest release of ScalaMeter.

4. Create a new project and a new file named `RangeMicrobenchmark.scala` in your editor.


## Implementing the microbenchmark

Start with the following `import` statement:

    import org.scalameter.api._

This will give you access to most of the ScalaMeter API.
Alternatively, you can import different ScalaMeter abstractions one by one, but this will do for now.

A ScalaMeter represents performance tests with the `PerformanceTest` abstract class -- to implement a
performance test, we have to extend this class.
A performance test can be a singleton `class` or a `object`.
The only difference from ScalaMeter's point of view is that `object` performance tests will be 
runnable applications which automatically get a `main` method.

We choose the latter:

    object RangeBenchmark
    extends PerformanceTest.Microbenchmark {

The `PerformanceTest` abstract class is a highly configurable test template which allows more than
we need right now.
Instead of inheriting it directly, we inherit the predefined class called `PerformanceTest.Microbenchmark`
which is a performance test configured to simply run the tests and output them in the terminal.

Most benchmarks need input data that they are executed for.
To allow defining input data in a clean and composable manner ScalaMeter introduces data generators
represented by the `Gen` interface.
These generators are similar to the ones in tools like [ScalaCheck](https://github.com/rickynils/scalacheck/wiki/User-Guide)
in that they are composable with `for`-comprehensions.
There are also some differences.








