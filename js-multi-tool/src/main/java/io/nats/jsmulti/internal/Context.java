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

package io.nats.jsmulti.internal;

import io.nats.client.Options;
import io.nats.client.api.AckPolicy;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Arguments;
import io.nats.jsmulti.shared.DefaultOptionsFactory;
import io.nats.jsmulti.shared.OptionsFactory;
import io.nats.jsmulti.shared.Usage;

import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nats.jsmulti.settings.Arguments.INDIVIDUAL;
import static io.nats.jsmulti.settings.Arguments.SHARED;
import static io.nats.jsmulti.shared.Utils.uniqueEnough;

public class Context {

    // ----------------------------------------------------------------------------------------------------
    // Settings
    // ----------------------------------------------------------------------------------------------------
    public final Action action;
    public final boolean latencyFlag;
    public final String server;
    public final int reportFrequency;
    public final String subject;
    public final int messageCount;
    public final int threads;
    public final boolean connShared;
    public final long jitter;
    public final int payloadSize;
    public final int roundSize;
    public final AckPolicy ackPolicy;
    public final int ackAllFrequency;
    public final int batchSize;

    // once per context for now
    public final String queueName = "qn-" + uniqueEnough();
    public final String queueDurable = "qd-" + uniqueEnough();
    public final int maxPubRetries = 10;

    // ----------------------------------------------------------------------------------------------------
    // macros / state / vars to access through methods instead of direct
    // ----------------------------------------------------------------------------------------------------
    private final OptionsFactory _optionsFactory;
    private final int[] perThread;
    private final byte[] payload; // private and with getter in case I want to do more with payload later
    private final Map<String, AtomicLong> subscribeCounters = Collections.synchronizedMap(new HashMap<>());

    public Options getOptions() throws Exception {
        return _optionsFactory.getOptions(this);
    }

    public byte[] getPayload() {
        return payload;
    }

    private String _pullDurable;
    public String getPullDurable(int durableId) {
        // is a queue, use the same durable
        if (action.isQueue()) {
            if (_pullDurable == null) {
                _pullDurable = "qd" + uniqueEnough();
            }
            return _pullDurable;
        }

        // not a queue, each durable is unique
        return "qd" + uniqueEnough() + durableId;
    }

    public int getPubCount(int id) {
        return perThread[id-1]; // ids start at 1
    }

    public AtomicLong getSubscribeCounter(String key) {
        return subscribeCounters.computeIfAbsent(key, k -> new AtomicLong());
    }

    public String getLabel(int id) {
        return action + (connShared ? "Shared" : "Individual") + id;
    }

    // ----------------------------------------------------------------------------------------------------
    // ToString
    // ----------------------------------------------------------------------------------------------------
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
        append(sb, "action", "lf", "Yes", latencyFlag);
        append(sb, "options factory", "of", _optionsFactory.getClass().getTypeName(), true);
        append(sb, "report frequency", "rf", reportFrequency == Integer.MAX_VALUE ? "no reporting" : "" + reportFrequency, true);
        append(sb, "subject", "u", subject, true);
        append(sb, "message count", "m", messageCount, true);
        append(sb, "threads", "d", threads, true);
        append(sb, "connection strategy", "n", connShared ? SHARED : INDIVIDUAL, threads > 1);

        append(sb, "payload size", "p", payloadSize + " bytes", action.isPubAction());
        append(sb, "jitter", "j", jitter, action.isPubAction());

        append(sb, "round size", "r", roundSize, action.isPubAction() && action.isPubSync());

        append(sb, "ack policy", "kp", ackPolicy, action.isSubAction());
        append(sb, "ack all frequency", "kf", ackAllFrequency, action.isSubAction());

        append(sb, "batch size", "b", batchSize, action.isPull());

