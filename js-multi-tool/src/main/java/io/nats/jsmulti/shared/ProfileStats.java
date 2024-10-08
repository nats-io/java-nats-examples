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

import io.nats.client.support.JsonValue;
import io.nats.client.support.JsonValueUtils;

import java.io.PrintStream;
import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nats.jsmulti.shared.Utils.makeId;

public class ProfileStats {
    public static final int VERSION = 1;

    public final int version;
    public final String id;
    public String action;
    public String contextId;

    public final long maxMemory;
    public final long allocatedMemory;
    public final long freeMemory;
    public final long heapInit;
    public final long heapUsed;
    public final long heapCommitted;
    public final long heapMax;
    public final long nonHeapInit;
    public final long nonHeapUsed;
    public final long nonHeapCommitted;
    public final long nonHeapMax;
    public int threadCount;
    public final List<String> deadThreads;
    public final List<String> liveThreads;

    public ProfileStats(String contextId, String action) {
        version = VERSION;
        id = makeId();
        deadThreads = new ArrayList<>();
        liveThreads = new ArrayList<>();

        this.action = action;
        this.contextId = contextId;

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();
        maxMemory = runtime.maxMemory();
        allocatedMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();

        MemoryUsage usage = memBean.getHeapMemoryUsage();
        heapInit = usage.getInit();
        heapUsed = usage.getUsed();
        heapCommitted = usage.getCommitted();
        heapMax = usage.getMax();

        usage = memBean.getNonHeapMemoryUsage();
        nonHeapInit = usage.getInit();
        nonHeapUsed = usage.getUsed();
        nonHeapCommitted = usage.getCommitted();
        nonHeapMax = usage.getMax();

        threadCount = threadBean.getThreadCount();
        long[] deadThreadIds = threadBean.findDeadlockedThreads();
        if (deadThreadIds == null) {
            deadThreadIds = new long[0];
        }
        for (long id : threadBean.getAllThreadIds()) {
            ThreadInfo ti = threadBean.getThreadInfo(id);
            if (ti != null) {
                String text = "<" + id + "> " + ti.getThreadName();
                if (isAlive(id, deadThreadIds)) {
                    liveThreads.add(text);
                }
                else {
                    deadThreads.add(text);
                }
            }
        }
    }

    public ProfileStats(JsonValue jv) {
        version = JsonValueUtils.readInteger(jv, "version", 0);
        id = JsonValueUtils.readString(jv, "id", null);
        action = JsonValueUtils.readString(jv, "action", null);
        contextId = JsonValueUtils.readString(jv, "contextId", null);
        maxMemory = JsonValueUtils.readLong(jv, "maxMemory", 0);
        allocatedMemory = JsonValueUtils.readLong(jv, "allocatedMemory", 0);
        freeMemory = JsonValueUtils.readLong(jv, "freeMemory", 0);
        heapInit = JsonValueUtils.readLong(jv, "heapInit", 0);
        heapUsed = JsonValueUtils.readLong(jv, "heapUsed", 0);
        heapCommitted = JsonValueUtils.readLong(jv, "heapCommitted", 0);
        heapMax = JsonValueUtils.readLong(jv, "heapMax", 0);
        nonHeapInit = JsonValueUtils.readLong(jv, "nonHeapInit", 0);
        nonHeapUsed = JsonValueUtils.readLong(jv, "nonHeapUsed", 0);
        nonHeapCommitted = JsonValueUtils.readLong(jv, "nonHeapCommitted", 0);
        nonHeapMax = JsonValueUtils.readLong(jv, "nonHeapMax", 0);
        threadCount = JsonValueUtils.readInteger(jv, "threadCount", 0);
        deadThreads = JsonValueUtils.readStringList(jv, "deadThreads");
        liveThreads = JsonValueUtils.readStringList(jv, "liveThreads");
    }

    public Map<String, JsonValue> toJsonValueMap() {
        JsonValueUtils.ArrayBuilder deadBuilder = JsonValueUtils.arrayBuilder();
        for (String s : deadThreads) {
            deadBuilder.add(s);
        }
        JsonValueUtils.ArrayBuilder liveBuilder = JsonValueUtils.arrayBuilder();
        for (String s : liveThreads) {
            liveBuilder.add(s);
        }
        return JsonValueUtils.mapBuilder()
            .put("version", version)
            .put("id", id)
            .put("action", action)
            .put("contextId", contextId)
            .put("maxMemory", maxMemory)
            .put("allocatedMemory", allocatedMemory)
            .put("freeMemory", freeMemory)
            .put("heapInit", heapInit)
            .put("heapUsed", heapUsed)
            .put("heapCommitted", heapCommitted)
            .put("heapMax", heapMax)
            .put("nonHeapInit", nonHeapInit)
            .put("nonHeapUsed", nonHeapUsed)
            .put("nonHeapCommitted", nonHeapCommitted)
            .put("nonHeapMax", nonHeapMax)
            .put("threadCount", threadCount)
            .put("deadThreads", deadBuilder.toJsonValue())
            .put("liveThreads", liveBuilder.toJsonValue())
            .toJsonValue().map;
    }

