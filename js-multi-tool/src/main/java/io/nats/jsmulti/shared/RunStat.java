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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nats.jsmulti.shared.Utils.makeId;

public class RunStat {
    public final String id;
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
    public final int threadCount;
    public final List<String> deadThreads;
    public final List<String> liveThreads;

    public RunStat() {
        id = makeId();

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();
        maxMemory = runtime.maxMemory();
        allocatedMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();

        System.out.println("Mem");
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

        deadThreads = new ArrayList<>();
        liveThreads = new ArrayList<>();
        threadCount = threadBean.getThreadCount();
        long[] deadThreadIds = threadBean.findDeadlockedThreads();
        if (deadThreadIds == null) {
            deadThreadIds = new long[0];
        }
        for (long id : threadBean.getAllThreadIds()) {
            String text = "<" + id + "> " + threadBean.getThreadInfo(id).getThreadName();
            if (isAlive(id, deadThreadIds)) {
                liveThreads.add(text);
            }
            else {
                deadThreads.add(text);
            }
        }
    }

    public RunStat(JsonValue jv) {
        id = JsonValueUtils.readString(jv, "id", null);
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
            .put("id", id)
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

        RunStat runStat = (RunStat) o;
        return maxMemory == runStat.maxMemory
            && allocatedMemory == runStat.allocatedMemory
            && freeMemory == runStat.freeMemory
            && heapInit == runStat.heapInit
            && heapUsed == runStat.heapUsed
            && heapCommitted == runStat.heapCommitted
            && heapMax == runStat.heapMax
            && nonHeapInit == runStat.nonHeapInit
            && nonHeapUsed == runStat.nonHeapUsed
            && nonHeapCommitted == runStat.nonHeapCommitted
            && nonHeapMax == runStat.nonHeapMax
            && threadCount == runStat.threadCount
            && id.equals(runStat.id)
            && equivalent(deadThreads, runStat.deadThreads)
            && equivalent(liveThreads, runStat.liveThreads);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
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
}
