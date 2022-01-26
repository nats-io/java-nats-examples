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

package io.nats.jstiming;

import io.nats.client.*;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.FileOutputStream;

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
        // 128,
        1024,
//        16 * 1024,
//        64 * 1024
    } ;

    public static final int[] MESSAGE_COUNTS = new int[] {
        1000,
//        5000,
//        10_000,
//        100_000
    } ;

    public static void main(String[] args )
    {
        Options options = new Options.Builder()
            .server(URL)
            .errorListener(new ErrorListener() {})
            .build();
        try (FileOutputStream out = new FileOutputStream(OUTPUT_FILE);
             Connection conn1 = Nats.connect(options);
             Connection conn2 = Nats.connect(options))
        {
            String text = "Server Version," + conn1.getServerInfo().getVersion() + lineSeparator() + "Client Version," + Nats.CLIENT_VERSION + lineSeparator();
            System.out.print(text);
            out.write(text.getBytes());

            // WORK IN PROGRESS!!!
            Stats allStats = new Stats(-1, -1);
            for (int messageCount : MESSAGE_COUNTS) {
                for (int payloadSize : PAYLOAD_SIZES) {
                    Stats.writeHeader(System.out, messageCount, payloadSize);;
                    int consumerReportInterval = messageCount / 10;
                    createStream(conn1);

                    JetStream js = conn1.jetStream();

//                    Consumer c = new Consumer(conn1, js, messageCount, consumerReportInterval);

                    int batchSize = 500;
                    new Producer(Producer.Kind.ASYNC, conn2, SUBJECT, messageCount, payloadSize, batchSize, 1)
                        .run();

                    // TODO maybe handle pub acks make sure all messages were published
                    // although the latch will timeout if all messages don't get published

//                    c.doneLatch.await(10, TimeUnit.MINUTES);
//                    c.close();
//
//                    Stats s = new Stats(payloadSize, messageCount);
//                    for (Result r : c.results) {
//                        s.update(r);
//                    }
//                    allStats.update(s);
//                    s.writeTo(out, System.out);
                }
            }
            allStats.writeTo(out, System.out);
        }
        catch (Exception e) {
            e.printStackTrace();
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