    private static boolean isAlive(long id, long[] deadThreadIds) {
        for (long dead : deadThreadIds) {
            if (dead == id) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileStats that = (ProfileStats) o;
        return version == that.version
            && maxMemory == that.maxMemory
            && allocatedMemory == that.allocatedMemory
            && freeMemory == that.freeMemory
            && heapInit == that.heapInit
            && heapUsed == that.heapUsed
            && heapCommitted == that.heapCommitted
            && heapMax == that.heapMax
            && nonHeapInit == that.nonHeapInit
            && nonHeapUsed == that.nonHeapUsed
            && nonHeapCommitted == that.nonHeapCommitted
            && nonHeapMax == that.nonHeapMax
            && threadCount == that.threadCount
            && id.equals(that.id)
            && equivalent(deadThreads, that.deadThreads)
            && equivalent(liveThreads, that.liveThreads);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Long.hashCode(version);
        result = 31 * result + Long.hashCode(maxMemory);
        result = 31 * result + Long.hashCode(allocatedMemory);
        result = 31 * result + Long.hashCode(freeMemory);
        result = 31 * result + Long.hashCode(heapInit);
        result = 31 * result + Long.hashCode(heapUsed);
        result = 31 * result + Long.hashCode(heapCommitted);
        result = 31 * result + Long.hashCode(heapMax);
        result = 31 * result + Long.hashCode(nonHeapInit);
        result = 31 * result + Long.hashCode(nonHeapUsed);
        result = 31 * result + Long.hashCode(nonHeapCommitted);
        result = 31 * result + Long.hashCode(nonHeapMax);
        result = 31 * result + threadCount;
        result = 31 * result + deadThreads.hashCode();
        result = 31 * result + liveThreads.hashCode();
        return result;
    }

    private static boolean equivalent(List<String> l1, List<String> l2)
    {
        if (l1 == null || l1.isEmpty()) {
            return l2 == null || l2.isEmpty();
        }

        if (l2 == null || l1.size() != l2.size()) {
            return false;
        }

        for (String s : l1) {
            if (!l2.contains(s)) {
                return false;
            }
        }
        return true;
    }

    public static final String REPORT_SEP_LINE = "| ------------------- | ---------- | ---------- | ---------- | ---------- | ---------- | ---------- | ---------- | ---------- | ------- | ------- |";
    public static final String REPORT_LINE_HEADER = "| %-19s |        max |   heap max |  allocated |       free |  heap used |  heap cmtd |   non used |   non cmtd |   alive |    dead |\n";
    public static final String REPORT_LINE_FORMAT = "| %-19s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %10s | %7s | %7s |\n";

    public static void report(List<ProfileStats> list) {
        for (int x = 0; x < list.size(); x++) {
            ProfileStats ps = list.get(x);
            report(ps, lineLabel(x), x == 0, false, System.out);
        }
        System.out.println(REPORT_SEP_LINE);
    }

    public static void report(ProfileStats p, String label, boolean header, boolean footer, PrintStream out) {
        if (header) {
            out.println("\n" + REPORT_SEP_LINE);
            out.printf(REPORT_LINE_HEADER, "");
            out.println(REPORT_SEP_LINE);
        }
        out.printf(REPORT_LINE_FORMAT, label,
            Stats.humanBytes(p.maxMemory),
            Stats.humanBytes(p.heapMax),
            Stats.humanBytes(p.allocatedMemory),
            Stats.humanBytes(p.freeMemory),
            Stats.humanBytes(p.heapUsed),
            Stats.humanBytes(p.heapCommitted),
            Stats.humanBytes(p.nonHeapUsed),
            Stats.humanBytes(p.nonHeapCommitted),
            p.liveThreads.size() + "/" + p.threadCount,
            p.deadThreads.size() + "/" + p.threadCount);

        if (footer) {
            out.println(REPORT_SEP_LINE);
        }
    }

    private static String lineLabel(int x) {
        return "Thread " + (x + 1);
    }
}
