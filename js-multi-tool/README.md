![NATS](../images/large-logo.png)

# JetStream Multi Tool

## About this Tooling

This is a tool to assist exercising and benchmarking of the NATS Java client in your own environment.

Administration and server related benchmarking of NATS should prefer the [NATS CLI](https://docs.nats.io/nats-tools/natscli) tooling, especially in production environments.

### Running from the Command Line

#### Maven exec:java
You can use the maven exec plugin...

```shell
> mvn clean compile exec:java -Dexec.mainClass="io.nats.jsmulti.JsMulti" -Dexec.args="-a PubSync -s nats://localhost:4222 -u sub -m 10_000"
> mvn clean compile exec:java -Dexec.mainClass="io.nats.jsmulti.examples.Producer" -Dexec.args="[arguments]"
> mvn clean compile exec:java -Dexec.mainClass="io.nats.jsmulti.examples.Consumer" -Dexec.args="[arguments]"
> mvn clean compile exec:java -Dexec.mainClass="io.nats.jsmulti.examples.Rtt" -Dexec.args="[arguments]"
```

You can increase memory for maven via environment variable, i.e.

```
set MAVEN_OPTS=-Xmx6g
```

#### Gradle Task
There are tasks available for js

```shell
> gradle clean jsMulti --args="-a PubSync -s nats://localhost:4222 -u sub -m 10_000"
> gradle clean producer --args="[arguments]"
> gradle clean consumer --args="[arguments]"
> gradle clean rtt --args="[arguments]"
```

You can increase memory for the gradle task by changing the `jvmArgs` value for the correct task in build.gradle.

#### Direct Java

Making some assumptions that you have a copy of the Java Nats Client (jnats) in your maven repo, since you needed it to build this project, 
you can simply run java with the correct classpath, which includes the location of classes folder as compiled and the location of the jar file.

At a minimum, you will need to provide `<my-code-path>` and `<my-repo-path>`

```shell
> java -cp <my-code-path>/java-nats-examples/js-multi-tool/target/classes;<my-repo-path>/io/nats/jnats/2.16.14/jnats-2.16.14.jar io.nats.jsmulti.JsMulti -a PubSync -s nats://localhost:4222 -u sub -m 10_000
```

You could also use a gradle task to build an uber jar. This jar will contain all necessary classes and the jnats library.

```shell
gradle uberJar
```

At that point you can run directly the programs you can run with the gradle tasks 

```shell
> java -cp <my-code-path>/java-nats-examples/js-multi-tool/build/libs/js-multi-tool-1.5.3-uber.jar io.nats.jsmulti.JsMulti [arguments] 
> java -cp <my-code-path>/java-nats-examples/js-multi-tool/build/libs/js-multi-tool-1.5.3-uber.jar io.nats.jsmulti.examples.Producer [arguments] 
> java -cp <my-code-path>/java-nats-examples/js-multi-tool/build/libs/js-multi-tool-1.5.3-uber.jar io.nats.jsmulti.examples.Consumer [arguments] 
> java -cp <my-code-path>/java-nats-examples/js-multi-tool/build/libs/js-multi-tool-1.5.3-uber.jar io.nats.jsmulti.examples.Rtt [arguments] 
```

### Running from an IDE

The JsMulti program has a `main` method so it's easy enough to run from any ide.
You can also call it from another program using the `public static void run(Arguments a)` method.
We have provided an `Arguments` class to help build configurations.
You could also use your IDE and build a runtime configuration that passes command line arguments.

The examples below show both command line and builder examples.

### Number Arguments

Number arguments are all integers. When providing numbers on the command line, you can use underscore `_`, comma `,` or period `.`
to make it more readable. So these are all valid for 1 million `1000000`, `1,000,000`, `1.000.000` or `1_000_000`

### Action Argument

`-a` The action to execute. Always required. One of the following (case ignored)

* `RTT` Round Trip Test
* `PubSync` publish synchronously
* `PubAsync` publish asynchronously
* `PubCore` publish synchronously using the core api
* `SubCore` core subscribe read messages (synchronously)
* `SubPush` push subscribe read messages (synchronously)
* `SubQueue` push subscribe read messages with queue (synchronously)
* `SubPull` pull subscribe fetch messages (different durable if threaded)
* `SubPullQueue` pull subscribe fetch messages (all with same durable)
* `SubPullRead` pull subscribe read messages (different durable if threaded)
* `SubPullReadQueue` pull subscribe read messages (all with same durable)

#### Using the builder

The builder was created to give another way to build configurations if you typically run from an IDE. 
You could use the builder and modify the `JsMulti` class or just create your own classes that make it easier to run from the command line.

The actions all have smart builder creation methods...

```java
Context ctx = Arguments.rtt() ... .build();
Context ctx = Arguments.pubSync("subject-name") ... .build();
Context ctx = Arguments.pubAsync("subject-name") ... .build();
Context ctx = Arguments.pubCore("subject-name") ... .build();
Context ctx = Arguments.subCore("subject-name") ... .build();
Context ctx = Arguments.subPush("subject-name") ... .build();
Context ctx = Arguments.subQueue("subject-name") ... .build();
Context ctx = Arguments.subPull("subject-name") ... .build();
Context ctx = Arguments.subPullRead("subject-name") ... .build();
Context ctx = Arguments.subPullReadQueue("subject-name") ... .build();
```

You could build your own custom class to run from the command line:

```java
public class MyMulti {
    public static void main(String[] args) throws Exception {
        Context ctx = Arguments.pubSync("subject-name") ... .build();
        JsMulti.run(new Arguments(args));
    }
}
```

Here are some variations of `JsMulti.run(...)` that are available. 
```shell
public static List<Stats> run(String[] args) throws Exception
public static List<Stats> run(String[] args, boolean printArgs, boolean reportWhenDone) throws Exception
public static List<Stats> run(Arguments args) throws Exception
public static List<Stats> run(Arguments args, boolean printArgs, boolean reportWhenDone) throws Exception
```

### Server Argument

`-s` The url of the server. if not provided, will default to `nats://localhost:4222`

_Command Line_

```shell
... JsMulti ... -s nats://myhost:4444 ...
```

_Builder_

```java
Context ctx = Arguments ... .server("nats://myhost:4444") ...
```

### Options Factory Argument

`-of` Options factory class name. Takes precedence over `-s`. Must have a default constructor so it can be instantiated by reflection.

By providing your own options factory you are able to connect to a server however you need to.
For instance, you can provide your own authentication or could override JsMulti's default No-Op error listener.
Make sure this value is a fully qualified class name with package, i.e. `com.myco.MyOptionsFactory`

```java
public class MyOptionsFactory implements OptionsFactory {
    @Override
    public Options getOptions() throws Exception {
        return ... do work here
    }
}
```

### Report Frequency

`-rf` report frequency (number) how often to print progress, defaults to 1000 messages. <= 0 for no reporting. 
Reporting time is excluded from timings.

_Command Line_

```shell
... JsMulti ... -rf 5000 ...
... JsMulti ... -rf -1 ...
```

_Builder_

```java
Context ctx = Arguments ... .reportFrequency(5000) ...
Context ctx = Arguments ... .noReporting() ...
```

## Publishing and Subscribing

Publishing and subscribing have some options in common.

### Required Arguments

`-u` subject (string), required for publishing or subscribing

### Optional Arguments

`-m` message count (number) for publishing or subscribing, defaults to 100,000

`-d` threads (number) for publishing or subscribing, defaults to 1

`-n` connection strategy (shared|individual) when threading, whether to share
* `shared` When running with more than 1 thread, only connect to the server once and share the connection among all threads. This is the default.
* `individual` When running with more than 1 thread, each thread will make its own connection to the server.

`-j` jitter (number) between publishes or subscribe message retrieval of random 
number from 0 to j-1, in milliseconds, defaults to 0 (no jitter), maximum 10_000
time spent in jitter is excluded from timings

```shell
... JsMulti ... -u subject-name -m 1_000_000 -d 4 -n shared -j 1500 
... JsMulti ... -u subject-name -d 4 -j 1500 
... JsMulti ... -u subject-name -m 2_000_000 -d 4 -n individual -j 1500 
```

### Publish Only Optional Arguments

`-ps` payload size (number) for publishing, defaults to 128, maximum 1048576"

`-rs` round size (number) for pubAsync, default to 100, maximum 1000.
Publishing asynchronously uses the "sawtooth" pattern. This means we publish a round of messages, collecting all the futures that receive the PublishAck
until we reach the round size. At that point we process all the futures we have collected, then we start over until we have published all the messages. 

`-lf` latency flag tells the publish to add information so the subscribe half of the run can calculate latency 

```shell
... JsMulti ... -ps 512 -rs 50 
```

> In real applications there are other algorithms one might use. For instance, you could optionally set up a separate thread to process the Publish Ack. 
This would require some concurrent mechanism like the `java.util.concurrent.LinkedBlockingQueue`
Publishing would run on one thread and place the futures in the queue. The second thread would
be pulling from the queue.

### Subscribe Push or Pull Optional Arguments

`-kp` ack policy (explicit|none|all) for subscriptions. Ack Policy must be `explicit` on pull subscriptions.

* `explicit` Explicit Ack Policy. Acknowledge each message received. This is the default.
* `none` None Ack Policy. Configures the consumer to not have to ack messages.
* `all` All Ack Policy. Configures the consumer to ack with one message for all the previous messages.

`-kf` ack frequency (number), applies to Ack Policy all, ack after kf messages, defaults to 1, maximum 256. 
For Ack Policy `explicit`, all messages will be acked after kf number of messages are received. 
For Ack Policy `all`, the last message will be acked once kf number of messages are received.
Does not apply to Ack Policy `none` 

### Subscribe Pull Optional Arguments

`-bs` batch size (number) for subPull*, defaults to 10, maximum 256

```shell
... JsMulti ... -kp explicit -kf 100 -bs 50 
... JsMulti ... -kp explicit -kf 100 -bs 50 
... JsMulti ... -kp all -kf 100 
... JsMulti ... -kp none 
```

## Publish Examples

#### Synchronous processing of the acknowledgement

* publish to `subject-name`
* 1 thread (default)
* 1 million messages (default)
* payload size of 128 bytes (default)
* shared connection (default)

_Command Line_

```shell
... JsMulti -a PubSync -u subject-name
```

_Builder_

```java
Context ctx = Arguments.pubSync("subject-name").build();
```

#### Synchronous processing of the acknowledgement

* publish to `subject-name`
* 5 million messages
* 5 threads
* payload size of 256 bytes
* jitter of 1 second (1000 milliseconds)

_Command Line_

```shell
... JsMulti -a PubSync -u subject-name -d 5 -m 5_000_000 -p 256 -j 1000
```

_Builder_

```java
Context ctx = Arguments.pubSync("subject-name")
    .messageCount(5_000_000)
    .threads(5)
    .jitter(1000)
    .payloadSize(256)
    .build();
```

#### Asynchronous processing of the acknowledgement

* publish to `subject-name`
* 2 million messages
* 2 threads
* payload size of 512 bytes
* shared/individual connections to the server

_Command Line_

```shell
... JsMulti -a PubAsync -u subject-name -m 2_000_000 -d 2 -p 512 -n shared
... JsMulti -a PubAsync -u subject-name -m 2_000_000 -d 2 -p 512 -n individual
```

_Builder_

```java
Context ctx = Arguments.pubSync("subject-name")
    .messageCount(2_000_000)
    .threads(2)
    .payloadSize(512)
    .sharedConnection()
    .build();

Context ctx = Arguments.pubSync("subject-name")
    .messageCount(2_000_000)
    .threads(2)
    .payloadSize(512)
    .individualConnection()
    .build();
```

#### Synchronous core style publishing

* publish to `subject-name`
* 2 million messages
* 2 threads
* payload size of 512 bytes
* individual connections to the server

_Command Line_

```shell
... JsMulti -a PubCore -u subject-name -m 2_000_000 -d 2 -p 512 -n individual
```

_Builder_

```java
Context ctx = Arguments.pubCore("subject-name")
    .messageCount(2_000_000)
    .threads(2)
    .payloadSize(512)
    .individual()    
    .build();
```

## Subscribe Examples

#### Push subscribe

* from 'subject-name'
* 1 thread (default)
* 1 million messages (default)
* explicit ack (default)

_Command Line_

```shell
... JsMulti -a SubPush -u subject-name
```

_Builder_

```java
Context ctx = Arguments.subPush("subject-name").build();
```

#### Push subscribe with a Queue

> IMPORTANT subscribing (push or pull) with a queue requires multiple threads

* from 'subject-name'
* 2 threads
* 1 million messages (default)
* ack none

_Command Line_

```shell
... JsMulti -a SubQueue -u subject-name -d 2 -kp none
```

_Builder_

```java
Context ctx = Arguments.subQueue("subject-name")
    .threads(2)
    .ackNone()
    .build();
```

#### Pull subscribe

#### Pull subscribe works like a queue

```shell
... JsMulti -u sub -a SubPullQueue -d 3 -n individual -rf 10000 -j 0 -m 100000
```

### Latency Examples

In order to test latency you must start a consumer run first, then start the publish with the latency flag.
This means that 2 instances run at the same time.

Start the consumer first...

```shell
... JsMulti -u sub -a SubPullQueue -d 3 -n individual -rf 10000 -j 0 -m 100000
```

The start the producer...

```shell
... JsMulti -s nats://localhost:4222 -u sub -a PubSync -lf -d 3 -n individual -rf 10000 -j 0 -m 100000
```

## Producer / Consumer Examples

Look at the java programs `src/main/java/io/nats/jsmulti/examples`

`Producer.java` demonstrates how to build a simple main to produce messages (publish).
`Consumer.java` demonstrates how to build a simple main to consume messages (subscribe).

These are provided as customization examples. They also have instructions on how to run them.

## Usage

```
Actions Arguments
-----------------
-a action (string), required, one of 
   RTT          - round trip timing
   pubSync      - publish synchronously
   pubAsync     - publish asynchronously
   pubCore      - core publish (synchronously) to subject
   subPush      - push subscribe read messages (synchronously)
   subQueue     - push subscribe read messages with queue (synchronously).
                    Requires 2 or more threads
   subPull      - pull subscribe fetch messages
   subPullQueue - pull subscribe fetch messages, queue (using common durable)
   subPullRead  - pull subscribe read messages
   subPullReadQueue - pull subscribe read messages, queue (using common durable)
                    Requires 2 or more threads

Server Arguments
----------------
-s server url (string), optional, defaults to nats://localhost:4222
-cf credentials file (string), optional
-ctms connection timeout millis, optional, defaults to 5000
-rwms reconnect wait millis, optional, defaults to 1000
-of options factory class name. Class with no op constructor that implements OptionsFactory
    If supplied, used instead of -s, -cf, -ctms and -rwms.
-rf report frequency (number) how often to print progress, defaults to 10% of message count.
    <= 0 for no reporting. Reporting time is excluded from timings

Latency Arguments
-----------------
-lf latency flag. Needed when publishing to test latency. See examples.
-lcsv latency-csv-file-spec

General Arguments
-----------------
-u subject (string), required for publishing or subscribing
-m message count (number) required > 1 for publishing or subscribing, defaults to 100_000
-d threads (number) for publishing or subscribing, defaults to 1
-n connection strategy (shared|individual) when threading, whether to share
     the connection, defaults to shared
-j jitter (number) between publishes or subscribe message retrieval of random
     number from 0 to j-1, in milliseconds, defaults to 0 (no jitter), maximum 10_000
     time spent in jitter is excluded from timings
-ps payload size (number) for publishing, defaults to 128, maximum 1048576
-rs round size (number) for pubAsync, default to 100, maximum 1000
-kp ack policy (explicit|none|all) for subscriptions, defaults to explicit
-kf ack all frequency (number), applies to ack policy all, ack after kf messages
      defaults to 1, maximum 100
-bs batch size (number) for subPull*, defaults to 10, maximum 200
-rqwms request wait millis, time to wait for a JetStream request to complete, default is 1000 milliseconds
-rtoms read timeout wait millis, time to wait for a individual synchronous message read (next or fetch), default is 1000 milliseconds
-rmxwms rmxwms read max wait millis, when reading messages in a loop, stop if there are no messages in this time, default is 10 seconds (10000ms) 

Notes
-----
All text constants are case insensitive, i.e.
  action, connection strategy, ack policy, pull type
Input numbers can be formatted for easier viewing. For instance, ten thousand
  can be any of these: 10000 10,000 10.000 10_000
  and can end with factors k, m, g meaning x 1000, x 1_000_000, x 1_000_000_000
  or ki, mi, gi meaning x 1024, x 1024 * 1024, x 1024 * 1024 * 1024
Use tls:// or opentls:// in the server url to require tls, via the Default SSLContext
```