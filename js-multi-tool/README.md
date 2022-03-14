![NATS](../images/large-logo.png)

# JetStream Multi Tool

## About this Tooling

This is a tool to provide complete exercising and benchmarking specific to the NATS Java client with the intent of establishing a baseline for client performance and behavior.

Administration and server related benchmarking of NATS should prefer the [NATS CLI](https://docs.nats.io/nats-tools/natscli) tooling, especially in production environments.

### Running from the Command Line

#### Maven exec
You can use the maven exec plugin...

```shell
mvn exec:java -Dexec.mainClass="io.nats.jsmulti.JsMulti" -Dexec.args="-a PubSync -s nats://localhost:4222 -u sub -m 10_000"
```

#### Direct Java

Making some assumptions that you have a copy of the Java Nats Client (jnats) in your maven repo, since you needed it to build this project, 
you can simply run java with the correct classpath, which includes the location of classes folder as compiled and the location of the jar file.

At a minimum, you will need to provide `<my-code-path>` and `<my-repo-path>`

```shell
java -cp <my-code-path>\java-nats-examples\js-multi-tool\target\classes;<my-repo-path>\io\nats\jnats\2.14.0\jnats-2.14.0.jar io.nats.jsmulti.JsMulti -a PubSync -s nats://localhost:4222 -u sub -m 10_000
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

* `PubSync` publish synchronously
* `PubAsync` publish asynchronously
* `PubCore` publish synchronously using the core api
* `SubPush` push subscribe read messages (synchronously)
* `SubQueue` push subscribe read messages with queue (synchronously)
* `SubPull` pull subscribe read messages (different durable if threaded)
* `SubPullQueue` pull subscribe read messages (all with same durable)

#### Using the builder

The builder was created to give another way to build configurations if you typically run from an IDE. 
You could use the builder and modify the `JsMulti` class or just create your own classes that make it easier to run from the command line.

The actions all have smart builder creation methods...

```java
Context ctx = Arguments.pubSync("subject-name") ... .build();
Context ctx = Arguments.pubAsync("subject-name") ... .build();
Context ctx = Arguments.pubCore("subject-name") ... .build();
Context ctx = Arguments.subPush("subject-name") ... .build();
Context ctx = Arguments.subQueue("subject-name") ... .build();
Context ctx = Arguments.subPull("subject-name") ... .build();
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

`-pt` pull type (fetch|iterate) defaults to iterate

`-bs` batch size (number) for subPull/subPullQueue, defaults to 10, maximum 256

```shell
... JsMulti ... -pt fetch -kp explicit -kf 100 -bs 50 
... JsMulti ... -pt iterate -kp explicit -kf 100 -bs 50 
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
 
