// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

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
                    else if (conAndSub.createTime[x] != Long.MIN_VALUE) {
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

        String tl = settings.timeLabel();
//        section("General");
//        value("Stream Name", settings.streamName);
//        value("Subject Prefix", settings.subjectGenerator.getSubjectPrefix());
//        value("Storage Type", settings.storageType);
//        value("Replicas", settings.replicas);
//        value("Timeout " + tl, settings.timeoutMs);
//        value("Inactive Threshold " + tl, settings.inactiveThresholdMs);
//        value("Before Create Delay " + tl, settings.beforeCreateDelayMs);
//        value("Before Sub Delay " + tl, settings.beforeSubDelayMs);
//        value("Publish Instances", settings.publishInstances);
//        value("Payload Size", settings.payloadSize);
//        value("Report Frequency", settings.reportFrequency);

        section("Settings");
        value("App Strategy", settings.appStrategy.name().replace("_", " "));
        value("Sub Strategy", settings.subStrategy.name().replace("_", " "));
        value("App Instances", settings.appInstances);
        value("Threads Per App", settings.threadsPerApp);
        value("Consumers Per App", settings.consumersPerApp);

//        section("Time");
//        value("Elapsed " + tl, settings.time(time));

        section("Create Consumer");
        if (forMedianCons.isEmpty()) {
            value("Count");
            value("Finished");
            value("Did Not Finish");
            value("Longest " + tl);
            value("Average " + tl);
            value("Median " + tl);
        }
        else {
            value("Count", finishedCons + dnfCons);
            value("Finished", finishedCons);
            value("Did Not Finish", dnfCons);
            value("Longest " + tl, settings.time(longestCon));
            value("Average " + tl, settings.time(totalCons / finishedCons));
            value("Median " + tl, settings.time(median(forMedianCons)));
        }

        section("Subscribe");
        if (forMedianSubs.isEmpty()) {
            value("Count");
            value("Finished");
            value("Did Not Finish");
            value("Longest " + tl);
            value("Average " + tl);
            value("Median " + tl);
        }
        else {
            value("Count", finishedSubs + dnfSubs);
            value("Finished", finishedSubs);
            value("Did Not Finish", dnfSubs);
            value("Longest " + tl, settings.time(longestSub));
            value("Average " + tl, settings.time(totalSub / finishedSubs));
            value("Median " + tl, settings.time(median(forMedianSubs)));
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

    void value(String description) {
        value(description, "");
    }
}
