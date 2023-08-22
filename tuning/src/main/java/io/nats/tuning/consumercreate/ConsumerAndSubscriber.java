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

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;

public class ConsumerAndSubscriber implements Runnable {
    Settings settings;
    JetStreamManagement jsm;
    JetStream js;
    Dispatcher d;
    int consumersEach;
    int appId;
    int threadId;
    public JetStreamSubscription[] subs;
    public long[] createTime;
    public long[] subscribeTime;

    public ConsumerAndSubscriber(Settings settings, JetStreamManagement jsm, JetStream js, Dispatcher d, int consumersEach, int appId, int threadId) {
        this.settings = settings;
        this.jsm = jsm;
        this.js = js;
        this.d = d;
        this.consumersEach = consumersEach;
        subs = new JetStreamSubscription[consumersEach];
        createTime = new long[consumersEach];
        subscribeTime = new long[consumersEach];
        this.appId = appId;
        this.threadId = threadId;
    }

    public void close() {
        for (JetStreamSubscription sub : subs) {
            try {
                sub.unsubscribe();
            }
            catch (Exception ignore) {
            }
        }
    }

    private static void sleep(long ms) {
        if (ms < 1) {
            return;
        }
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException ignore) {
        }
    }

    @Override
    public void run() {
        for (int conIx = 0; conIx < consumersEach; conIx++) {
            String name = getName(conIx);
            subscribeTime[conIx] = Long.MIN_VALUE;
            createConsumer(conIx, name);
            if (settings.subBehavior == SubBehavior.Immediately) {
                subscribe(conIx, name);
            }
        }
        if (settings.subBehavior == SubBehavior.After_Creates) {
            for (int conIx = 0; conIx < consumersEach; conIx++) {
                subscribe(conIx, getName(conIx));
            }
        }
    }

    private void createConsumer(int conIx, String name) {
        try {
            sleep(settings.beforeCreateDelayMs);
            createTime[conIx] = System.nanoTime();
            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                .name(name)
                .inactiveThreshold(settings.inactiveThresholdMs)
                .deliverSubject(NUID.nextGlobal())
                .build();
            jsm.addOrUpdateConsumer(settings.streamName, cc);
            createTime[conIx] = System.nanoTime() - createTime[conIx];
            if (conIx == 0 || conIx % settings.reportFrequency == 0) {
                System.out.println("CON " + name + " | " + (createTime[conIx] / 1_000_000) + "ms");
            }
        }
        catch (Exception e) {
            System.err.println("CON EX " + name + " | " + e);
            createTime[conIx] = -1;
        }
    }

    private void subscribe(int conIx, String name) {
        if (createTime[conIx] == -1) {
            return;
        } // create failed, can't subscribe
        try {
            sleep(settings.beforeSubDelayMs);
            PushSubscribeOptions pso = PushSubscribeOptions.bind(settings.streamName, name);
            subscribeTime[conIx] = System.nanoTime();
            subs[conIx] = js.subscribe(null, d, Message::ack, false, pso);
            subscribeTime[conIx] = System.nanoTime() - subscribeTime[conIx];
            if (conIx == 0 || conIx % settings.reportFrequency == 0) {
                System.out.println("SUB " + name + " | " + (subscribeTime[conIx] / 1_000_000) + "ms");
            }
        }
        catch (Exception e) {
            System.err.println("SUB EX " + name + " | " + e);
            subscribeTime[conIx] = -1;
        }
    }

    private String getName(int conIx) {
        return "App" + appId + "-Thread" + threadId + "-Id" + conIx;
    }
}
