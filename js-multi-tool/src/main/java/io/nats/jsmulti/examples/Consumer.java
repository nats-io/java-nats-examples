package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;

import java.text.NumberFormat;

/**
 * Example class running a consumer (publisher)
 *
 * Various ways to run the code
 * 1. Through an ide...
 * 2. Maven: mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.examples.Consumer -Dexec.args="[args]"
 *    ! You can increase memory for maven via environment variable, i.e. set MAVEN_OPTS=-Xmx6g
 * 3. Gradle: gradle clean consumer --args="[args]"
 *    ! You can increase memory for the gradle task by changing the `jvmArgs` value for the `consumer` task in build.gradle.
 * 4. Command Line: java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.examples.Consumer [args]
 */
public class Consumer {

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    public static void main(String[] args) throws Exception {
        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("\n\nfree memory: " + format.format(freeMemory / 1024) + "\n");
        sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
        sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n\n");
        System.out.println(sb);
        if (true) return;
        
        // You could code this to use args to create the Arguments
        Arguments a = Arguments.instance()
            .server(SERVER)
            .subject(SUBJECT)
            .action(Action.SUB_PULL_QUEUE)  // could be Action.SUB_PULL for example
            .messageCount(50_000)           // default is 100_000. Consumer needs this to know when to stop.
            // .ackPolicy(AckPolicy.None)   // default is AckPolicy.Explicit which is the only policy allowed for PULL at the moment
            // .ackAllFrequency(20)         // for AckPolicy.All how many message to wait before acking, DEFAULT IS 1
            .batchSize(20)                  // default is 10 only used with pull subs
            .threads(3)                     // default is 1
            .individualConnection()         // versus .sharedConnection()
            // .reportFrequency(500)        // default is 10% of message count
            ;

        a.printCommandLine();

        Context ctx = new Context(a);

        // -----------------------------------------------------
        // Uncomment for latency runs. The stream needs to exist
        // before the consumers start.
        // -----------------------------------------------------
        // StreamUtils.setupStream(STREAM, ctx);

        JsMulti.run(ctx, true, true);
    }
}
