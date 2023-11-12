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

package io.nats;

import io.nats.client.*;
import io.nats.client.api.ServerInfo;
import io.nats.client.support.Status;

import java.util.logging.Level;

public class ExampleErrorListener implements ErrorListener {
    public void errorOccurred(final Connection conn, final String error) {
        report(Level.SEVERE, "errorOccurred", conn, null, null, "Error: ", error);
    }

    @Override
    public void exceptionOccurred(final Connection conn, final Exception exp) {
        report(Level.SEVERE, "exceptionOccurred", conn, null, null, "Exception: ", exp);
    }

    @Override
    public void slowConsumerDetected(final Connection conn, final Consumer consumer) {
        report(Level.WARNING, "slowConsumerDetected", conn, consumer, null);
    }

    @Override
    public void messageDiscarded(final Connection conn, final Message msg) {
        report(Level.INFO, "messageDiscarded", conn, null, null, "Message: ", msg);
    }

    @Override
    public void heartbeatAlarm(final Connection conn, final JetStreamSubscription sub,
                               final long lastStreamSequence, final long lastConsumerSequence) {
        report(Level.SEVERE, "heartbeatAlarm", conn, null, sub, "lastStreamSequence: ", lastStreamSequence, "lastConsumerSequence: ", lastConsumerSequence);
    }

    @Override
    public void unhandledStatus(final Connection conn, final JetStreamSubscription sub, final Status status) {
        report(Level.WARNING, "unhandledStatus", conn, null, sub, "Status:", status);
    }

    @Override
    public void pullStatusWarning(Connection conn, JetStreamSubscription sub, Status status) {
        report(Level.WARNING, "pullStatusWarning", conn, null, sub, "Status:", status);
    }

    @Override
    public void pullStatusError(Connection conn, JetStreamSubscription sub, Status status) {
        report(Level.SEVERE, "pullStatusError", conn, null, sub, "Status:", status);
    }

    @Override
    public void flowControlProcessed(Connection conn, JetStreamSubscription sub, String id, FlowControlSource source) {
        report(Level.INFO, "flowControlProcessed", conn, null, sub, "FlowControlSource:", source);
    }

    private void report(Level level, String label, Connection conn, Consumer consumer, Subscription sub, Object... pairs) {
        StringBuilder sb = new StringBuilder("Error Listener: ").append(level.toString()).append(": ").append(label);
        if (conn != null) {
            ServerInfo si = conn.getServerInfo();
            if (si != null) {
                sb.append(", Connection: ").append(conn.getServerInfo().getClientId());
            }
        }
        if (consumer != null) {
            sb.append(", Consumer: ").append(consumer.hashCode());
        }
        if (sub != null) {
            sb.append(", Subscription: ").append(sub.hashCode());
            if (sub instanceof JetStreamSubscription) {
                JetStreamSubscription jssub = (JetStreamSubscription)sub;
                sb.append(", Consumer Name: ").append(jssub.getConsumerName());
            }
        }
        for (int x = 0; x < pairs.length; x++) {
            sb.append(", ").append(pairs[x]).append(pairs[++x]);
        }
        if (level == Level.SEVERE) {
            System.err.println(sb);
        }
        else {
            System.out.println(sb);
        }
    }
}
