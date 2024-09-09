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

import io.nats.client.Connection;
import io.nats.client.JetStreamOptions;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.AckPolicy;
import io.nats.jsmulti.shared.ActionRunner;
import io.nats.jsmulti.shared.Application;
import io.nats.jsmulti.shared.OptionsFactory;
import io.nats.jsmulti.shared.Utils;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static io.nats.jsmulti.settings.Arguments.INDIVIDUAL;
import static io.nats.jsmulti.settings.Arguments.SHARED;
import static io.nats.jsmulti.shared.Utils.*;

public class Context {

    public static final long DEFAULT_READ_TIMEOUT_MS = 1000;
    public static final long DEFAULT_REQUEST_WAIT_MS = 1000;
    public static final long DEFAULT_MAX_WAIT_MS = 10000;
    public static final int MIN_WAIT_MS = 100;

    // ----------------------------------------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------------------------------------
    public final String id;
    public final Action action;
    public final ActionRunner customActionRunner;

    // latency
    public final boolean latencyFlag;
    public final String lcsv;

    // connection options
    public final String credsFile;
    public final long connectionTimeoutMillis;
    public final long reconnectWaitMillis;

    // general
    public final String stream;
    public final String subject;
    public final long messageCount;
    public final int threads;
    public final boolean connShared;
    public final long jitter;
    public final int payloadSize;
    public final int payloadVariants;
    public final int roundSize;
    public final AckPolicy ackPolicy;
    public final int ackAllFrequency;
    public final int batchSize;
    public final long reportFrequency;

    public final Duration requestWaitDuration;
    public final Duration readTimeoutDuration;
    public final Duration readMaxWaitDuration;

    // once per context for now
    public final String queueName;

    // constant now but might change in the future
    public final int maxPubRetries = 10;
    public final int ackWaitSeconds = 120;
    public final long postPubWaitMillis = 1000;

    // ----------------------------------------------------------------------------------------------------
    // Application
    // ----------------------------------------------------------------------------------------------------
    public final Application app;

    // ----------------------------------------------------------------------------------------------------
    // macros / state / vars allow direct access. Be careful though
    // ----------------------------------------------------------------------------------------------------
    public final List<String> subDurables = new ArrayList<>();

    // ----------------------------------------------------------------------------------------------------
    // macros / state / vars to access through methods instead of direct
    // ----------------------------------------------------------------------------------------------------
    private final String[] servers;
    private final OptionsFactory optionsFactory;
    private final long[] perThread;
    private final List<byte[]> payloads; // private and with getter in case I want to do more with payload later
    private final Map<String, AtomicLong> subscribeCounters = Collections.synchronizedMap(new HashMap<>());
    private final String subNameWhenQueue;
    private int lastServerIndex;

    public Options getOptions() throws Exception {
        return optionsFactory.getOptions(this, OptionsFactory.OptionsType.DEFAULT);
    }

    public Options getOptions(OptionsFactory.OptionsType ot) {
        return optionsFactory.getOptions(this, ot);
    }

    public JetStreamOptions getJetStreamOptions() throws Exception {
        return optionsFactory.getJetStreamOptions(this);
    }

    public Connection connect() throws Exception {
        return connect(OptionsFactory.OptionsType.DEFAULT);
    }

    public Connection connect( OptionsFactory.OptionsType ot) throws Exception {
        Options options = getOptions(ot);
        Connection nc = Nats.connect(options);
        for (long x = 0; x < 100; x++) { // waits up to 10 seconds (100 * 100 = 10000) millis to be connected
            Utils.sleep(100);
            if (nc.getStatus() == Connection.Status.CONNECTED) {
                return nc;
            }
        }
        return nc;
    }

    final ReentrantLock gplock = new ReentrantLock();
    public byte[] getPayload() {
        if (payloadVariants == 1) {
            return payloads.get(0);
        }

        gplock.lock();
        try {
            byte[] payload = payloads.remove(0);
            payloads.add(payload);
            return payload;
        }
        finally {
            gplock.unlock();
        }
    }

