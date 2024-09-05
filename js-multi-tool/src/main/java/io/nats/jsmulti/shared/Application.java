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

package io.nats.jsmulti.shared;

import io.nats.jsmulti.settings.Context;

public interface Application {
    default void init(Context ctx) {
    }

    default void track(Stats stats, boolean isFinal) {
    }

    default void report(Object o) {
        System.out.println(format(o));
    }

    default void reportErr(Object o) {
        System.err.println(format(o));
    }

    default void reportEx(Exception e) {
        System.err.println(format(e.getMessage()));
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
    }

    default void usage() {
        System.err.println(Usage.USAGE);
    }

    default void exit(int status) {
        System.exit(status);
    }

    default String format(Object o) {
        return time() + " [" + threadInfo() + "] " + o.toString();
    }

    default String time() {
        String t = "" + System.currentTimeMillis();
        return t.substring(t.length() - 9);
    }

    default String threadInfo() {
        return Thread.currentThread().getName();
    }

    default OptionsFactory getOptionsFactory() {
        return new OptionsFactory() {};
    }
}
