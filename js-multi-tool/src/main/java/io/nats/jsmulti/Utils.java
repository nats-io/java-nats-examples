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

import io.nats.client.ErrorListener;
import io.nats.client.Options;

import java.time.Duration;

public abstract class Utils {
    public static final String PUB_ACTIONS = "pubsync pubasync pubcore";
    public static final String SUB_ACTIONS = "subpush subqueue subpull subpullqueue";
    public static final String ALL_ACTIONS = PUB_ACTIONS + " " + SUB_ACTIONS;
    public static final String PULL_ACTIONS = "subpull subpullqueue";
    public static final String QUEUE_ACTIONS = "subqueue subpullqueue";

    public static final String PUB_SYNC = "pubsync";
    public static final String PUB_ASYNC = "pubasync";
    public static final String PUB_CORE = "pubcore";
    public static final String SUB_PUSH = "subpush";
    public static final String SUB_QUEUE = "subqueue";
    public static final String SUB_PULL = "subpull";
    public static final String SUB_PULL_QUEUE = "subpullqueue";

    public static final String INDIVIDUAL = "individual";
    public static final String SHARED = "shared";
    public static final String ITERATE = "iterate";
    public static final String FETCH = "fetch";

    public static final String HDR_PUB_TIME = "pt";

    public static final int MISS_THRESHOLD = 10;

    public static String uniqueEnough() {
        String hex = Long.toHexString(System.currentTimeMillis()).substring(6);
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < hex.length(); x++) {
            char c = hex.charAt(x);
            if (c < 58) {
                sb.append((char)(c+55));
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { /* ignored */ }
    }

    public static Options createOptions(String server) {
        return new Options.Builder()
            .server(server)
            .connectionTimeout(Duration.ofSeconds(5))
            .pingInterval(Duration.ofSeconds(10))
            .reconnectWait(Duration.ofSeconds(1))
            .errorListener(new ErrorListener() {})
            .build();
    }
}
