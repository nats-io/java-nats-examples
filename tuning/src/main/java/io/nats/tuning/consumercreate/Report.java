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

package io.nats.tuning.consumercreate;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Report {
    public final String title;
    public final List<String> sections;
    public final List<String> descriptions;
    public final List<String> values;
    public int descriptionWidth;

    public static void writeTextReport(List<Report> reports, String fn) throws Exception {
        try (PrintStream ps = new PrintStream(fn)) {
            for (Report r : reports) {
                r.print(ps);
            }
        }
    }

    public static void writeCsv(List<Report> reports, String fn) throws Exception {
        try (PrintStream ps = new PrintStream(fn)) {
            Report r0 = reports.get(0);
            int rows = r0.rows();
            ps.print(",");
            for (Report r : reports) {
                ps.print(",");
                ps.print(r.title);
            }
            ps.println();

            for (int row = 0; row < rows; row++) {
                String text = r0.sections.get(row);
                if (text == null) {
                    text = r0.descriptions.get(row);
                    if (!text.equals("Servers")) {
                        ps.print(",");
                        ps.print(text);
                        for (Report r : reports) {
                            ps.print(",");
                            ps.print(r.values.get(row));
                        }
                        ps.println();
                    }
                }
                else {
                    ps.println(text);
                }
            }
        }
    }

    public Report(String title, Settings settings, AppSimulator[] apps, long time) {
        this.title = title;
        sections = new ArrayList<>();
        descriptions = new ArrayList<>();
        values = new ArrayList<>();

        long totalCons = 0;
        long longestCon = 0;
        long totalSub = 0;
        long longestSub = 0;
        int finishedCons = 0;
        int finishedSubs = 0;
        int dnfCons = 0;
        int dnfSubs = 0;
        List<Long> forMedianCons = new ArrayList<>();
        List<Long> forMedianSubs = new ArrayList<>();
        for (AppSimulator app : apps) {
            for (ConsumerAndSubscriber conAndSub : app.conAndSubs) {
                for (int x = 0; x < conAndSub.createTime.length; x++) {
                    if (conAndSub.createTime[x] == -1) {
                        dnfCons++;
                    }
                    else {
                        finishedCons++;
                        totalCons += conAndSub.createTime[x];
                        longestCon = Math.max(longestCon, conAndSub.createTime[x]);
                        forMedianCons.add(conAndSub.createTime[x]);
                    }
                    if (conAndSub.subscribeTime[x] == -1) {
                        dnfSubs++;
                    }
                    else if (conAndSub.subscribeTime[x] != Long.MIN_VALUE) {
                        finishedSubs++;
                        totalSub += conAndSub.subscribeTime[x];
                        longestSub = Math.max(longestSub, conAndSub.subscribeTime[x]);
                        forMedianSubs.add(conAndSub.subscribeTime[x]);
                    }
                }
            }
        }

        long medianCons = median(forMedianCons) / 1_000_000;

        section("Settings");
        value("Stream Name", settings.streamName);
        value("Subject Prefix", settings.subjectGenerator.getPrefix());
        value("Storage Type", settings.storageType);
        value("Replicas", settings.replicas);
        value("Timeout ms", settings.timeoutMs);
        value("App Instances", settings.appInstances);
        value("Threads Per App", settings.threadsPerApp);
        value("Consumers Per App", settings.consumersPerApp);
        value("Inactive Threshold ms", settings.inactiveThresholdMs);
        value("Before Create Delay ms", settings.beforeCreateDelayMs);
        value("Sub Behavior", settings.subBehavior.name().replace("_", " "));
        value("Before Sub Delay ms", settings.beforeSubDelayMs);
        value("Publish Instances", settings.publishInstances);
        value("Payload Size", settings.payloadSize);
        value("Report Frequency", settings.reportFrequency);

        section("Time");
        value("Elapsed ms", time / 1_000_000);

        section("Create Consumer");
        value("Count", finishedCons + dnfCons);
        value("Finished", finishedCons);
        value("Did Not Finish", dnfCons);
        value("Longest ms", (longestCon / 1_000_000));
        value("Average ms", ((totalCons / finishedCons) / 1_000_000));
        value("Median ms", medianCons);

        section("Subscribe");
        if (forMedianSubs.isEmpty()) {
            noValue("Count");
            noValue("Finished");
            noValue("Did Not Finish");
            noValue("Longest ms");
            noValue("Average ms");
            noValue("Median ms");
        }
        else {
            value("Count", finishedSubs + dnfSubs);
            value("Finished", finishedSubs);
            value("Did Not Finish", dnfSubs);
            value("Longest ms", longestSub / 1_000_000);
            value("Average ms", (totalSub / finishedSubs) / 1_000_000);
            value("Median ms", median(forMedianSubs) / 1_000_000);
        }
    }

    public int rows() {
        return sections.size();
    }

    public void print(PrintStream out) {
        StringBuilder pad = new StringBuilder();
        for (int p = 0; p < descriptionWidth; p++) {
            pad.append(" ");
        }

        out.println(title);
        for (int x = 0; x < values.size(); x++) {
            String text = sections.get(x);
            if (text == null) {
                // value line
                String value = values.get(x);
                if (value != null) {
                    out.print("    ");
                    out.print((descriptions.get(x) + pad).substring(0, descriptionWidth));
                    out.print(" : ");
                    out.println(value);
                }
            }
            else {
                // section line
                out.println(text);
            }
        }
        out.println();
    }

    private static long median(List<Long> list) {
        Collections.sort(list);
        if (list.size() % 2 == 0) {
            return list.get(list.size() / 2 - 1);
        }
        int ix = list.size() / 2;
        return (list.get(ix) + list.get(ix + 1)) / 2;
    }

    void section(String section) {
        this.sections.add(section);
        descriptions.add("");
        values.add("");
    }

    void value(String description, Object value) {
        this.sections.add(null);
        descriptions.add(description);
        values.add(value == null ? null : value.toString());
        descriptionWidth = Math.max(descriptionWidth, description.length());
    }

    void noValue(String description) {
        value(description, "");
    }
}