    public String getFirstServer() {
        return servers[0];
    }

    final ReentrantLock nextServerLock = new ReentrantLock();
    public String getNextServer() {
        nextServerLock.lock();
        try {
            if (++lastServerIndex >= servers.length) {
                lastServerIndex = 0;
            }
            return servers[lastServerIndex];
        }
        finally {
            nextServerLock.unlock();
        }
    }

    private String _getSubName(String pfx, int id) {
        // is a queue, use the same durable
        // not a queue, each durable is unique
        return action.isQueue() ? subNameWhenQueue : pfx + randomString() + id;
    }

    public String getSubName(int subId) {
        return _getSubName("sub", subId);
    }

    public String getSubDurable(int durableId) {
        String sd = _getSubName("dur", durableId);
        subDurables.add(sd);
        return sd;
    }

    public long getPubCount(int id) {
        return perThread[id-1]; // ids start at 1
    }

    public AtomicLong getSubscribeCounter(String key) {
        return subscribeCounters.computeIfAbsent(key, k -> new AtomicLong());
    }

    public String getLabel(int id) {
        return action + "-" + id;
    }

    // ----------------------------------------------------------------------------------------------------
    // ToString
    // ----------------------------------------------------------------------------------------------------
    @SuppressWarnings("unused")
    public void print() {
        System.out.println(this + "\n");
    }

