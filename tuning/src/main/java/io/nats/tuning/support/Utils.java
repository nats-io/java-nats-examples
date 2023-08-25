// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

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
