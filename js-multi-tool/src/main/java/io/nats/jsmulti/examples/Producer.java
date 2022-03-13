package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.ArgumentBuilder;

public class Producer {

    static final String STREAM = "strm";
    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    static final boolean latencyRun = true;

    public static void main(String[] args) throws Exception {
        // latency run, the consumer code sets up the stream
        if (!latencyRun) {
            StreamUtils.setupStream(STREAM, SUBJECT, SERVER);
            return;
        }

        JsMulti.run(
            ArgumentBuilder.builder()
                .server(SERVER)
                .subject(SUBJECT)
                .action(Action.PUB_SYNC)
//                .action(Action.PUB_ASYNC)
//                .action(Action.PUB_CORE)
                .latencyFlag(true)
                .threads(3)
                .individualConnection() // versus shared
                .reportFrequency(10000) // report every 10K
                .jitter(0) // > 0 means use jitter
                .messageCount(100_000)
                .print()
                .build());
    }
}
