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

import io.nats.client.Connection;
import io.nats.client.JetStreamSubscription;
import io.nats.client.impl.ErrorListenerLoggerImpl;
import io.nats.client.support.Status;

public class ExampleErrorListener extends ErrorListenerLoggerImpl {
    @Override
    public void heartbeatAlarm(Connection conn, JetStreamSubscription sub, long lastStreamSequence, long lastConsumerSequence) {
        System.err.println("Error Listener: Heartbeat Alarm for '" + sub.getConsumerName() + "', lastStreamSequence=" + lastStreamSequence + ", lastConsumerSequence=" + lastConsumerSequence);
    }

    @Override
    public void pullStatusWarning(Connection conn, JetStreamSubscription sub, Status status) {
        System.out.println("Error Listener: Pull Status Warning for '" + sub.getConsumerName() + "', " + status);
    }

    @Override
    public void pullStatusError(Connection conn, JetStreamSubscription sub, Status status) {
        System.err.println("Error Listener: Pull Status Error for '" + sub.getConsumerName() + "', " + status);
    }
}
