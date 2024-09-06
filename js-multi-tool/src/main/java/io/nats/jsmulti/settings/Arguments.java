// Copyright 2021-2024 The NATS Authors
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
import io.nats.client.support.JsonParser;
import io.nats.client.support.JsonValue;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.nats.jsmulti.settings.Action.*;

public class Arguments {

    public static final String INDIVIDUAL = "individual";
    public static final String SHARED = "shared";

    public final List<String> args = new ArrayList<>();

    public static Arguments instance() { return new Arguments(); }
    public static Arguments instance(String subject) { return instance().subject(subject); }
    public static Arguments rtt() { return instance().action(RTT); }
    public static Arguments pubSync(String subject) { return instance().action(PUB_SYNC).subject(subject); }
    public static Arguments pubAsync(String subject) { return instance().action(PUB_ASYNC).subject(subject); }
    public static Arguments pubCore(String subject) { return instance().action(PUB_CORE).subject(subject); }
    public static Arguments subCore(String subject) { return instance().action(SUB_CORE).subject(subject); }
    public static Arguments subPush(String subject) { return instance().action(SUB_PUSH).subject(subject); }
    public static Arguments subQueue(String subject) { return instance().action(SUB_QUEUE).subject(subject); }
    public static Arguments subPull(String subject) { return instance().action(SUB_PULL).subject(subject); }
    public static Arguments subPullQueue(String subject) { return instance().action(SUB_PULL_QUEUE).subject(subject); }
    public static Arguments subPullRead(String subject) { return instance().action(SUB_PULL_READ).subject(subject); }
    public static Arguments subPullReadQueue(String subject) { return instance().action(SUB_PULL_READ_QUEUE).subject(subject); }

    public Arguments addJsonConfigFile(String jsonFileSpec) throws IOException {
        return _addJsonConfig(JsonParser.parse(Files.readAllBytes(Paths.get(jsonFileSpec))));
    }

    public Arguments addJsonConfig(String json) throws IOException {
        return _addJsonConfig(JsonParser.parse(json));
    }

    private Arguments _addJsonConfig(JsonValue jv) {
        if (jv.map != null) {
            for (Map.Entry<String, JsonValue> entry : jv.map.entrySet()) {
                JsonValue value = entry.getValue();
                switch (value.type) {
                    case STRING:
                        add(entry.getKey(), value.string);
                        break;
                    case INTEGER:
                        add(entry.getKey(), value.i.toString());
                        break;
                    case LONG:
                        add(entry.getKey(), value.l.toString());
                        break;
                }
            }
        }
        return this;
    }

    public Arguments add(String option) {
        args.add("-" + option);
        return this;
    }

    public Arguments add(String option, Object value) {
        args.add("-" + option);
        args.add(value.toString());
        return this;
    }

    public Arguments add(String[] inArgs) {
        args.addAll(Arrays.asList(inArgs));
        return this;
    }

    public Arguments action(Action action) {
        return add("a", action);
    }

    public Arguments customAction(String customActionClassName) {
        return add("ca", customActionClassName);
    }

    @SuppressWarnings("rawtypes")
    public Arguments customAction(Class clazz) {
        return add("ca", clazz.getCanonicalName());
    }

    public Arguments appClass(String customAppClass) {
        return add("app", customAppClass);
    }

    @SuppressWarnings("rawtypes")
    public Arguments appClass(Class clazz) {
        return add("app", clazz.getCanonicalName());
    }

    public Arguments server(String server) {
        return add("s", server);
    }

    public Arguments credsFile(String credsFile) {
        return add("cf", credsFile);
    }

    public Arguments connectionTimeoutMillis(long connectionTimeoutMillis) {
        return add("ctms", connectionTimeoutMillis);
    }

    public Arguments reconnectWaitMillis(long reconnectWaitMillis) {
        return add("rwms", reconnectWaitMillis);
    }

    public Arguments latencyFlag() {
        return add("lf");
    }

    public Arguments latencyFlag(boolean lf) {
        return lf ? add("lf") : this;
    }

    public Arguments latencyCsv(String latencyCsvFileSpec) {
        return add("lcsv", latencyCsvFileSpec);
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

    public Arguments queueName(String queueName) {
        return add("q", queueName);
    }

    public Arguments subDurableWhenQueue(String subDurableWhenQueue) {
        return add("sdwq", subDurableWhenQueue);
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

    public Arguments payloadVariants(int payloadVariants) {
        return add("pv", payloadVariants);
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

    public Arguments ackAllFrequency(int ackAllFrequency) {
        return add("kf", ackAllFrequency);
    }

    public Arguments batchSize(int batchSize) {
        return add("bs", batchSize);
    }

    public Arguments requestWaitMillis(long requestWaitMillis) {
        return add("rqwms", requestWaitMillis);
    }

    public Arguments readTimeoutMillis(long readTimeoutMillis) {
        return add("rtoms", readTimeoutMillis);
    }

    public Arguments readMaxWaitMillis(long readMaxWaitMillis) {
        return add("rmxwms", readMaxWaitMillis);
    }

    public String[] toStringArray() {
        return args.toArray(new String[0]);
    }

    public void printCommandLine(PrintStream ps) {
        for (String a : args) {
            ps.print(a + " ");
        }
        ps.println();
    }

    public void printCommandLineFormatted(PrintStream ps) {
        for (int i = 0; i < args.size(); i++) {
            String k = args.get(i);
            String v = args.get(++i);
            ps.println(k + " " + v);
        }
        ps.println();
    }

    public void printCommandLine() {
        printCommandLine(System.out);
    }

    public void printCommandLineFormatted() {
        printCommandLineFormatted(System.out);
    }
}
