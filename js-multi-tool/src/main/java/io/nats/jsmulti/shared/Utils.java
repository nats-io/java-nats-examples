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

package io.nats.jsmulti.shared;

import java.util.UUID;

public abstract class Utils {

    public static final String HDR_PUB_TIME = "pt";

    public static String uniqueEnough() {
        return UUID.randomUUID().toString().substring(24);
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { /* ignored */ }
    }

    public static void log(Object s) {
        System.out.println(format(s));
    }

    public static void logEx(Exception e) {
        System.err.println(format(e.getMessage()));
        e.printStackTrace();
    }

    public static String format(Object s) {
        return System.currentTimeMillis() + " [" + Thread.currentThread().getName() + "] " + s.toString();
    }
}
