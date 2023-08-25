// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

package io.nats.tuning.consumercreate;

import io.nats.client.JetStream;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Publisher extends Thread {
    private final Settings settings;
    private final JetStream js;
    private final int id;
    private final String payloadTemplate;

    public AtomicBoolean go = new AtomicBoolean(true);
    public AtomicLong totalTime = new AtomicLong();
    public AtomicLong count = new AtomicLong();

    public Publisher(Settings settings, JetStream js, int id) {
        this.js = js;
        this.id = id;
        StringBuilder sb = new StringBuilder();
        this.settings = settings;
        for (int x = 0; x < settings.payloadSize; x++) {
            sb.append(" ");
        }
        payloadTemplate = sb.toString();
    }

    @Override
    public void run() {
        while (go.get()) {
            try {
                String subject = settings.subjectGenerator.getSubject(id);
                byte[] payload = (id + "" + (count.incrementAndGet()) + payloadTemplate).substring(0, settings.payloadSize).getBytes();
                long start = System.nanoTime();
                js.publish(subject, payload);
                totalTime.set(totalTime.get() + System.nanoTime() - start);
            }
            catch (Exception e) {
                System.err.println("PUB [" + id + "] Exception: " + e);
            }
        }
    }
}
