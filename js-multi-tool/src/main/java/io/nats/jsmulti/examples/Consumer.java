package io.nats.jsmulti.examples;

import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.ArgumentBuilder;

public class Consumer {

    static final String SUBJECT = "sub";
    static final String SERVER = "nats://localhost:4222";

    public static void main(String[] args) throws Exception {
        JsMulti.run(
            ArgumentBuilder.builder()
                .server(SERVER)
                .subject(SUBJECT)
                .action(Action.SUB_PULL_QUEUE)
                .threads(3)
//                .action(Action.SUB_PULL)
                .pullType(ArgumentBuilder.FETCH)
                .individualConnection() // versus shared
                .reportFrequency(10000) // report every 10K
                .jitter(0) // > 0 means use jitter
                .messageCount(1_000_000)
                .print()
                .build());
    }
}
