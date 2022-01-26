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
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.support.DateTimeUtils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nats.client.impl.JsTimingMessage.HDR_MSG_ID;

public class Consumer implements AutoCloseable {

    private final Connection conn;
    private final JetStream js;
    private final String queue;
    private final long maxAckPending;
    private final int ackInterval;
    private final int reportInterval;
    private final List<Result> results;
    private Dispatcher d;
    private JetStreamSubscription sub;

    public Consumer(Connection conn, String queue, long maxAckPending, int ackInterval, int reportInterval) throws IOException, JetStreamApiException {
        this.conn = conn;
        this.queue = queue;
        this.maxAckPending = maxAckPending > 0 ? maxAckPending : -1;
        this.ackInterval = ackInterval;
        this.reportInterval = reportInterval;

        js = conn.jetStream();

        results = new ArrayList<>();

        createPushSub();
    }

    private void createPushSub() throws IOException, JetStreamApiException {
        d = conn.createDispatcher();

        MessageHandler mh = msg -> {
            try {
                ZonedDateTime receivedDateTime = ZonedDateTime.now();

                if (results.size() + 1 % ackInterval == 0) {
                    msg.ack();
                }

                ZonedDateTime afterAckDateTime = ZonedDateTime.now();

                Result r = new Result(msg, receivedDateTime, afterAckDateTime);
                results.add(r);

                int count = results.size();
                if (count % ackInterval == 0) {
                    msg.ack();
                }

                if (count % reportInterval == 0) {
                    System.out.println(DateTimeUtils.toRfc3339(ZonedDateTime.now()) + " " + msg.getHeaders().getFirst(HDR_MSG_ID));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        AckPolicy ap = ackInterval == 1 ? AckPolicy.Explicit : (ackInterval > 1 ? AckPolicy.All : AckPolicy.None);

        PushSubscribeOptions pso = ConsumerConfiguration.builder()
            .ackPolicy(ap)
            .maxAckPending(maxAckPending)
            .flowControl(10000)
            .deliverGroup(queue)
            .buildPushSubscribeOptions();
        sub = js.subscribe(JsTimingExample.SUBJECT, d, mh, false, pso);
    }

    public int getReceivedCount() {
        return results.size();
    }

    public List<Result> getResults() {
        return Collections.unmodifiableList(results);
    }

    @Override
    public void close() throws Exception {
        try {
            if (sub != null) {
                if (d == null) {
                    sub.unsubscribe();
                }
                else {
                    d.unsubscribe(sub);
                }
            }
        }
        catch (Exception ignore) {}
        d = null;
        sub = null;
    }
}