    private static final String TS_TMPL = "\n  %-26s%s";
    private void append(StringBuilder sb, String label, String arg, Object value, boolean test) {
        if (test) {
            sb.append(String.format(TS_TMPL, label + " (-" + arg + "):", value));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JetStream Multi-Tool Run Config:");
        append(sb, "action", "a", action, true);
        append(sb, "latency flag", "lf", "Yes", latencyFlag);
        append(sb, "options factory", "of", optionsFactory.getClass().getTypeName(), true);
        append(sb, "report frequency", "rf", reportFrequency < 1 ? "no reporting" : "" + reportFrequency, true);

        append(sb, "request wait millis", "rqwms", requestWaitDuration, requestWaitDuration.toMillis() != DEFAULT_REQUEST_WAIT_MS);
        append(sb, "read timeout millis", "rtoms", readTimeoutDuration, readTimeoutDuration.toMillis() != DEFAULT_READ_TIMEOUT_MS);
        append(sb, "read max wait millis", "rmxwms", readMaxWaitDuration, readMaxWaitDuration.toMillis() != DEFAULT_MAX_WAIT_MS);

        append(sb, "subject", "u", subject, true);
        append(sb, "message count", "m", messageCount, true);
        append(sb, "threads", "d", threads, true);
        append(sb, "connection strategy", "n", connShared ? SHARED : INDIVIDUAL, threads > 1);

        append(sb, "payload size", "p", payloadSize + " bytes", action.isPubAction());
        append(sb, "payload variants", "p", payloadVariants, payloadVariants > 1 && action.isPubAction());

        append(sb, "jitter", "j", jitter, action.isPubAction());

        append(sb, "round size", "r", roundSize, action.isPubAsync());

        append(sb, "ack policy", "kp", ackPolicy, action.isSubAction());
        append(sb, "ack all frequency", "kf", ackAllFrequency, action.isPush() && ackPolicy == AckPolicy.All);

        append(sb, "batch size", "b", batchSize, action.isPull());

        return sb.toString();
    }

    // ----------------------------------------------------------------------------------------------------
    // Construction
    // ----------------------------------------------------------------------------------------------------
    public Context(Arguments args) {
        this(args.toStringArray());
    }

    public Context(String[] args) {
        id = makeId();

        Action _action = null;
        String _customActionClassName = null;
        String _customAppClassName = null;
        boolean _latencyFlag = false;
        String[] _servers = new String[]{Options.DEFAULT_URL};
        String _credsFile = null;
        long _connectionTimeoutMillis = 5000;
        long _reconnectWaitMillis = 1000;
        String _optionsFactoryClassName = null;
        Integer _reportFrequency = null;
        String _stream = null;
        String _subject = "sub" + randomString();
        int _messageCount = 100_000;
        int _threads = 1;
        boolean _connShared = true;
        long _jitter = 0;
        int _payloadSize = 128;
        int _payloadVariants = 1;
        int _roundSize = MIN_WAIT_MS;
        AckPolicy _ackPolicy = AckPolicy.Explicit;
        int _ackAllFrequency = 1;
        int _batchSize = 10;
        String _lcsv = null;
        String _queueName = "qn" + randomString();
        String _subDurableWhenQueue = "qd" + randomString();
        long _requestWaitMillis = DEFAULT_REQUEST_WAIT_MS;
        long _readTimeoutMillis = DEFAULT_READ_TIMEOUT_MS;
        long _readMaxWaitMillis = DEFAULT_MAX_WAIT_MS;

        if (args != null && args.length > 0) {
            try {
                for (int x = 0; x < args.length; x++) {
                    String arg = args[x].trim();
                    switch (arg) {
                        case "-s":
                        case "-server":
                            _servers = asString(args[++x]).split(",");
                            break;
                        case "-lcsv":
                        case "-latency_csv_file_spec":
                            _lcsv = asString(args[++x]);
                            break;
                        case "-of":
                        case "-options_factory_class_name":
                            _optionsFactoryClassName = asString(args[++x]);
                            break;
                        case "-a":
                        case "-action":
                            _action = Action.getInstance(asString(args[++x]));
                            if (_action == null) {
                                error("Valid action required!");
                            }
                            break;
                        case "-ca":
                        case "-_custom_action_class_name":
                            _action = Action.CUSTOM;
                            _customActionClassName = asString(args[++x]);
                            break;
                        case "-app":
                            _customAppClassName = asString(args[++x]);
                            break;
                        case "-lf":
                        case "-latency_flag":
                            _latencyFlag = true;
                            break;
                        case "-t":
                        case "-stream":
                            _stream = asString(args[++x]);
                            break;
                        case "-u":
                        case "-subject":
                            _subject = asString(args[++x]);
                            break;
                        case "-m":
                        case "-message_count":
                            _messageCount = asInt("total messages", args[++x], -1);
                            break;
                        case "-ps":
                        case "-payload_size":
                            _payloadSize = asInt("payload size", args[++x], 64 * 1024 * 1024); // 67108864
                            break;
                        case "-pv":
                        case "-payload_variants":
                            _payloadVariants = asInt("payload variants", args[++x], 100); // 67108864
                            break;
                        case "-bs":
                        case "-batch_size":
                            _batchSize = asInt("batch size", args[++x], 200);
                            break;
                        case "-rs":
                        case "-round_size":
                            _roundSize = asInt("round size", args[++x], 1000);
                            break;
                        case "-d":
                        case "-threads":
                            _threads = asInt("number of threads", args[++x], 20);
                            break;
                        case "-j":
                        case "-jitter":
                            _jitter = asInt("jitter", args[++x], 10_000);
                            break;
                        case "-n":
                        case "-connection_strategy":
                            _connShared = bool("connection strategy", args[++x], SHARED, INDIVIDUAL);
                            break;
                        case "-kp":
                        case "-ack_policy":
                            _ackPolicy = AckPolicy.get(asString(args[++x]).toLowerCase());
                            if (_ackPolicy == null) {
                                error("Invalid Ack Policy, must be one of [explicit, none, all]");
                            }
                            break;
                        case "-kf":
                        case "-ack_all_frequency":
                            _ackAllFrequency = asInt("ack frequency", args[++x], MIN_WAIT_MS);
                            break;
                        case "-rf":
                        case "-report_frequency":
                            _reportFrequency = asInt("report frequency", args[++x]);
                            break;
                        case "-cf":
                        case "-creds_file":
                            _credsFile = asString(args[++x]);
                            break;
                        case "-ctms":
                        case "-connection_timeout_millis":
                            _connectionTimeoutMillis = asInt("connection timeout millis", args[++x]);
                            break;
                        case "-rwms":
                        case "-recconect_wait_millis":
                            _reconnectWaitMillis = asInt("reconnect wait millis", args[++x]);
                            break;
                        case "-q":
                        case "-queue":
                        case "-queue_name":
                            _queueName = asString(args[++x]);
                            break;
                        case "-sdwq":
                        case "-sub_durable_when_queue":
                            _subDurableWhenQueue = asString(args[++x]);
                            break;
                        case "-rqwms":
                        case "-request_wait_Millis":
                            _requestWaitMillis = asInt("request wait millis", args[++x]);
                            break;
                        case "-rtoms":
                        case "-read_timeout_millis":
                            _readTimeoutMillis = asInt("read timeout wait millis", args[++x]);
                            break;
                        case "-rmxwms":
                        case "-read_max_wait_millis":
                            _readMaxWaitMillis = asInt("read max wait millis", args[++x]);
                            break;
                        case "":
                            break;
                        default:
                            error("Unknown argument: " + arg);
                            break;
                    }
                }
            }
            catch (Exception e) {
                error("Exception while parsing, most likely missing an argument value.");
            }
        }

        if (_action == Action.CUSTOM) {
            if (_customActionClassName == null) {
                action = null;
                customActionRunner = null;
            }
            else {
                action = _action;
                customActionRunner = (ActionRunner)classForName(_customActionClassName, "Custom Action Runner");
            }
        }
        else {
            action = _action;
            customActionRunner = null;
        }

        // all errors exit
        if (_action == null) {
            error("Valid action required!");
        }
        else if (_action.requiresStreamName() && _stream == null) {
            error("Action requires stream name");
        }
        else if (_messageCount < 1) {
            error("Message count required!");
        }
        else if (_threads == 1 && _action.isQueue()) {
            error("Queue subscribing requires multiple threads!");
        }
        else if (_requestWaitMillis < MIN_WAIT_MS) {
            error("Request wait millis is too short!");
        }
        else if (_readTimeoutMillis < MIN_WAIT_MS) {
            error("Request read timeout millis is too short!");
        }
        else if (_readMaxWaitMillis < _readTimeoutMillis) {
            error("Request max wait millis is too short!");
        }

        latencyFlag = _latencyFlag;
        servers = _servers;
        credsFile = _credsFile;
        connectionTimeoutMillis = _connectionTimeoutMillis;
        reconnectWaitMillis = _reconnectWaitMillis;
        stream = _stream;
        subject = _subject;
        lcsv = _lcsv;
        messageCount = _messageCount;
        threads = _threads;
        connShared = _connShared;
        jitter = _jitter;
        payloadSize = _payloadSize;
        payloadVariants = _payloadVariants;
        roundSize = _roundSize;
        ackPolicy = _ackPolicy;
        ackAllFrequency = _ackAllFrequency;
        batchSize = _batchSize;

        requestWaitDuration = Duration.ofMillis(_requestWaitMillis);
        readTimeoutDuration = Duration.ofMillis(_readTimeoutMillis);
        readMaxWaitDuration = Duration.ofMillis(_readMaxWaitMillis);

        if (_reportFrequency == null) {
            reportFrequency = messageCount / 10;
        }
        else {
            reportFrequency = _reportFrequency;
        }

        queueName = _queueName;
        subNameWhenQueue = _subDurableWhenQueue;
        lastServerIndex = servers.length;   // will roll to 0 first use, see getNextServer

        payloads = new ArrayList<>();
        if (payloadVariants == 1) {
            payloads.add(new byte[payloadSize]);
        }
        else {
            int lower = payloadSize - (payloadSize / 10);
            int upper = payloadSize + (payloadSize / 10);
            for (int i = 0; i < payloadVariants; i++) {
                payloads.add(new byte[ThreadLocalRandom.current().nextInt(lower, upper)]);
            }
        }

        long total = 0;
        perThread = new long[threads];
        for (int x = 0; x < threads; x++) {
            perThread[x] = messageCount / threads;
            total += perThread[x];
        }

        int ix = -1;
        while (total < messageCount) {
            if (++ix == threads) {
                ix = 0;
            }
            perThread[ix]++;
            total++;
        }

        if (_customAppClassName == null) {
            app = new Application() {};
        }
        else {
            app = (Application)classForName(_customAppClassName, "Custom Application");
        }

        if (_optionsFactoryClassName == null) {
            OptionsFactory appOf = app.getOptionsFactory();
            if (appOf == null) {
                optionsFactory = new OptionsFactory() {};
            }
            else {
                optionsFactory = appOf;
            }
        }
        else {
            optionsFactory = (OptionsFactory)classForName(_optionsFactoryClassName, "Options Factory");
        }

        app.init(this);
    }

    @SuppressWarnings("SameParameterValue")
    private Object classForName(String className, String label) {
        try {
            Class<?> c = Class.forName(className);
            Constructor<?> cons = c.getConstructor();
            return cons.newInstance();
        }
        catch (Exception e) {
            System.err.println("Error creating " + label + ": " + e);
            throw new RuntimeException();
        }
    }

    private void error(String errMsg) {
        if (app == null) {
            System.err.println("ERROR: " + errMsg);
            System.exit(-1);
        }
        else {
            app.reportErr("ERROR: " + errMsg);
            app.exit(-1);
        }
    }

    private String asString(String val) {
        return val.trim();
    }

    private int asInt(String name, String val) {
        return asInt(name, val, -2);
    }

    private int asInt(String name, String val, int upper) {
        int v = parseInt(val);
        if (upper == -2 && v < 1) {
            return Integer.MAX_VALUE;
        }
        if (upper > 0) {
            if (v > upper) {
                error("Value for " + name + " cannot exceed " + upper);
            }
        }
        return v;
    }

    private int asInt(String name, String val, int lower, int upper) {
        int v = parseInt(val);
        if (v < lower) {
            error("Value for " + name + " cannot be less than " + lower);
        }
        if (v > upper) {
            error("Value for " + name + " cannot exceed " + upper);
        }
        return v;
    }

    private long asLong(String name, String val) {
        return asLong(name, val, -2);
    }

    private long asLong(String name, String val, long upper) {
        long v = parseLong(val);
        if (upper == -2 && v < 1) {
            return Long.MAX_VALUE;
        }
        if (upper > 0) {
            if (v > upper) {
                error("Value for " + name + " cannot exceed " + upper);
            }
        }
        return v;
    }

    private long asLong(String name, String val, long lower, long upper) {
        long v = parseLong(val);
        if (v < lower) {
            error("Value for " + name + " cannot be less than " + lower);
        }
        if (v > upper) {
            error("Value for " + name + " cannot exceed " + upper);
        }
        return v;
    }

    private boolean bool(String name, String val, String trueChoice, String falseChoice) {
        return choiceInt(name, val, trueChoice, falseChoice) == 0;
    }

    private int choiceInt(String name, String val, String... choices) {
        String value = asString(val);
        for (int x = 0; x < choices.length; x++) {
            if (value.equalsIgnoreCase(choices[x])) {
                return x;
            }
        }

        error("Invalid choice for " + name + " [" + value + "]. Must be one of " + Arrays.toString(choices));
        return -1;
    }

    private String choice(String name, String val, String... choices) {
        return choices[choiceInt(name, val, choices)];

    }
}
