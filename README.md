![NATS](images/large-logo.png)

# NATS - Java Examples

A [Java](http://java.com) examples for the [NATS messaging system](https://nats.io).

[![License Apache 2](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Build Badge](https://github.com/nats-io/java-nats-examples/actions/workflows/build.yml/badge.svg)](https://github.com/nats-io/java-nats-examples/actions/workflows/build.yml)


## Introduction

These Java examples of using the NATS Java client [nats.java project](https://github.com/nats-io/nats.java)

In addition to this repository, there are also more examples in the [examples directory](https://github.com/nats-io/nats.java/tree/main/src/examples) of the NATS Java client project.

There is also [NATS by Example](https://natsbyexample.com) - An evolving collection of runnable, cross-client reference examples for NATS.
The [nats-by-example](nats-by-example/README.md) directory contains the same examples. 

### Starters

The starters projects are provided to give you a jump start to adding the NATS Java client to your project.

* starter-maven [pom.xml](starter-maven/pom.xml) for maven users
* starter-gradle-groovy [build.gradle](starter-gradle-groovy/build.gradle) for gradle users who like the Groovy DSL
* starter-gradle-kotlin [build.gradle.kts](starter-gradle-kotlin/build.gradle.kts) for gradle users who like the Kotlin DSL

As a side note for Kotlin users, there is a small example in the [kotlin-nats-examples](https://github.com/nats-io/kotlin-nats-examples) project.

### Hello World

The [Hello World](hello-world/README.md) examples does things like create streams, publish and subscribe.

### Core Request Reply
The [Core Request Reply](core-request-reply-patterns/README.md)
is an example demonstrating that using core nats, there are multiple ways to do request reply.

### JS Multi Tool

The [JS Multi Tool](js-multi-tool/README.md) is a tool to assist exercising and benchmarking of the NATS Java client in your own environment.

### Example Dev Cluster Configs

The files in the [Example Dev Cluster Configs](example-dev-cluster-configs/README.md) directory are templates for building a simple dev cluster.

### OCSP
The [OCSP](ocsp/README.md) project has instructions and examples for building a OCSP aware SSL Context.

### SSL Context Factory 
The [SSL Context Factory](ssl-context-factory/README.md) example demonstrates how to implement the SSLContextFactory,
an alternative way to provide an SSL Context to the Connection Options.

### Auth Callout

The [Auth Callout](auth-callout/README.md) example demonstrates a basic Auth Callout handler

### Recreate Consumer
The [Recreate Consumer](recreate-consumer/README.md) example demonstrates creating a durable consumer that will start where another one left off.

### Robust Push Subscription
The [Robust Push Subscription](robust-push-subscription/README.md) is an application with more robust error handler including recreating the consumer if heartbeat alarm occurs.

### Encoding 

The [Encoding](encoding/README.md) project has examples that encoded/decode message payload.

### Chain Of Command

The [Chain Of Command](chain-of-command/README.md) example shows subscribing with wildcard subjects to form a chain of command.
Both "publish style" and "request style" workflow are demonstrated. 

The "publish style" does not know if messages were received. 
The "request style" knows if the request was received, so it could handle the case when it is not.

### Object Store / File Transfer

The [Manual File Transfer](file-transfer-manual/README.md) project was a proof of concept project was done
as part of the design of Object Store. 

The [File Transfer Object Store](file-transfer-object-store/README.md) project demonstrates 
transferring a file using the completed Object Store API.

### Error and Heartbeat Experiments

The [Error and Heartbeat Experiments](error-and-heartbeat-experiments/README.md) project
are experiments to demonstrate how heartbeats and error listening works.

### Js Over Core
The [Js Over Core](js-over-core/README.md) uses core nats to publish (with Publish Acks!) and subscribe.

### Server Pool
The [Server Pool](server-pool/README.md) is an example how the developer can provide the connection/reconnection info themselves / dynamically.

### Multi Subject Worker
The [Multi Subject Worker](multi-subject-worker/README.md) is an example that processes multiple subjects.

### Original Functional Examples

The [Original Functional Examples](functional-examples/README.md) where the original examples
for the JNATS client and demonstrate one feature at a time.

## License

Unless otherwise noted, the NATS source files are distributed
under the Apache Version 2.0 license found in the LICENSE file.
