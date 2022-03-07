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

package io.nats.jsmulti;

import io.nats.client.api.AckPolicy;
import io.nats.client.api.StorageType;

import java.util.ArrayList;
import java.util.List;

import static io.nats.jsmulti.Utils.*;

class ArgumentBuilder {
    private final List<String> args = new ArrayList<>();

    static ArgumentBuilder pubSync(String subject) { return new ArgumentBuilder().action(PUB_SYNC).subject(subject); }
    static ArgumentBuilder pubAsync(String subject) { return new ArgumentBuilder().action(PUB_ASYNC).subject(subject); }
    static ArgumentBuilder pubCore(String subject) { return new ArgumentBuilder().action(PUB_CORE).subject(subject); }
    static ArgumentBuilder subPush(String subject) { return new ArgumentBuilder().action(SUB_PUSH).subject(subject); }
    static ArgumentBuilder subQueue(String subject) { return new ArgumentBuilder().action(SUB_QUEUE).subject(subject); }
    static ArgumentBuilder subPull(String subject) { return new ArgumentBuilder().action(SUB_PULL).subject(subject); }
    static ArgumentBuilder subPullQueue(String subject) { return new ArgumentBuilder().action(SUB_PULL_QUEUE).subject(subject); }

    Arguments build() {
        return new Arguments(args.toArray(new String[0]));
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

    ArgumentBuilder action(String action) {
        return add("a", action);
    }

    ArgumentBuilder server(String server) {
        return add("s", server);
    }

    ArgumentBuilder latencyAction(String action) {
        return add("la", action);
    }

    ArgumentBuilder latencyTracking() {
        return add("lt");
    }

    ArgumentBuilder optionsFactory(String optionsFactoryClassName) {
        return add("of", optionsFactoryClassName);
    }

    ArgumentBuilder reportFrequency(int reportFrequency) {
        return add("rf", reportFrequency);
    }

    ArgumentBuilder noReporting() {
        return add("rf", -1);
    }

    ArgumentBuilder storageType(StorageType storageType) {
        return add("o", storageType);
    }

    ArgumentBuilder memory() {
        return storageType(StorageType.Memory);
    }

    ArgumentBuilder file() {
        return storageType(StorageType.File);
    }

    ArgumentBuilder replicas(int replicas) {
        return add("c", replicas);
    }

    ArgumentBuilder subject(String subject) {
        if (subject == null) {
            return this;
        }
        return add("u", subject);
    }

    ArgumentBuilder messageCount(int messageCount) {
        return add("m", messageCount);
    }

    ArgumentBuilder threads(int threads) {
        return add("d", threads);
    }

    ArgumentBuilder connectionStrategy(String strategy) {
        return add("n", strategy);
    }

    ArgumentBuilder sharedConnection() {
        return connectionStrategy(SHARED);
    }

    ArgumentBuilder individualConnection() {
        return connectionStrategy(INDIVIDUAL);
    }

    ArgumentBuilder jitter(long jitter) {
        return add("j", jitter);
    }

    ArgumentBuilder payloadSize(int payloadSize) {
        return add("ps", payloadSize);
    }

    ArgumentBuilder roundSize(int roundSize) {
        return add("rs", roundSize);
    }

    ArgumentBuilder pullType(String pullType) {
        return add("pt", pullType);
    }

    ArgumentBuilder iterate() {
        return pullType(ITERATE);
    }

    ArgumentBuilder fetch() {
        return pullType(FETCH);
    }

    ArgumentBuilder ackPolicy(AckPolicy ackPolicy) {
        return add("kp", ackPolicy);
    }

    ArgumentBuilder ackExplicit() {
        return ackPolicy(AckPolicy.Explicit);
    }

    ArgumentBuilder ackNone() {
        return ackPolicy(AckPolicy.None);
    }

    ArgumentBuilder ackAll() {
        return ackPolicy(AckPolicy.All);
    }

    ArgumentBuilder ackFrequency(int ackFrequency) {
        return add("kf", ackFrequency);
    }

    ArgumentBuilder batchSize(int batchSize) {
        return add("bs", batchSize);
    }
}