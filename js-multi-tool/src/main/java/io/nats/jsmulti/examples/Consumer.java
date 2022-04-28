package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;

public class Consumer {
    /*
        gradle clean consumer --args="[args]"
        mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.examples.Consumer -Dexec.args="[args]"
        java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.examples.Consumer [args]
     */

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    public static void main(String[] args) throws Exception {
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
            .reportFrequency(5000)          // default is 10_000
            ;

        a.printCommandLine();

        Context ctx = new Context(a);

        // Consumer sets up the stream for latency (non-normal) runs.
        // Uncomment for latency runs
        // StreamUtils.setupStream(STREAM, ctx);

        JsMulti.run(ctx, true, true);
    }
}
