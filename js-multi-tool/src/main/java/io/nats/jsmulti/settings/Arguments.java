// Copyright 2021-2022 The NATS Authors
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

package io.nats.jsmulti.settings;

import io.nats.client.api.AckPolicy;
import io.nats.client.api.StorageType;
import io.nats.jsmulti.internal.Context;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static io.nats.jsmulti.settings.Action.*;

public class Arguments {

    public static final String INDIVIDUAL = "individual";
    public static final String SHARED = "shared";

    private final List<String> args = new ArrayList<>();

    public static Arguments builder() { return new Arguments(); }
    public static Arguments builder(String subject) { return builder().subject(subject); }
    public static Arguments pubSync(String subject) { return builder().action(PUB_SYNC).subject(subject); }
    public static Arguments pubAsync(String subject) { return builder().action(PUB_ASYNC).subject(subject); }
    public static Arguments pubCore(String subject) { return builder().action(PUB_CORE).subject(subject); }
    public static Arguments subPush(String subject) { return builder().action(SUB_PUSH).subject(subject); }
    public static Arguments subQueue(String subject) { return builder().action(SUB_QUEUE).subject(subject); }
    public static Arguments subPull(String subject) { return builder().action(SUB_PULL).subject(subject); }
    public static Arguments subPullQueue(String subject) { return builder().action(SUB_PULL_QUEUE).subject(subject); }

    public Context build() {
        return new Context(args.toArray(new String[0]));
    }

    public Arguments printCommandLine(PrintStream ps) {
        for (String a : args) {
            ps.print(a + " ");
        }
        ps.println("");
        return this;
    }

    public Arguments printCommandLine() {
        printCommandLine(System.out);
        return this;
    }

    private Arguments add(String option) {
        args.add("-" + option);
        return this;
    }

    private Arguments add(String option, Object value) {
        args.add("-" + option);
        args.add(value.toString());
        return this;
    }

    public Arguments action(Action action) {
        return add("a", action);
    }

    public Arguments server(String server) {
        return add("s", server);
    }

    public Arguments latencyFlag() {
        return add("lf");
    }

    public Arguments latencyFlag(boolean lf) {
        return lf ? add("lf") : this;
    }

    public Arguments optionsFactory(String optionsFactoryClassName) {
        return add("of", optionsFactoryClassName);
    }

    public Arguments reportFrequency(int reportFrequency) {
        return add("rf", reportFrequency);
    }

    public Arguments noReporting() {
        return add("rf", -1);
    }

    public Arguments memory() {
        return add("o", StorageType.Memory);
    }

    public Arguments file() {
        return add("o", StorageType.File);
    }

    public Arguments replicas(int replicas) {
        return add("c", replicas);
    }

    public Arguments subject(String subject) {
        if (subject == null) {
            return this;
        }
        return add("u", subject);
    }

    public Arguments messageCount(int messageCount) {
        return add("m", messageCount);
    }

    public Arguments threads(int threads) {
        return add("d", threads);
    }

    public Arguments individualConnection() {
        return add("n", INDIVIDUAL);
    }

    public Arguments sharedConnection() {
        return add("n", SHARED);
    }

    public Arguments sharedConnection(boolean shared) {
        return add("n", shared ? SHARED : INDIVIDUAL);
    }

    public Arguments jitter(long jitter) {
        return add("j", jitter);
    }

    public Arguments payloadSize(int payloadSize) {
        return add("ps", payloadSize);
    }

    public Arguments roundSize(int roundSize) {
        return add("rs", roundSize);
    }

    public Arguments ackPolicy(AckPolicy ackPolicy) {
        return add("kp", ackPolicy);
    }

    public Arguments ackExplicit() {
        return ackPolicy(AckPolicy.Explicit);
    }

    public Arguments ackNone() {
        return ackPolicy(AckPolicy.None);
    }

    public Arguments ackAll() {
        return ackPolicy(AckPolicy.All);
    }

    public Arguments ackFrequency(int ackFrequency) {
        return add("kf", ackFrequency);
    }

    public Arguments batchSize(int batchSize) {
        return add("bs", batchSize);
    }
}
