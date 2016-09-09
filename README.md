
CI service | Status | Description
-----------|--------|------------
Travis | [![Build Status](https://travis-ci.org/scalameter/scalameter.png?branch=master)](https://travis-ci.org/scalameter/scalameter) | Linux container tests
Drone | [![Build Status](http://ci.storm-enroute.com:443/api/badges/scalameter/scalameter/status.svg)](http://ci.storm-enroute.com:443/scalameter/scalameter) | Linux container tests
AppVeyor | [![Build status](https://ci.appveyor.com/api/projects/status/08hfljfae46wj9hc/branch/master?svg=true)](https://ci.appveyor.com/project/storm-enroute-bot/scalameter/branch/master) | Windows tests
[scalameter-examples](https://github.com/scalameter/scalameter-examples) | [![Build Status](https://travis-ci.org/scalameter/scalameter-examples.svg?branch=master)](https://travis-ci.org/scalameter/scalameter-examples) | ScalaMeter benchmark example projects
Maven | [![Maven Artifact](https://img.shields.io/maven-central/v/com.storm-enroute/scalameter_2.11.svg)](http://mvnrepository.com/artifact/com.storm-enroute/scalameter_2.11) | ScalaMeter artifact on Maven

ScalaMeter
==========

Microbenchmarking and performance regression testing framework for the JVM platform.
ScalaMeter can automatically measure and collect various metrics of your program,
and then produce nice reports, or store your data.
For example, it measures:

- memory footprint
- running time
- GC cycles
- invocations of specific methods
- boxing of primitive values

Learn more at the official ScalaMeter website:
[scalameter.github.io](http://scalameter.github.io)
