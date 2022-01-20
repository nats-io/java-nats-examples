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

package io.nats.example;

import io.nats.client.*;
import io.nats.client.api.PublishAck;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import io.nats.client.support.DateTimeUtils;
import io.nats.client.support.RandomUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.nats.example.Result.*;
import static java.lang.System.lineSeparator;

/*
Timing Example
 */
public class JsTimingExample
{
    public static final String URL = "nats://localhost:4222";
    public static final StorageType STORAGE_TYPE = StorageType.Memory;

    public static final String OUTPUT_FILE = "C:\\temp\\JsTiming.csv";
    public static final String STREAM = "JsTimingStream";
    public static final String SUBJECT = "JsTimingSubject";

    public static final int[] PAYLOAD_SIZES = new int[] {
        128, 1024, 16 * 1024, 64 * 1024
    } ;

    public static final int[] MESSAGE_COUNTS = new int[] {
        1000, 10_000, 100_000
    } ;

    public static void main(String[] args )
    {
        try (FileOutputStream out = new FileOutputStream(OUTPUT_FILE);
             Connection nc = Nats.connect(URL))
        {
            String text = "Server Version," + nc.getServerInfo().getVersion() + lineSeparator() + "Client Version," + Nats.CLIENT_VERSION + lineSeparator();
            System.out.print(text);
            out.write(text.getBytes());

            Stats allStats = new Stats(-1, -1);
            for (int messageCount : MESSAGE_COUNTS) {
                for (int payloadSize : PAYLOAD_SIZES) {
                    Stats.writeHeader(System.out, messageCount, payloadSize);;
                    int consumerReportInterval = messageCount / 10;
                    createStream(nc);

                    JetStream js = nc.jetStream();

                    Consumer c = new Consumer(nc, js, messageCount, consumerReportInterval);
                    List<CompletableFuture<PublishAck>> acks = publish(js, messageCount, payloadSize);

                    // TODO maybe handle pub acks make sure all messages were published
                    // although the latch will timeout if all messages don't get published

                    c.doneLatch.await(10, TimeUnit.MINUTES);
                    c.close();

                    Stats s = new Stats(payloadSize, messageCount);
                    for (Result r : c.results) {
                        s.update(r);
                    }
                    allStats.update(s);
                    s.writeTo(out, System.out);
                }
            }
            allStats.writeTo(out, System.out);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<CompletableFuture<PublishAck>> publish(JetStream js, int messageCount, int payloadSize) {
        byte[] payloadBuffer = new byte[payloadSize];
        RandomUtils.PRAND.nextBytes(payloadBuffer);

        List<CompletableFuture<PublishAck>> acks = new ArrayList<>();
        for (int x = 1; x <= messageCount; x++) {
            acks.add(
                js.publishAsync(
                    NatsMessage.builder()
                        .subject(SUBJECT)
                        .data(payloadBuffer)
                        .headers(new Headers()
                            .put(HDR_MSG_ID, "M-" + x)
                            .put(HDR_START_TIME_NANOS, "" + System.nanoTime())
                            .put(HDR_START_TIME_ZDT, "" + DateTimeUtils.toRfc3339(ZonedDateTime.now()))
                        )
                        .build())
            );
        }

        return acks;
    }

    public static class Consumer implements AutoCloseable {
        public List<Result> results;
        Dispatcher d;
        JetStreamSubscription sub;
        AtomicInteger count;
        public final CountDownLatch doneLatch;

        public Consumer(Connection nc, JetStream js, int messageCount, final int consumerReportInterval) throws IOException, JetStreamApiException {
            results = new ArrayList<>();
            d = nc.createDispatcher();
            count = new AtomicInteger();
            doneLatch = new CountDownLatch(messageCount);

            MessageHandler mh = msg -> {
                try {
                    long receivedTimeNanos = System.nanoTime();
                    ZonedDateTime receivedDateTime = ZonedDateTime.now();
                    msg.ack();
                    long afterAckTimeNanos = System.nanoTime();
                    ZonedDateTime afterAckDateTime = ZonedDateTime.now();
                    Result r = new Result(msg,
                        receivedTimeNanos, receivedDateTime,
                        afterAckTimeNanos, afterAckDateTime);
                    results.add(r);

                    doneLatch.countDown();
                    if (count.incrementAndGet() % consumerReportInterval == 0) {
                        System.out.println(DateTimeUtils.toRfc3339(ZonedDateTime.now()) + " " + msg.getHeaders().getFirst(HDR_MSG_ID));
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            };

            sub = js.subscribe(SUBJECT, d, mh, false);
        }

        @Override
        public void close() throws Exception {
            d.unsubscribe(sub);
        }
    }

    private static void createStream(Connection nc) throws Exception {
        JetStreamManagement jsm = nc.jetStreamManagement();
        try {
            nc.jetStreamManagement().deleteStream(STREAM);
        }
        catch (Exception e) {
            // don't care if it doesn't exist
        }
        StreamConfiguration streamConfig = StreamConfiguration.builder()
            .name(STREAM)
            .subjects(SUBJECT)
            .storageType(STORAGE_TYPE)
            .build();
        jsm.addStream(streamConfig);
    }

    private static void sleep(int amt) {
        try {
            Thread.sleep(amt);
        } catch (InterruptedException e) { /* ignore */ }
    }
}
