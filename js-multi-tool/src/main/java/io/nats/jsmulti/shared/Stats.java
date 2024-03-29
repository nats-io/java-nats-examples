package io.nats.jsmulti.shared;

import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.jsmulti.settings.Action;
import io.nats.jsmulti.settings.Context;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.nats.jsmulti.shared.Utils.HDR_PUB_TIME;

public class Stats {
    private static final double MILLIS_PER_SECOND = 1000;
    private static final double NANOS_PER_MILLI = 1000000;

    private static final long HUMAN_BYTES_BASE = 1024;
    private static final String[] HUMAN_BYTES_UNITS = new String[] {"b", "kb", "mb", "gb", "tb", "pb", "eb"};
    private static final String ZEROS = "000000000";

    private static final String REPORT_SEP_LINE    = "| --------------- | ----------------- | --------------- | ------------------------ | ---------------- |";
    private static final String REPORT_LINE_HEADER = "| %-15s |             count |            time |                 msgs/sec |        bytes/sec |\n";
    private static final String REPORT_LINE_FORMAT = "| %-15s | %12s msgs | %12s ms | %15s msgs/sec | %12s/sec |\n";

    private static final String RTT_REPORT_SEP_LINE    = "| --------------- | ------------ | ------------------ | ------------------ |";
    private static final String RTT_REPORT_LINE_HEADER = "| %-15s |        count |         total time |       average time |\n";
    private static final String RTT_REPORT_LINE_FORMAT = "| %-15s | %12s |    %12s ms | %15s ms |\n";

    private static final String LT_REPORT_SEP_LINE    = "| --------------- | ------------------------ | ---------------- | ------------------------ | ---------------- | ------------------------ | ---------------- |";
    private static final String LT_REPORT_LINE_HEADER = "| Latency Total   |                   Publish to Server Created |         Server Created to Consumer Received |                Publish to Consumer Received |";
    private static final String LT_REPORT_LINE_FORMAT = "| %-15s | %15s msgs/sec | %12s/sec | %15s msgs/sec | %12s/sec | %15s msgs/sec | %12s/sec |\n";

    private static final String LM_REPORT_SEP_LINE    = "| ----------------- | ------------------- | ------------------- | ------------------- |";
    private static final String LM_REPORT_LINE_HEADER = "| Latency Message   | Publish to Server   | Server to Consumer  | Publish to Consumer |";
    private static final String LM_REPORT_LINE_FORMAT = "| %17s |  %15s ms |  %15s ms |  %15s ms |\n";

    private static final String LCSV_HEADER = "Publish Time,Server Time,Received Time,Publish to Server,Server to Consumer,Publish to Consumer\n";

    private long elapsed = 0;
    private long bytes = 0;
    private int messageCount = 0;

    // latency
    private long messagePubToServerTimeElapsed = 0;
    private long messageServerToReceiverElapsed = 0;
    private long messageFullElapsed = 0;
    private long messagePubToServerTimeElapsedForAverage = 0;
    private long messageServerToReceiverElapsedForAverage = 0;
    private long messageFullElapsedForAverage = 0;
    private long maxMessagePubToServerTimeElapsed = 0;
    private long maxMessageServerToReceiverElapsed = 0;
    private long maxMessageFullElapsed = 0;
    private long minMessagePubToServerTimeElapsed = Long.MAX_VALUE;
    private long minMessageServerToReceiverElapsed = Long.MAX_VALUE;
    private long minMessageFullElapsed = Long.MAX_VALUE;

    // Time keeping
    private long milliNow;

    // Misc
    private final Context ctx;
    private final String hdrLabel;

    private final ExecutorService countService = Executors.newSingleThreadExecutor();
    private final FileOutputStream lout;

    public Stats() {
        ctx = null;
        hdrLabel = "";
        lout = null;
    }

    public Stats(Context ctx) throws IOException {
        this.ctx = ctx;
        hdrLabel = ctx.action.getLabel();
        if (ctx.lcsv == null) {
            lout = null;
        }
        else {
            lout = new FileOutputStream(ctx.lcsv);
            lout.write(LCSV_HEADER.getBytes(StandardCharsets.US_ASCII));
        }
    }

    public void shutdown() {
        countService.shutdown();
    }

