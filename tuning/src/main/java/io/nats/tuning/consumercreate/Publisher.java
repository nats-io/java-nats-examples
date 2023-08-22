// Copyright 2023 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
