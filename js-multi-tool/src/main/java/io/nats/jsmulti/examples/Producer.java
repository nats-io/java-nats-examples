package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;

public class Producer {

    /*
        gradle clean producer --args="[args]"
        mvn clean compile exec:java -Dexec.mainClass=io.nats.jsmulti.examples.Producer -Dexec.args="[args]"
        java -cp <path-to-js-multi-files-or-jar>:<path-to-jnats-jar> io.nats.jsmulti.examples.Producer [args]
     */

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    static final boolean LATENCY_RUN = false;

    public static void main(String[] args) throws Exception {
        Arguments a = Arguments.instance()
            .server(SERVER)
            .subject(SUBJECT)
            .action(Action.PUB_SYNC)    // or Action.PUB_ASYNC or Action.PUB_CORE for example
            .latencyFlag(LATENCY_RUN)   // tells the code to add latency info to the header
            .messageCount(50_000)       // default is 100_000
            .payloadSize(256)           // default is 128
            .roundSize(50)              // how often to check Async Publish Acks, default is 100
            .threads(3)                 // default is 1
            .individualConnection()     // versus .sharedConnection()
            .reportFrequency(5000)      // default is 10_000
            ;

        a.printCommandLine();

        Context ctx = new Context(a);

        // latency run, the consumer code sets up the stream
        if (!LATENCY_RUN) {
            StreamUtils.setupStream(STREAM, ctx);
        }

        JsMulti.run(ctx);
    }
}
