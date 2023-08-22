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

import java.time.Duration;

public class AppSimulator extends Thread {
    private final Settings settings;
    private final int appId;
    public final ConsumerAndSubscriber[] conAndSubs;

    public AppSimulator(Settings settings, int id) {
        this.settings = settings;
        this.appId = id;
        conAndSubs = new ConsumerAndSubscriber[settings.threadsPerApp];
    }

    @Override
    public void run() {

        Options options = settings.optionsBuilder.getBuilder().connectionTimeout(Duration.ofMillis(settings.timeoutMs)).build();
        try (Connection nc = Nats.connect(options)) {
            JetStreamOptions jso = JetStreamOptions.builder().requestTimeout(Duration.ofMillis(settings.timeoutMs)).build();
            JetStreamManagement jsm = nc.jetStreamManagement(jso);
            JetStream js = nc.jetStream(jso);
            Dispatcher d = nc.createDispatcher();

            int consumersEach = settings.consumersPerApp / settings.threadsPerApp;
            Thread[] threads = new Thread[settings.threadsPerApp];
            for (int tid = 0; tid < threads.length; tid++) {
                conAndSubs[tid] = new ConsumerAndSubscriber(settings, jsm, js, d, consumersEach, appId, tid);
                threads[tid] = new Thread(conAndSubs[tid]);
                threads[tid].start();
            }
            for (int i = 0; i < threads.length; i++) {
                Thread thread = threads[i];
                thread.join();
                conAndSubs[i].close();
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