        return sb.toString();
    }

    // ----------------------------------------------------------------------------------------------------
    // Construction
    // ----------------------------------------------------------------------------------------------------
    public Context(Arguments args) {
        this(args.toArray());
    }

    public Context(String[] args) {
        if (args == null || args.length == 0) {
            exit();
        }

        Action _action = null;
        boolean _latencyFlag = false;
        String _server = Options.DEFAULT_URL;
        String _optionsFactoryClassName = null;
        int _reportFrequency = 1000;
        String _subject = "sub" + uniqueEnough();
        int _messageCount = 100_000;
        int _threads = 1;
        boolean _connShared = true;
        long _jitter = 0;
        int _payloadSize = 128;
        int _roundSize = 100;
        AckPolicy _ackPolicy = AckPolicy.Explicit;
        int _ackAllFrequency = 1;
        int _batchSize = 10;

        if (args != null && args.length > 0) {
            try {
                for (int x = 0; x < args.length; x++) {
                    String arg = args[x].trim();
                    switch (arg) {
                        case "-s":
                            _server = asString(args[++x]);
                            break;
                        case "-of":
                            _optionsFactoryClassName = asString(args[++x]);
                            break;
                        case "-a":
                            _action = Action.getInstance(asString(args[++x]));
                            if (_action == null) {
                                error("Valid action required!");
                            }
                            break;
                        case "-lf":
                            _latencyFlag = true;
                            break;
                        case "-u":
                            _subject = asString(args[++x]);
                            break;
                        case "-m":
                            _messageCount = asNumber("total messages", args[++x], -1);
                            break;
                        case "-ps":
                            _payloadSize = asNumber("payload size", args[++x], 1048576);
                            break;
                        case "-bs":
                            _batchSize = asNumber("batch size", args[++x], 256);
                            break;
                        case "-rs":
                            _roundSize = asNumber("round size", args[++x], 1000);
                            break;
                        case "-d":
                            _threads = asNumber("number of threads", args[++x], 10);
                            break;
                        case "-j":
                            _jitter = asNumber("jitter", args[++x], 10_000);
                            break;
                        case "-n":
                            _connShared = bool("connection strategy", args[++x], SHARED, INDIVIDUAL);
                            break;
                        case "-kp":
                            _ackPolicy = AckPolicy.get(asString(args[++x]).toLowerCase());
                            if (_ackPolicy == null) {
                                error("Invalid Ack Policy, must be one of [explicit, none, all]");
                            }
                            break;
                        case "-kf":
                            _ackAllFrequency = asNumber("ack frequency", args[++x], 256);
                            break;
                        case "-rf":
                            _reportFrequency = asNumber("report frequency", args[++x], -2);
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

        if (_messageCount < 1) {
            error("Message count required!");
        }

        if (_threads == 1 && _action.isQueue()) {
            error("Queue subscribing requires multiple threads!");
        }

        if (_action.isPull()) {
            // TODO IF MUST LIMIT PULL ACK POLICY
        }

        action = _action;
        latencyFlag = _latencyFlag;
        server = _server;
        reportFrequency = _reportFrequency;
        subject = _subject;
        messageCount = _messageCount;
        threads = _threads;
        connShared = _connShared;
        jitter = _jitter;
        payloadSize = _payloadSize;
        roundSize = _roundSize;
        ackPolicy = _ackPolicy;
        ackAllFrequency = _ackAllFrequency;
        batchSize = _batchSize;

        OptionsFactory ofTemp = null;
        if (_optionsFactoryClassName == null) {
            try {
                // making sure the server value is valid
                new Options.Builder().build().createURIForServer(_server);
            } catch (URISyntaxException e) {
                error("Invalid server URI: " + _server);
            }
            ofTemp = new DefaultOptionsFactory();
        }
        else {
            try {
                Class<?> c = Class.forName(_optionsFactoryClassName);
                Constructor<?> cons = c.getConstructor();
                ofTemp = (OptionsFactory) cons.newInstance();
            }
            catch (Exception e) {
                error("Error creating OptionsFactory: " + e);
            }
        }
        _optionsFactory = ofTemp;

        payload = new byte[payloadSize];

        int total = 0;
        perThread = new int[threads];
        for (int x = 0; x < threads; x++) {
            perThread[x] = messageCount / threads;
            total += perThread[x];
        }

        int ix = 0;
        while (total < messageCount) {
            perThread[ix++]++;
            total++;
        }
    }

    private static void error(String errMsg) {
        System.err.println("\nERROR: " + errMsg);
        exit();
    }

    private static void exit() {
        System.err.println(Usage.USAGE);
        System.exit(-1);
    }

    private static String normalize(String s) {
        return s.replaceAll("_", "").replaceAll(",", "").replaceAll("\\.", "");
    }

    private static String asString(String val) {
        return val.trim();
    }

    private static int asNumber(String name, String val, int upper) {
        int v = Integer.parseInt(normalize(asString(val)));
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

    private static boolean bool(String name, String val, String trueChoice, String falseChoice) {
        return choice(name, val, trueChoice, falseChoice) == 0;
    }

    private static int choice(String name, String val, String... choices) {
        String value = asString(val);
        for (int x = 0; x < choices.length; x++) {
            if (value.equalsIgnoreCase(choices[x])) {
                return x;
            }
        }

        error("Invalid choice for " + name + " [" + value + "]. Must be one of " + Arrays.toString(choices));
        return -1;
    }
}
