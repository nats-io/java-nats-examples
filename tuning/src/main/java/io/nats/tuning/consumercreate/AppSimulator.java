// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

package io.nats.tuning.consumercreate;

import io.nats.client.*;

import java.time.Duration;

import static io.nats.tuning.support.Utils.reportEx;

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
            reportEx(e);
        }
    }
}
