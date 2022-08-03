package io.nats.jsmulti.shared;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.jsmulti.settings.Action;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static io.nats.jsmulti.shared.Utils.HDR_PUB_TIME;

public class Stats {
    private static final double NANOS_PER_MILLISECOND = 1e6;
    private static final double NANOS_PER_SECOND = 1e9;

    private static final long HUMAN_BYTES_BASE = 1024;
    private static final String[] HUMAN_BYTES_UNITS = new String[] {"b", "kb", "mb", "gb", "tb", "pb", "eb"};
    private static final String ZEROS = "000000000";

    private static final String REPORT_SEP_LINE    = "| --------------- | ----------------- | --------------- | ------------------------ | ---------------- |";
    private static final String REPORT_LINE_HEADER = "| %-15s |             count |            time |                 msgs/sec |        bytes/sec |\n";
    private static final String REPORT_LINE_FORMAT = "| %-15s | %12s msgs | %12s ms | %15s msgs/sec | %12s/sec |\n";

    private static final String LT_REPORT_SEP_LINE    = "| --------------- | ------------------------ | ---------------- | ------------------------ | ---------------- | ------------------------ | ---------------- |";
    private static final String LT_REPORT_LINE_HEADER = "| Latency Total   |                   Publish to Server Created |         Server Created to Consumer Received |                Publish to Consumer Received |";
    private static final String LT_REPORT_LINE_FORMAT = "| %-15s | %15s msgs/sec | %12s/sec | %15s msgs/sec | %12s/sec | %15s msgs/sec | %12s/sec |\n";

    private static final String LM_REPORT_SEP_LINE    = "| ----------------- | ------------------- | ------------------- | ------------------- |";
    private static final String LM_REPORT_LINE_HEADER = "| Latency Message   | Publish to Server   | Server to Consumer  | Publish to Consumer |";
    private static final String LM_REPORT_LINE_FORMAT = "| %17s |  %15s ms |  %15s ms |  %15s ms |\n";

    private double elapsed = 0;
    private double bytes = 0;
    private int messageCount = 0;

    // latency
    private double messagePubToServerTimeElapsed = 0;
    private double messageServerToReceiverElapsed = 0;
    private double messageFullElapsed = 0;
    private double messagePubToServerTimeElapsedForAverage = 0;
    private double messageServerToReceiverElapsedForAverage = 0;
    private double messageFullElapsedForAverage = 0;
    private double maxMessagePubToServerTimeElapsed = 0;
    private double maxMessageServerToReceiverElapsed = 0;
    private double maxMessageFullElapsed = 0;
    private double minMessagePubToServerTimeElapsed = Long.MAX_VALUE;
    private double minMessageServerToReceiverElapsed = Long.MAX_VALUE;
    private double minMessageFullElapsed = Long.MAX_VALUE;

    // Time keeping
    private long nanoNow;

    // Misc
    private final String hdrLabel;

    public Stats() { hdrLabel = ""; }

    public Stats(Action action) {
        hdrLabel = action.getLabel();
    }

    public void start() {
        nanoNow = System.nanoTime();
    }

    public void stop() {
        elapsed += System.nanoTime() - nanoNow;
    }

    public long elapsed() {
        return System.nanoTime() - nanoNow;
    }

    public void acceptHold(long hold) {
        if (hold > 0) {
            elapsed += hold;
        }
    }

    public void count(long bytes) {
        messageCount++;
        this.bytes += bytes;
    }

    public void stopAndCount(long bytes) {
        stop();
        count(bytes);
    }

    public void count(Message m, long mReceived) {
        messageCount++;
        this.bytes += m.getData().length;
        Headers h = m.getHeaders();
        if (h != null) {
            String hPubTime = h.getFirst(HDR_PUB_TIME);
            if (hPubTime != null) {
                long messagePubTime = Long.parseLong(hPubTime);
                long messageStampTime = m.metaData().timestamp().toInstant().toEpochMilli();

                double el = elapsedLatency(messagePubTime, messageStampTime);
                messagePubToServerTimeElapsed += el;
                maxMessagePubToServerTimeElapsed = Math.max(maxMessagePubToServerTimeElapsed, el);
                minMessagePubToServerTimeElapsed = Math.min(minMessagePubToServerTimeElapsed, el);

                el = elapsedLatency(messageStampTime, mReceived);
                messageServerToReceiverElapsed += el;
                maxMessageServerToReceiverElapsed = Math.max(maxMessageServerToReceiverElapsed, el);
                minMessageServerToReceiverElapsed = Math.min(minMessageServerToReceiverElapsed, el);

                el = elapsedLatency(messagePubTime, mReceived);
                messageFullElapsed += el;
                maxMessageFullElapsed = Math.max(maxMessageFullElapsed, el);
                minMessageFullElapsed = Math.min(minMessageFullElapsed, el);
            }
        }
    }

