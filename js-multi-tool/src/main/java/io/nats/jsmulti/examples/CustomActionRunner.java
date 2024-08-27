// Copyright 2022 The NATS Authors
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

package io.nats.jsmulti.examples;

import io.nats.client.Connection;
import io.nats.jsmulti.JsMulti;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.settings.Context;
import io.nats.jsmulti.shared.ActionRunner;
import io.nats.jsmulti.shared.Stats;

import static io.nats.jsmulti.shared.Utils.report;
import static io.nats.jsmulti.shared.Utils.reportMaybe;

public class CustomActionRunner implements ActionRunner {
    public static void main(String[] args) throws Exception {
        // You could code this to use args to create the Arguments
        Arguments a = Arguments.instance()
            .customAction(CustomActionRunner.class.getCanonicalName())
            .messageCount(1_000_000)
            .reportFrequency(50000);

        a.printCommandLine();

        Context ctx = new Context(a);

        JsMulti.run(ctx);
    }

    @Override
    public void run(Context ctx, Connection nc, Stats stats, int id) throws Exception {
        int round = 0;
        int unReported = 0;
        report(round, "Begin Custom", ctx.app);
        while (round < ctx.messageCount) {
            stats.manualElapsed(nc.RTT().toNanos(), 1);
            unReported = reportMaybe(ctx, ++round, ++unReported, "Customs so far");
        }
        report(round, "Customs completed", ctx.app);
    }
}
