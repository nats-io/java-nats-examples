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

public enum Action {
    PUB_SYNC(            "PubSync",             true,  true,  false, false, false, false),
    PUB_ASYNC(           "PubAsync",            true,  false, false, false, false, false),
    PUB_CORE(            "PubCore",             true,  true,  false, false, false, false),
    PUB(                 "Pub",                 true,  true,  true,  false, false, false),

    REQUEST(             "Request",             true,  true,  true,  false, false, false),
    // REQUEST_ASYNC(       "RequestAsync",        true,  false, true,  false, false),
    REPLY(               "Reply",               false, false, true,  true,  false, false),

    SUB_CORE(            "SubCore",             false, false, true,  true,  false, false),
    SUB_CORE_QUEUE(      "SubCoreQueue",        false, false, true,  true,  true, false),

    SUB_PUSH(            "SubPush",             false, false, false, true,  false, false),
    SUB_QUEUE(           "SubQueue",            false, false, false, true,  true, false),
    SUB_PULL(            "SubPull",             false, false, false, false, false, false),
    SUB_PULL_READ(       "SubPullRead",         false, false, false, false, false, false), // read is manual continuous sync pull (pre-simplification)
    SUB_PULL_QUEUE(      "SubPullQueue",        false, false, false, false, true, false),
    SUB_PULL_READ_QUEUE( "SubPullReadQueue",    false, false, false, false, false, false), // read is manual continuous sync pull (pre-simplification)

    // simplification
    SUB_FETCH(           "SubFetch",            false, false, false, false, false, true),
    SUB_ITERATE(         "SubIterate",          false, false, false, false, false, true),
    // SUB_CONSUME(         "SubConsume",          false, false, false, false, false, true),
    SUB_FETCH_QUEUE(     "SubFetchQueue",       false, false, false, false, true,  true),
    SUB_ITERATE_QUEUE(   "SubIterateQueue",     false, false, false, false, true,  true),
    // SUB_CONSUME_QUEUE(   "SubConsumeQueue",     false, false, false, false, true,  true),

    RTT(                 "RTT",                 true,  true,  true,  false, false, false),
    CUSTOM(              "CUSTOM",              false, false, false, false, false, false);

    private final String label;
    private final boolean pubAction;
    private final boolean pubSync;
    private final boolean subAction;
    private final boolean regularCore;
    private final boolean push;
    private final boolean pull;
    private final boolean queue;
    private final boolean requiresStreamName;

    Action(String label, boolean pub, boolean pubSync, boolean regularCore, boolean push, boolean queue, boolean requiresStreamName) {
        this.label = label;
        this.pubAction = pub;
        this.pubSync = pubSync;
        this.subAction = !pub;
        this.regularCore = regularCore;
        this.push = push;
        this.pull = !regularCore && !push;
        this.queue = queue;
        this.requiresStreamName = requiresStreamName;
    }

    public static Action getInstance(String text) {
        for (Action a : Action.values()) {
            if (a.label.equalsIgnoreCase(text)) {
                return a;
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPubAction() {
        return pubAction;
    }

    public boolean isPubSync() {
        return pubAction && pubSync;
    }

    public boolean isPubAsync() {
        return pubAction && !pubSync;
    }

    public boolean isSubAction() {
        return subAction;
    }

    public boolean isRegularCore() {
        return regularCore;
    }

    public boolean isPush() {
        return push;
    }

    public boolean isPull() {
        return pull;
    }

    public boolean isQueue() {
        return queue;
    }

    public boolean requiresStreamName() {
        return requiresStreamName;
    }

    @Override
    public String toString() {
        return label;
    }
}
