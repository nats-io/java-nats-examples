package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;

public class Consumer {

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    static final boolean latencyRun = true;

    public static void main(String[] args) throws Exception {
        // latency run, the consumer code sets up the stream
        if (latencyRun) {
            StreamUtils.setupStream(STREAM, SUBJECT, SERVER);
        }

        JsMulti.run(
            Arguments.instance()
                .server(SERVER)
                .subject(SUBJECT)
                .action(Action.SUB_PULL_QUEUE)
//                .action(Action.SUB_PULL)
                .threads(3)
                .individualConnection() // versus shared
                .reportFrequency(10000) // report every 10K
                .jitter(0) // > 0 means use jitter
                .messageCount(100_000)
                .printCommandLine());
    }
}
