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

import io.nats.client.NUID;

public abstract class Utils {

    public static final String HDR_PUB_TIME = "pt";

    public static String randomString() {
        return new NUID().next();
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { /* ignored */ }
    }

    public static int parseInt(String val) {
        long l = parseLong(val);
        if (l > (long)Integer.MAX_VALUE || l < (long)Integer.MIN_VALUE) {
            throw new NumberFormatException(
                "Input string outside results in a number outside of the range for an int: \"" + val + "\"");
        }
        return (int)l;
    }

    public static long parseLong(String val) {
        String vl = val
            .trim()
            .toLowerCase()
            .replaceAll("_", "")
            .replaceAll(",", "")
            .replaceAll("\\.", "");

        long factor = 1;
        int fl = 1;
        if (vl.endsWith("k")) {
            factor = 1000;
        }
        else if (vl.endsWith("ki")) {
            factor = 1024;
            fl = 2;
        }
        else if (vl.endsWith("m")) {
            factor = 1_000_000;
        }
        else if (vl.endsWith("mi")) {
            factor = 1024 * 1024;
            fl = 2;
        }
        else if (vl.endsWith("g")) {
            factor = 1_000_000_000;
        }
        else if (vl.endsWith("gi")) {
            factor = 1024 * 1024 * 1024;
            fl = 2;
        }
        if (factor > 1) {
            vl = vl.substring(0, vl.length() - fl);
        }
        return Long.parseLong(vl) * factor;
    }
}
