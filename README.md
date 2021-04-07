![](https://raw.githubusercontent.com/nats-io/nats-site/master/src/img/large-logo.png)

# NATS - Java Examples

A [Java](http://java.com) examples for the [NATS messaging system](https://nats.io).

[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://travis-ci.org/nats-io/java-nats-examples.svg?branch=master)](http://travis-ci.org/nats-io/java-nats-examples)


## Introduction

These are the Java examples used on [nats.io](https://nats.io).

### Functional Examples

The functional examples demonstrate one feature at a time. 

There are also more functional feature examples in the [nats.java project](https://github.com/nats-io/nats.java) in the
[examples project](https://github.com/nats-io/nats.java/tree/main/src/examples).

### Chain Of Command

The chain of command example shows subscribing with wildcard subjects to form a chain of command.
Both publish style and request style workflow are demonstrated. 

The publish style does not know if it's publish was received. 
The request style knows if the request was received, so it could handle the case when it is not.