    private double elapsedLatency(double startMs, double stopMs) {
        double d = stopMs - startMs;
        return d < 1 ? 0 : d * NANOS_PER_MILLISECOND;
    }

    public static void report(Stats stats) {
        report(stats, "Total", true, true, System.out);
    }

    public static void report(Stats stats, String label, boolean header, boolean footer) {
        report(stats, label, header, footer, System.out);
    }

    public static void report(Stats stats, String tlabel, boolean header, boolean footer, PrintStream out) {
        double elapsed = stats.elapsed / NANOS_PER_MILLISECOND;
        double messagesPerSecond = stats.elapsed == 0 ? 0 : stats.messageCount * NANOS_PER_SECOND / stats.elapsed;
        double bytesPerSecond = NANOS_PER_SECOND * (stats.bytes) / (stats.elapsed);
        if (header) {
            out.println("\n" + REPORT_SEP_LINE);
            out.printf(REPORT_LINE_HEADER, stats.hdrLabel);
            out.println(REPORT_SEP_LINE);
        }
        out.printf(REPORT_LINE_FORMAT, tlabel,
            format(stats.messageCount),
            format3(elapsed),
            format3(messagesPerSecond),
            humanBytes(bytesPerSecond));
        if (footer) {
            out.println(REPORT_SEP_LINE);
        }
    }

    public static void ltReport(Stats stats, String label, boolean header, boolean footer, PrintStream out) {
        if (header) {
            out.println("\n" + LT_REPORT_SEP_LINE);
            out.println(LT_REPORT_LINE_HEADER);
            out.println(LT_REPORT_SEP_LINE);
        }

        double pubMper = stats.messagePubToServerTimeElapsed == 0 ? 0 : stats.messageCount * NANOS_PER_SECOND / stats.messagePubToServerTimeElapsed;
        double pubBper = stats.bytes * NANOS_PER_SECOND /(stats.messagePubToServerTimeElapsed);
        double recMper = stats.messageServerToReceiverElapsed == 0 ? 0 : stats.messageCount * NANOS_PER_SECOND / stats.messageServerToReceiverElapsed;
        double recBper = stats.bytes * NANOS_PER_SECOND /(stats.messageServerToReceiverElapsed);
        double totMper = stats.messageFullElapsed == 0 ? 0 : stats.messageCount * NANOS_PER_SECOND / stats.messageFullElapsed;
        double totBper = stats.bytes * NANOS_PER_SECOND /(stats.messageFullElapsed);
        out.printf(LT_REPORT_LINE_FORMAT, label,
            format3(pubMper),
            humanBytes(pubBper),
            format3(recMper),
            humanBytes(recBper),
            format3(totMper),
            humanBytes(totBper));
        if (footer) {
            out.println(LT_REPORT_SEP_LINE);
        }
    }

    public static void lmReport(Stats stats, String label, boolean header, boolean total, PrintStream out) {
        if (header) {
            out.println("\n" + LM_REPORT_SEP_LINE);
            out.println(LM_REPORT_LINE_HEADER);
            out.println(LM_REPORT_SEP_LINE);
        }

        double pubMper;
        double recMper;
        double totMper;
        if (total) {
            pubMper = stats.messagePubToServerTimeElapsedForAverage == 0 ? 0 : stats.messagePubToServerTimeElapsedForAverage / NANOS_PER_MILLISECOND / stats.messageCount;
            recMper = stats.messageServerToReceiverElapsedForAverage == 0 ? 0 : stats.messageServerToReceiverElapsedForAverage / NANOS_PER_MILLISECOND / stats.messageCount;
            totMper = stats.messageFullElapsedForAverage == 0 ? 0 : stats.messageFullElapsedForAverage / NANOS_PER_MILLISECOND / stats.messageCount;
        }
        else {
            pubMper = stats.messagePubToServerTimeElapsed == 0 ? 0 : stats.messagePubToServerTimeElapsed / NANOS_PER_MILLISECOND / stats.messageCount;
            recMper = stats.messageServerToReceiverElapsed == 0 ? 0 : stats.messageServerToReceiverElapsed / NANOS_PER_MILLISECOND / stats.messageCount;
            totMper = stats.messageFullElapsed == 0 ? 0 : stats.messageFullElapsed / NANOS_PER_MILLISECOND / stats.messageCount;
        }
        out.printf(LM_REPORT_LINE_FORMAT, label + " Average", format(pubMper), format(recMper), format(totMper));

        pubMper = stats.minMessagePubToServerTimeElapsed == 0 ? 0 : stats.minMessagePubToServerTimeElapsed / NANOS_PER_MILLISECOND;
        recMper = stats.minMessageServerToReceiverElapsed == 0 ? 0 : stats.minMessageServerToReceiverElapsed / NANOS_PER_MILLISECOND;
        totMper = stats.minMessageFullElapsed == 0 ? 0 : stats.minMessageFullElapsed / NANOS_PER_MILLISECOND;
        out.printf(LM_REPORT_LINE_FORMAT, "Minimum", format(pubMper), format(recMper), format(totMper));

        pubMper = stats.maxMessagePubToServerTimeElapsed == 0 ? 0 : stats.maxMessagePubToServerTimeElapsed / NANOS_PER_MILLISECOND;
        recMper = stats.maxMessageServerToReceiverElapsed == 0 ? 0 : stats.maxMessageServerToReceiverElapsed / NANOS_PER_MILLISECOND;
        totMper = stats.maxMessageFullElapsed == 0 ? 0 : stats.maxMessageFullElapsed / NANOS_PER_MILLISECOND;
        out.printf(LM_REPORT_LINE_FORMAT, "Maximum", format(pubMper), format(recMper), format(totMper));

        out.println(LM_REPORT_SEP_LINE);
    }

