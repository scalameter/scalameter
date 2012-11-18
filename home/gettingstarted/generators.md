---
layout: default
title: Generators
permalink: /generators/index.html

partof: getting-started
num: 4
---


A ScalaMeter generator, represented by the `Gen[T]` trait, is a datatype that
provides input data for the test.
More specifically, a generator provides a sequence of warmup test inputs,
a sequence of parameter combinations which produce a specific test input value and
allows producing a test input value from a parameter combination.

The trait `Gen[T]` looks roughly like this:

    trait Gen[T] {
      def warmupset: Iterator[T]
      def dataset: Iterator[Parameters]
      def generate(params: Parameters): T
    }

As mentioned earlier, generators are divided into two main categories -- the
*basic* generators and the *composed* generators.
A number of basic generators are already predefined, and you can obtain
new ones by implementing the above mentioned trait.
However, the preferred way to obtain generators for more complex data types
is from basic ones using `for`-comprehensions.
The generators obtained this way are called the composed generators.


### Basic generators

`Gen.unit(axis: String)`
<br/>
Iterates only a single value -- a `()`.
This generator is useful when we don't need a range of different inputs,
or there is just one meaningful input that is encoded in the microbenchmark.
For example, measuring the time needed to ping some fixed web address fits
into this category.

`Gen.single[T](axis: String)(v: T)`
<br/>
Generates a single, specified value `v`.
Similar to the previous generator, but more general.

`Gen.range(axis: String)(from: Int, upto: Int, hop: Int)`
<br/>
Generates an inclusive range of integer values.
Used to generate collection sizes, problem size input for various algorithms,
or parametrizing algorithms.

`Gen.enumeration[T](axis: String)(xs: T*)`
<br/>
Generates the enumerated values of type `T`.
Useful when parametrizing benchmarked algorithms or methods with
non-numeric data.

`Gen.exponential(axis: String)(from: Int, until: Int, factor: Int)`
<br/>
Generates an inclusive exponential range of integer values.
The starting value is `from`, and each subsequent value is mutliplied
by `factor`, until the value `until` is reached.
Useful as an input when the measurement changes in an interesting way
with a power of some parameter -- for example, the parallelism level
or the data size for a sorting algorithm.

Each basic generator has a **single axis**.
The name of this axis is the name specified when the generator was created.
This same name will be the name of an axis when you generate a chart using
a `ChartReporter`.


### Composed generators

Here is an example of a composed generator:

    for {
      size <- Gen.sizes("size")(5000, 50000, 10000)
      par <- Gen.exponential("par")(1, 8, 2)
    } yield {
      val parrange = (0 until size).par
      parrange.tasksupport = createTaskSupport(par)
      parrange
    }

The `for`-comprehension is desugared into `map` and `flatMap` calls on
generators.
The new generator will go over the combinations of `"size"` and `"par"`
to generate different values.
It will have **two axes**, meaning that every running time
of a benchmark run using it will depend on two input parameters.
<br/>
Such data dependency is best displayed using a 3D chart.

In the next section we take a look at the different [reporters](/scalameter/home/gettingstarted/reporters/).




<div class="imagenoframe">
  <img src="/scalameter/resources/images/logo-yellow-small.png"></img>
</div>



















