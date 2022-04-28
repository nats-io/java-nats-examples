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

public enum Action {
    PUB_SYNC(       "PubSync",       true),
    PUB_ASYNC(      "PubAsync",      false),
    PUB_CORE(       "PubCore",       true),
    SUB_PUSH(       "SubPush",       true,  false),
    SUB_QUEUE(      "SubQueue",      true,  true),
    SUB_PULL(       "SubPull",       false, false),
    SUB_PULL_QUEUE( "SubPullQueue",  false, true);

    private final String label;
    private final boolean pubAction;
    private final boolean pubSync;
    private final boolean subAction;
    private final boolean push;
    private final boolean pull;
    private final boolean queue;

    Action(String label, boolean sync) {
        this.label = label;
        this.pubAction = true;
        this.pubSync = sync;
        this.subAction = false;
        this.push = false;
        this.pull = false;
        this.queue = false;
    }

    Action(String label, boolean push, boolean queue) {
        this.label = label;
        this.pubAction = false;
        this.pubSync = false;
        this.subAction = true;
        this.push = push;
        this.pull = !push;
        this.queue = queue;
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

    public boolean isPush() {
        return push;
    }

    public boolean isPull() {
        return pull;
    }

    public boolean isQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return label;
    }
}
