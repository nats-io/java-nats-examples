![NATS](images/large-logo.png)

# NATS - Java Examples

A [Java](http://java.com) examples for the [NATS messaging system](https://nats.io).

[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Build Badge](https://github.com/nats-io/java-nats-examples/actions/workflows/build.yml/badge.svg)](https://github.com/nats-io/java-nats-examples/actions/workflows/build.yml)


## Introduction

These Java examples of using the NATS Java client [nats.java project](https://github.com/nats-io/nats.java)

In addition to this repository, there are also more examples in the [examples folder](https://github.com/nats-io/nats.java/tree/main/src/examples) of the NATS Java client porject.

### Starters

The starters projects are provided to give you a jump start to adding the NATS Java client to your project.

* starter-gradle-groovy for gradle users who like the Groovy DSL
* starter-gradle-kotlin for gradle users who like the Kotlin DSL
* starter-maven for maven users

### Hello World

Some examples that create streams, publish and subscribe.

### Functional Examples

The functional examples demonstrate one feature at a time.

### Chain Of Command

The chain of command example shows subscribing with wildcard subjects to form a chain of command.
Both "publish style" and "request style" workflow are demonstrated. 

The "publish style" does not know if messages were received. 
The "request style" knows if the request was received, so it could handle the case when it is not.

### Kotlin

As a side note for Kotlin users, there is a small example in the [kotlin-nats-examples](https://github.com/nats-io/kotlin-nats-examples) project.

## License

Unless otherwise noted, the NATS source files are distributed
under the Apache Version 2.0 license found in the LICENSE file.
