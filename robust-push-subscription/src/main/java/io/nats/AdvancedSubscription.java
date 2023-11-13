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

import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import io.nats.client.PushSubscribeOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AdvancedSubscription implements MessageHandler {
    public final AtomicLong lastAckedSequence;
    public final AtomicBoolean connected;
    public final AtomicReference<CompletableFuture<Boolean>> drainFuture;
    
    public PushSubscribeOptions pso;
    public JetStreamSubscription sub;

    public AdvancedSubscription() {
        this.lastAckedSequence = new AtomicLong(0);
        connected = new AtomicBoolean(true);
        drainFuture = new AtomicReference<>();
    }

    @Override
    public void onMessage(Message msg) throws InterruptedException {
        System.out.println("Message Received to " + hashCode() + "/" + sub.hashCode() + " on " + msg.getSubject() + " ==> " + new String(msg.getData()));
        msg.ack();
        lastAckedSequence.set(msg.metaData().streamSequence());
    }
}