    public static Stats total(List<Stats> statList) {
        Stats total = new Stats();
        for (Stats stats : statList) {
            total.elapsed = Math.max(total.elapsed, stats.elapsed);
            total.messageCount += stats.messageCount;
            total.bytes += stats.bytes;

            total.messagePubToServerTimeElapsed = Math.max(total.messagePubToServerTimeElapsed, stats.messagePubToServerTimeElapsed);
            total.messageServerToReceiverElapsed = Math.max(total.messageServerToReceiverElapsed, stats.messageServerToReceiverElapsed);
            total.messageFullElapsed = Math.max(total.messageFullElapsed, stats.messageFullElapsed);

            total.messagePubToServerTimeElapsedForAverage += stats.messagePubToServerTimeElapsed;
            total.messageServerToReceiverElapsedForAverage += stats.messageServerToReceiverElapsed;
            total.messageFullElapsedForAverage += stats.messageFullElapsed;

            total.maxMessagePubToServerTimeElapsed = Math.max(total.maxMessagePubToServerTimeElapsed, stats.maxMessagePubToServerTimeElapsed);
            total.maxMessageServerToReceiverElapsed = Math.max(total.maxMessageServerToReceiverElapsed, stats.maxMessageServerToReceiverElapsed);
            total.maxMessageFullElapsed = Math.max(total.maxMessageFullElapsed, stats.maxMessageFullElapsed);

            total.minMessagePubToServerTimeElapsed = Math.min(total.minMessagePubToServerTimeElapsed, stats.minMessagePubToServerTimeElapsed);
            total.minMessageServerToReceiverElapsed = Math.min(total.minMessageServerToReceiverElapsed, stats.minMessageServerToReceiverElapsed);
            total.minMessageFullElapsed = Math.min(total.minMessageFullElapsed, stats.minMessageFullElapsed);
        }
        return total;
    }

    public static void report(List<Stats> statList) {
        report(statList, System.out);
    }

    public static void report(List<Stats> statList, PrintStream out) {
        Stats totalStats = total(statList);
        for (int x = 0; x < statList.size(); x++) {
            report(statList.get(x), "Thread " + (x+1), x == 0, false, out);
        }
        out.println(REPORT_SEP_LINE);
        report(totalStats, "Total", false, true, out);

        if (statList.get(0).messagePubToServerTimeElapsed > 0) {
            for (int x = 0; x < statList.size(); x++) {
                ltReport(statList.get(x), "Thread " + (x+1), x == 0, false, out);
            }
            out.println(LT_REPORT_SEP_LINE);
            ltReport(totalStats, "Total", false, true, out);

            for (int x = 0; x < statList.size(); x++) {
                lmReport(statList.get(x), "Thread " + (x+1), x == 0, false, out);
            }
            lmReport(totalStats, "Total", false, true, out);
        }
    }

    public static String humanBytes(double bytes) {
        if (bytes < HUMAN_BYTES_BASE) {
            return String.format("%.2f b", bytes);
        }
        int exp = (int) (Math.log(bytes) / Math.log(HUMAN_BYTES_BASE));
        try {
            return String.format("%.2f %s", bytes / Math.pow(HUMAN_BYTES_BASE, exp), HUMAN_BYTES_UNITS[exp]);
        }
        catch (Exception e) {
            return String.format("%.2f b", bytes);
        }
    }

    public static String format(Number s) {
        return NumberFormat.getNumberInstance(Locale.getDefault()).format(s);
    }

    public static String format3(Number s) {
        if (s.longValue() >= 1_000_000_000) {
            return humanBytes(s.doubleValue());
        }
        String f = format(s);
        int at = f.indexOf('.');
        if (at == 0) {
            return f + "." + ZEROS.substring(0, 3);
        }
        return (f + ZEROS).substring(0, at + 3 + 1);
    }
}
