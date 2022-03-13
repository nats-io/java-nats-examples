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

public class ArgumentBuilder {

    public static final String INDIVIDUAL = "individual";
    public static final String SHARED = "shared";
    public static final String FETCH = "fetch";

    private final List<String> args = new ArrayList<>();

    public static ArgumentBuilder builder() { return new ArgumentBuilder(); }
    public static ArgumentBuilder builder(String subject) { return builder().subject(subject); }
    public static ArgumentBuilder pubSync(String subject) { return builder().action(PUB_SYNC).subject(subject); }
    public static ArgumentBuilder pubAsync(String subject) { return builder().action(PUB_ASYNC).subject(subject); }
    public static ArgumentBuilder pubCore(String subject) { return builder().action(PUB_CORE).subject(subject); }
    public static ArgumentBuilder subPush(String subject) { return builder().action(SUB_PUSH).subject(subject); }
    public static ArgumentBuilder subQueue(String subject) { return builder().action(SUB_QUEUE).subject(subject); }
    public static ArgumentBuilder subPull(String subject) { return builder().action(SUB_PULL).subject(subject); }
    public static ArgumentBuilder subPullQueue(String subject) { return builder().action(SUB_PULL_QUEUE).subject(subject); }

    public Context build() {
        return new Context(args.toArray(new String[0]));
    }

    public ArgumentBuilder print(PrintStream ps) {
        for (String a : args) {
            ps.print(a + " ");
        }
        ps.println("");
        return this;
    }

    public ArgumentBuilder print() {
        print(System.out);
        return this;
    }

    private ArgumentBuilder add(String option) {
        args.add("-" + option);
        return this;
    }

    private ArgumentBuilder add(String option, Object value) {
        args.add("-" + option);
        args.add(value.toString());
        return this;
    }

    public ArgumentBuilder action(Action action) {
        return add("a", action);
    }

    public ArgumentBuilder server(String server) {
        return add("s", server);
    }

    public ArgumentBuilder latencyFlag() {
        return add("lf");
    }

    public ArgumentBuilder latencyFlag(boolean lf) {
        return lf ? add("lf") : this;
    }

    public ArgumentBuilder optionsFactory(String optionsFactoryClassName) {
        return add("of", optionsFactoryClassName);
    }

    public ArgumentBuilder reportFrequency(int reportFrequency) {
        return add("rf", reportFrequency);
    }

    public ArgumentBuilder noReporting() {
        return add("rf", -1);
    }

    public ArgumentBuilder memory() {
        return add("o", StorageType.Memory.toString());
    }

    public ArgumentBuilder file() {
        return add("o", StorageType.File);
    }

    public ArgumentBuilder replicas(int replicas) {
        return add("c", replicas);
    }

    public ArgumentBuilder subject(String subject) {
        if (subject == null) {
            return this;
        }
        return add("u", subject);
    }

    public ArgumentBuilder messageCount(int messageCount) {
        return add("m", messageCount);
    }

    public ArgumentBuilder threads(int threads) {
        return add("d", threads);
    }

    public ArgumentBuilder connectionStrategy(String strategy) {
        return add("n", strategy);
    }

    public ArgumentBuilder sharedConnection() {
        return connectionStrategy(SHARED);
    }

    public ArgumentBuilder sharedConnection(boolean shared) {
        return connectionStrategy(shared ? SHARED : INDIVIDUAL);
    }

    public ArgumentBuilder individualConnection() {
        return connectionStrategy(INDIVIDUAL);
    }

    public ArgumentBuilder jitter(long jitter) {
        return add("j", jitter);
    }

    public ArgumentBuilder payloadSize(int payloadSize) {
        return add("ps", payloadSize);
    }

    public ArgumentBuilder roundSize(int roundSize) {
        return add("rs", roundSize);
    }

//    public ArgumentBuilder pullType(String pullType) {
//        return add("pt", pullType);
//    }
//
//    public ArgumentBuilder fetch() {
//        return pullType(FETCH);
//    }
//
//    public ArgumentBuilder fetch(boolean fetch) {
//        return pullType(fetch ? FETCH : ITERATE);
//    }

    public ArgumentBuilder ackPolicy(AckPolicy ackPolicy) {
        return add("kp", ackPolicy);
    }

    public ArgumentBuilder ackExplicit() {
        return ackPolicy(AckPolicy.Explicit);
    }

    public ArgumentBuilder ackNone() {
        return ackPolicy(AckPolicy.None);
    }

    public ArgumentBuilder ackAll() {
        return ackPolicy(AckPolicy.All);
    }

    public ArgumentBuilder ackFrequency(int ackFrequency) {
        return add("kf", ackFrequency);
    }

    public ArgumentBuilder batchSize(int batchSize) {
        return add("bs", batchSize);
    }
}
