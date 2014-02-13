---
layout: default
title: SBT integration
permalink: /sbt/index.html

partof: getting-started
num: 8
---


Running ScalaMeter tests from SBT is easy -- just follow these simple steps.

* Add a ScalaMeter dependency to your project in SBT.
Open `build.sbt` and add the following line:

      resolvers += "Sonatype OSS Snapshots" at
        "https://oss.sonatype.org/content/repositories/snapshots"

      libraryDependencies += "com.github.axel22" %% "scalameter" % "[version]"

Where `[version]` should be the desired ScalaMeter release.
You can find the exact maven dependencies for each ScalaMeter release
in the [download section](/scalameter/home/download).
Alternatively, you can include ScalaMeter library as a manual dependency
by downloading it and placing it into the `lib` directory of your project.
See the [download section](/scalameter/home/download) for instructions on how to download
the JAR file manually.

* SBT needs to know that there is a new test interface in your project.
Open `build.sbt` again and add the following lines:

        testFrameworks += new TestFramework(
          "org.scalameter.ScalaMeterFramework")
      
        logBuffered := false

And voila -- you should be able to run ScalaMeter from the SBT shell now.

To run all the tests:

    > test

To run a single test:

    > test-only mypackage.MyScalaMeterTestName

To run tests with arguments:

    > test-only mypackage.MyScalaMeterTestName -- <arguments>

## SBT/command line test arguments

Below is a list of useful arguments.
These can be used both from SBT and when running ScalaMeter from the command line.

Since ScalaMeter 0.5, running a performance test by default produces verbose output about the status of the test.
ScalaMeter shows information about running your benchmarks, warmup times, measuring time, etc.
The `-silent` flag disables this verbose output:

    > test-only mypackage.MyScalaMeterTestName -- -silent

In versions prior to ScalaMeter 0.5 the verbose output was off by default.
The `-verbose` flag enabled verbose output.

    > test-only mypackage.MyScalaMeterTestName -- -verbose

The `-CresultDir` option changes the directory the results are generated into:

    > test-only mypackage.MyScalaMeterTestName -- -CresultDir tmp2

Starting from ScalaMeter 0.4 you can selectively run only certain tests from your test suite.

The `-CscopeFilter <test-name-regex>` option runs only the benchmarks whose name starts with `<test-name-regex>`.
For example:

    > test-only mypackage.MyScalaMeterTestName -- -CscopeFilter 'Seq'

    > test-only mypackage.MyScalaMeterTestName -- -CscopeFilter 'Seq\.foreach'

    > test-only mypackage.MyScalaMeterTestName -- -CscopeFilter 'Seq\.foreach.*'



<div class="imagenoframe">
  <img src="/scalameter/resources/images/logo-yellow-small.png"></img>
</div>