    public boolean isTerminated() {
        return countService.isTerminated();
    }

    public void start() {
        milliNow = System.currentTimeMillis();
    }

    public void stop() {
        elapsed += System.currentTimeMillis() - milliNow;
    }

    public long elapsed() {
        return System.currentTimeMillis() - milliNow;
    }

    public void manualElapsed(long mElapsed) {
        elapsed += mElapsed;
    }

    public void manualElapsed(long mElapsed, long mMessageCount) {
        elapsed += mElapsed;
        messageCount += mMessageCount;
    }

    public void manualElapsed(long mElapsed, long mMessageCount, long mBytes) {
        elapsed += mElapsed;
        messageCount += mMessageCount;
        bytes += mBytes;
    }

    public void stopAndCount(long bytes) {
        stop();
        messageCount++;
        this.bytes += bytes;
    }

    public void count(final Message m, final long mReceived) {
        messageCount++;
        this.bytes += m.getData().length;
        countService.submit(() -> countTask(m, mReceived));
    }

    private void countTask(Message m, long mReceived) {
        Headers h = m.getHeaders();
        if (h != null) {
            String hPubTime = h.getFirst(HDR_PUB_TIME);
            if (hPubTime != null) {
                long messagePubTime = Long.parseLong(hPubTime);
                long messageStampTime = m.metaData().timestamp().toInstant().toEpochMilli();

                long pToS = messageStampTime - messagePubTime;
                messagePubToServerTimeElapsed += pToS;
                maxMessagePubToServerTimeElapsed = Math.max(maxMessagePubToServerTimeElapsed, pToS);
                minMessagePubToServerTimeElapsed = Math.min(minMessagePubToServerTimeElapsed, pToS);

                long sToR = mReceived - messageStampTime;
                messageServerToReceiverElapsed += sToR;
                maxMessageServerToReceiverElapsed = Math.max(maxMessageServerToReceiverElapsed, sToR);
                minMessageServerToReceiverElapsed = Math.min(minMessageServerToReceiverElapsed, sToR);

                long full = mReceived - messagePubTime;
                messageFullElapsed += full;
                maxMessageFullElapsed = Math.max(maxMessageFullElapsed, full);
                minMessageFullElapsed = Math.min(minMessageFullElapsed, full);

                if (lout != null) {
                    try {
                        lout.write(("" + messagePubTime + "," + messageStampTime + "," + mReceived
                            + "," + pToS + "," + sToR + "," + full
                            + "\n").getBytes(StandardCharsets.US_ASCII));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static void report(Stats stats, String label, boolean header, boolean footer, PrintStream out) {
        double messagesPerSecond = stats.elapsed == 0 ? 0 : stats.messageCount * MILLIS_PER_SECOND / stats.elapsed;
        double bytesPerSecond = MILLIS_PER_SECOND * (stats.bytes) / (stats.elapsed);
        if (header) {
            out.println("\n" + REPORT_SEP_LINE);
            out.printf(REPORT_LINE_HEADER, stats.hdrLabel);
            out.println(REPORT_SEP_LINE);
        }
        out.printf(REPORT_LINE_FORMAT, label,
            format(stats.messageCount),
            format3(stats.elapsed),
            format3(messagesPerSecond),
            humanBytes(bytesPerSecond));
        if (footer) {
            out.println(REPORT_SEP_LINE);
        }
    }

    public static void rttReport(Stats stats, String tlabel, boolean header, boolean footer, PrintStream out) {
        if (header) {
            out.println("\n" + RTT_REPORT_SEP_LINE);
            out.printf(RTT_REPORT_LINE_HEADER, stats.hdrLabel);
            out.println(RTT_REPORT_SEP_LINE);
        }
        out.printf(RTT_REPORT_LINE_FORMAT, tlabel,
            format(stats.messageCount),
            format3(stats.elapsed / NANOS_PER_MILLI),
            format3(stats.elapsed / NANOS_PER_MILLI / stats.messageCount));
        if (footer) {
            out.println(RTT_REPORT_SEP_LINE);
        }
    }

    public static void ltReport(Stats stats, String label, boolean header, boolean footer, PrintStream out) {
        if (header) {
            out.println("\n" + LT_REPORT_SEP_LINE);
            out.println(LT_REPORT_LINE_HEADER);
            out.println(LT_REPORT_SEP_LINE);
        }

        double pubMper = stats.messagePubToServerTimeElapsed == 0 ? 0 : stats.messageCount * MILLIS_PER_SECOND / stats.messagePubToServerTimeElapsed;
        double pubBper = stats.bytes * MILLIS_PER_SECOND / (stats.messagePubToServerTimeElapsed);
        double recMper = stats.messageServerToReceiverElapsed == 0 ? 0 : stats.messageCount * MILLIS_PER_SECOND / stats.messageServerToReceiverElapsed;
        double recBper = stats.bytes * MILLIS_PER_SECOND / (stats.messageServerToReceiverElapsed);
        double totMper = stats.messageFullElapsed == 0 ? 0 : stats.messageCount * MILLIS_PER_SECOND / stats.messageFullElapsed;
        double totBper = stats.bytes * MILLIS_PER_SECOND / (stats.messageFullElapsed);
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
            pubMper = stats.messagePubToServerTimeElapsedForAverage == 0 ? 0 : stats.messagePubToServerTimeElapsedForAverage / MILLIS_PER_SECOND / stats.messageCount;
            recMper = stats.messageServerToReceiverElapsedForAverage == 0 ? 0 : stats.messageServerToReceiverElapsedForAverage / MILLIS_PER_SECOND / stats.messageCount;
            totMper = stats.messageFullElapsedForAverage == 0 ? 0 : stats.messageFullElapsedForAverage / MILLIS_PER_SECOND / stats.messageCount;
        }
        else {
            pubMper = stats.messagePubToServerTimeElapsed == 0 ? 0 : stats.messagePubToServerTimeElapsed / MILLIS_PER_SECOND / stats.messageCount;
            recMper = stats.messageServerToReceiverElapsed == 0 ? 0 : stats.messageServerToReceiverElapsed / MILLIS_PER_SECOND / stats.messageCount;
            totMper = stats.messageFullElapsed == 0 ? 0 : stats.messageFullElapsed / MILLIS_PER_SECOND / stats.messageCount;
        }
        out.printf(LM_REPORT_LINE_FORMAT, label + " Average", format(pubMper), format(recMper), format(totMper));

        pubMper = stats.minMessagePubToServerTimeElapsed == 0 ? 0 : stats.minMessagePubToServerTimeElapsed / MILLIS_PER_SECOND;
        recMper = stats.minMessageServerToReceiverElapsed == 0 ? 0 : stats.minMessageServerToReceiverElapsed / MILLIS_PER_SECOND;
        totMper = stats.minMessageFullElapsed == 0 ? 0 : stats.minMessageFullElapsed / MILLIS_PER_SECOND;
        out.printf(LM_REPORT_LINE_FORMAT, "Minimum", format(pubMper), format(recMper), format(totMper));

        pubMper = stats.maxMessagePubToServerTimeElapsed == 0 ? 0 : stats.maxMessagePubToServerTimeElapsed / MILLIS_PER_SECOND;
        recMper = stats.maxMessageServerToReceiverElapsed == 0 ? 0 : stats.maxMessageServerToReceiverElapsed / MILLIS_PER_SECOND;
        totMper = stats.maxMessageFullElapsed == 0 ? 0 : stats.maxMessageFullElapsed / MILLIS_PER_SECOND;
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

        Context ctx = statList.get(0).ctx;
        if (ctx != null && ctx.action == Action.RTT) {
            for (int x = 0; x < statList.size(); x++) {
                rttReport(statList.get(x), "Thread " + (x+1), x == 0, false, out);
            }
            out.println(RTT_REPORT_SEP_LINE);
            rttReport(totalStats, "Total", false, true, out);
            return;
        }

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

    public static String format3(Number n) {
        if (n.longValue() >= 1_000_000_000) {
            return humanBytes(n.doubleValue());
        }
        String f = format(n);
        int at = f.indexOf('.');
        if (at == -1) {
            return f;
        }
        if (at == 0) {
            return f + "." + ZEROS.substring(0, 3);
        }
        return (f + ZEROS).substring(0, at + 3 + 1);
    }
}
