package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;

public class Producer {

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    static final boolean latencyRun = true;

    public static void main(String[] args) throws Exception {
        Arguments a = Arguments.instance()
            .server(SERVER)
            .subject(SUBJECT)
            .action(Action.PUB_SYNC) // or Action.PUB_ASYNC or Action.PUB_CORE for example
            .latencyFlag(latencyRun)
            .threads(3)
            .individualConnection() // versus shared
            .reportFrequency(10000) // report every 10K
            .jitter(0) // > 0 means use jitter
            .messageCount(100_000);

        a.printCommandLine();

        Context ctx = new Context(a);

        // latency run, the consumer code sets up the stream
        if (!latencyRun) {
            StreamUtils.setupStream(STREAM, SUBJECT, ctx);
        }

        JsMulti.run(ctx);
    }
}
