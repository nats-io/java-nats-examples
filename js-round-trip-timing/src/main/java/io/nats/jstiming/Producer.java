// Copyright 2021 The NATS Authors
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

package io.nats.jstiming;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamOptions;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.JsTimingMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.nats.client.support.RandomUtils.PRAND;
import static io.nats.jstiming.Utils.generateId;
import static io.nats.jstiming.Utils.sleep;

public class Producer implements AutoCloseable, Runnable {
    public enum Kind {
        SYNC, ASYNC, NO_PUBLISH_ACK;

        public static Kind getInstance(String s) {
            if (ASYNC.name().equalsIgnoreCase(s)) { return ASYNC; }
            if (NO_PUBLISH_ACK.name().equalsIgnoreCase(s)) { return NO_PUBLISH_ACK; }
            return SYNC;
        }
    }

    final String id;
    final Connection conn;
    final JetStream js;
    final String subject;
    final int messageCount;
    final int messageSize;
    final Kind kind;
    final int batchSize;
    final int halfBatchSize;
    final long batchDelay;
    final byte[] messageData;

    public Producer(Kind kind, Connection conn, String subject,
                    int messageCount, int messageSize, int batchSize, long batchDelay) throws IOException
    {
        id = generateId("P");
        this.conn = conn;
        this.subject = subject;
        this.messageCount = messageCount;
        this.messageSize = messageSize;
        this.kind = kind;
        this.batchSize = batchSize;
        halfBatchSize = Math.min(1, batchSize / 2);
        this.batchDelay = Math.min(1, batchDelay);

        messageData = new byte[messageSize];
        PRAND.nextBytes(messageData);

        JetStreamOptions jso = JetStreamOptions.builder().publishNoAck(kind == Kind.NO_PUBLISH_ACK).build();
        js = conn.jetStream(jso);
    }

    @Override
    public void run() {
        try {
            if (kind == Kind.ASYNC) {
                if (batchSize <= 0) {
                    asyncNoBatch();
                }
                else {
                    asyncBatch();
                }
            }
            else if (batchSize <= 0) {
                syncNoBatch();
            }
            else {
                syncBatch();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void syncNoBatch() throws IOException, JetStreamApiException {
        for (int x = 0; x < messageCount; x++) {
            js.publish(new JsTimingMessage(id, x, subject, messageData));
        }
    }

    private void syncBatch() throws IOException, JetStreamApiException {
        js.publish(new JsTimingMessage(id, 0, subject, messageData));
        for (int x = 1; x < messageCount; x++) {
            if (x % batchSize == 0) {
                sleep(batchDelay);
            }
            js.publish(new JsTimingMessage(id, x, subject, messageData));
        }
    }

    private void asyncNoBatch() {
        for (int x = 0; x < messageCount; x++) {
            js.publishAsync(new JsTimingMessage(id, x, subject, messageData));
        }
    }

    private void asyncBatch() throws ExecutionException, InterruptedException {
        List<CompletableFuture<PublishAck>> acks = new ArrayList<>();
        for (int x = 0; x < messageCount; x++) {
            acks.add(js.publishAsync(new JsTimingMessage(id, x, subject, messageData)));
            if (acks.size() >= batchSize) {
                acks = clearAcks(acks);
                sleep(batchDelay);
            }
        }
        while (acks.size() > 0) {
            acks = clearAcks(acks);
        }
    }

    private List<CompletableFuture<PublishAck>> clearAcks(List<CompletableFuture<PublishAck>> acks) throws ExecutionException, InterruptedException {
        while (acks.size() >= halfBatchSize) {
            List<CompletableFuture<PublishAck>> notDone = new ArrayList<>();
            for (CompletableFuture<PublishAck> f : acks) {
                if (f.isDone()) {
                    f.get(); // throws an exception if the future had one
                }
                else {
                    notDone.add(f);
                }
            }
            acks = notDone;
        }
        return acks;
    }

    @Override
    public void close() throws Exception {
    }
}
