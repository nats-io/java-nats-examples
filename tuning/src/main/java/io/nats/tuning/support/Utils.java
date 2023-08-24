// Copyright 2023 The NATS Authors
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

package io.nats.tuning.support;

import io.nats.client.Connection;

import java.text.SimpleDateFormat;
import java.util.Date;

/*
    Code to help tune Consumer Create on startup
 */
public class Utils
{
    public static boolean waitForStatus(Connection conn, long maxWaitMs, Connection.Status statusToWaitFor) {
        long times = (maxWaitMs + 99) / 100;
        for (long x = 0; x < times; x++) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (conn.getStatus() == statusToWaitFor) {
                return true;
            }
        }
        return false;
    }

    public static void report(Object o) {
        System.out.println(stamp() + " [INFO] " + o);
    }

    public static void reportEx(Object o) {
        System.err.println(stamp() + " [ERROR] " + o);
    }

    public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static String stamp() {
        return FORMATTER.format(new Date());
    }
}
