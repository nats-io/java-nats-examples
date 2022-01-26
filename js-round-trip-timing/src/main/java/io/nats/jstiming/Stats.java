// Copyright 2021 The NATS Authors
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

package io.nats.jstiming;

import java.io.IOException;
import java.io.OutputStream;

import static java.lang.System.lineSeparator;

public class Stats {

    static final String HEADER = ",Pub-Server(ms),Server-Consumer(ms),Consumer-Ack(ms),Pub-Consumer(ms),Pub-Ack(ms)" + lineSeparator();

    long elapsedMillisBetweenPublishAndServer;
    long elapsedMillisBetweenServerAndReceived;
    long elapsedMillisBetweenReceivedAndAfterAck;
    long elapsedMillisBetweenPublishAndReceived;
    long elapsedMillisBetweenPublishAndAfterAck;

    long elapsedMillisBetweenPublishAndServerMin = Long.MAX_VALUE;
    long elapsedMillisBetweenServerAndReceivedMin = Long.MAX_VALUE;
    long elapsedMillisBetweenReceivedAndAfterAckMin = Long.MAX_VALUE;
    long elapsedMillisBetweenPublishAndReceivedMin = Long.MAX_VALUE;
    long elapsedMillisBetweenPublishAndAfterAckMin = Long.MAX_VALUE;

    long elapsedMillisBetweenPublishAndServerMax = Long.MIN_VALUE;
    long elapsedMillisBetweenServerAndReceivedMax = Long.MIN_VALUE;
    long elapsedMillisBetweenReceivedAndAfterAckMax = Long.MIN_VALUE;
    long elapsedMillisBetweenPublishAndReceivedMax = Long.MIN_VALUE;
    long elapsedMillisBetweenPublishAndAfterAckMax = Long.MIN_VALUE;

    int totalMessages;
    long totalBytes;

    int payloadSize;
    int messageCount;

    public Stats(int payloadSize, int messageCount) {
        this.payloadSize = payloadSize;
        this.messageCount = messageCount;
    }

    public void update(Result r) {
//        System.out.println(r);
        ++totalMessages;
        totalBytes += payloadSize;

        long xElapsedMillisBetweenPublishAndServer = r.elapsedMillisBetweenPublishAndServer();
        long xElapsedMillisBetweenServerAndReceived = r.elapsedMillisBetweenServerAndReceived();
        long xElapsedMillisBetweenReceivedAndAfterAck = r.elapsedMillisBetweenReceivedAndAfterAck();
        long xElapsedMillisBetweenPublishAndReceived = r.elapsedMillisBetweenPublishAndReceived();
        long xElapsedMillisBetweenPublishAndAfterAck = r.elapsedMillisBetweenPublishAndAfterAck();

        elapsedMillisBetweenPublishAndServer += xElapsedMillisBetweenPublishAndServer;
        elapsedMillisBetweenServerAndReceived += xElapsedMillisBetweenServerAndReceived;
        elapsedMillisBetweenReceivedAndAfterAck += xElapsedMillisBetweenReceivedAndAfterAck;
        elapsedMillisBetweenPublishAndReceived += xElapsedMillisBetweenPublishAndReceived;
        elapsedMillisBetweenPublishAndAfterAck += xElapsedMillisBetweenPublishAndAfterAck;

        // min
        elapsedMillisBetweenPublishAndServerMin = Math.min(elapsedMillisBetweenPublishAndServerMin, xElapsedMillisBetweenPublishAndServer);
        elapsedMillisBetweenServerAndReceivedMin = Math.min(elapsedMillisBetweenServerAndReceivedMin, xElapsedMillisBetweenServerAndReceived);
        elapsedMillisBetweenReceivedAndAfterAckMin = Math.min(elapsedMillisBetweenReceivedAndAfterAckMin, xElapsedMillisBetweenReceivedAndAfterAck);
        elapsedMillisBetweenPublishAndReceivedMin = Math.min(elapsedMillisBetweenPublishAndReceivedMin, xElapsedMillisBetweenPublishAndReceived);
        elapsedMillisBetweenPublishAndAfterAckMin = Math.min(elapsedMillisBetweenPublishAndAfterAckMin, xElapsedMillisBetweenPublishAndAfterAck);

        // max
        elapsedMillisBetweenPublishAndServerMax = Math.max(elapsedMillisBetweenPublishAndServerMax, xElapsedMillisBetweenPublishAndServer);
        elapsedMillisBetweenServerAndReceivedMax = Math.max(elapsedMillisBetweenServerAndReceivedMax, xElapsedMillisBetweenServerAndReceived);
        elapsedMillisBetweenReceivedAndAfterAckMax = Math.max(elapsedMillisBetweenReceivedAndAfterAckMax, xElapsedMillisBetweenReceivedAndAfterAck);
        elapsedMillisBetweenPublishAndReceivedMax = Math.max(elapsedMillisBetweenPublishAndReceivedMax, xElapsedMillisBetweenPublishAndReceived);
        elapsedMillisBetweenPublishAndAfterAckMax = Math.max(elapsedMillisBetweenPublishAndAfterAckMax, xElapsedMillisBetweenPublishAndAfterAck);
    }

    public Stats update(Stats s) {
        totalMessages += s.totalMessages;
        totalBytes += s.totalBytes;

        elapsedMillisBetweenPublishAndServer += s.elapsedMillisBetweenPublishAndServer;
        elapsedMillisBetweenServerAndReceived += s.elapsedMillisBetweenServerAndReceived;
        elapsedMillisBetweenReceivedAndAfterAck += s.elapsedMillisBetweenReceivedAndAfterAck;
        elapsedMillisBetweenPublishAndReceived += s.elapsedMillisBetweenPublishAndReceived;
        elapsedMillisBetweenPublishAndAfterAck += s.elapsedMillisBetweenPublishAndAfterAck;

        // min
        elapsedMillisBetweenPublishAndServerMin = Math.min(elapsedMillisBetweenPublishAndServerMin, s.elapsedMillisBetweenPublishAndServerMin);
        elapsedMillisBetweenServerAndReceivedMin = Math.min(elapsedMillisBetweenServerAndReceivedMin, s.elapsedMillisBetweenServerAndReceivedMin);
        elapsedMillisBetweenReceivedAndAfterAckMin = Math.min(elapsedMillisBetweenReceivedAndAfterAckMin, s.elapsedMillisBetweenReceivedAndAfterAckMin);
        elapsedMillisBetweenPublishAndReceivedMin = Math.min(elapsedMillisBetweenPublishAndReceivedMin, s.elapsedMillisBetweenPublishAndReceivedMin);
        elapsedMillisBetweenPublishAndAfterAckMin = Math.min(elapsedMillisBetweenPublishAndAfterAckMin, s.elapsedMillisBetweenPublishAndAfterAckMin);

        // max
        elapsedMillisBetweenPublishAndServerMax = Math.max(elapsedMillisBetweenPublishAndServerMax, s.elapsedMillisBetweenPublishAndServerMax);
        elapsedMillisBetweenServerAndReceivedMax = Math.max(elapsedMillisBetweenServerAndReceivedMax, s.elapsedMillisBetweenServerAndReceivedMax);
        elapsedMillisBetweenReceivedAndAfterAckMax = Math.max(elapsedMillisBetweenReceivedAndAfterAckMax, s.elapsedMillisBetweenReceivedAndAfterAckMax);
        elapsedMillisBetweenPublishAndReceivedMax = Math.max(elapsedMillisBetweenPublishAndReceivedMax, s.elapsedMillisBetweenPublishAndReceivedMax);
        elapsedMillisBetweenPublishAndAfterAckMax = Math.max(elapsedMillisBetweenPublishAndAfterAckMax, s.elapsedMillisBetweenPublishAndAfterAckMax);

        return this;
    }

    private void column(OutputStream out, long v) throws IOException {
        String text = "," + v;
        out.write(text.getBytes());
    }

    private void column(OutputStream out, double v) throws IOException {
        String text = "," + String.format("%6.3f", v).trim();
        out.write(text.getBytes());
    }

    public void writeTo(OutputStream... outs) throws Exception {
        for (OutputStream out : outs) {
            _writeTo(out);
        }
    }

    public static void writeHeader(OutputStream out, int totalMessages, int payloadSize) throws IOException {
        String text = lineSeparator() + (payloadSize == -1 ? "All" : totalMessages + " x " + payloadSize) + HEADER;
        out.write(text.getBytes());
    }

    private void _writeTo(OutputStream out) throws IOException {
        double dTotalMessages = totalMessages;

        writeHeader(out, totalMessages, payloadSize);
        out.write("Total".getBytes());
        column(out, elapsedMillisBetweenPublishAndServer);
        column(out, elapsedMillisBetweenServerAndReceived);
        column(out, elapsedMillisBetweenReceivedAndAfterAck);
        column(out, elapsedMillisBetweenPublishAndReceived);
        column(out, elapsedMillisBetweenPublishAndAfterAck);
//        column(out, nanosBetweenPublishAndReceived);
//        column(out, nanosBetweenPublishAndAfterAck);
//        column(out, nanosBetweenReceivedAndAfterAck);
        out.write(lineSeparator().getBytes());

        out.write("Average".getBytes());
        column(out, elapsedMillisBetweenPublishAndServer / dTotalMessages);
        column(out, elapsedMillisBetweenServerAndReceived / dTotalMessages);
        column(out, elapsedMillisBetweenReceivedAndAfterAck / dTotalMessages);
        column(out, elapsedMillisBetweenPublishAndReceived / dTotalMessages);
        column(out, elapsedMillisBetweenPublishAndAfterAck / dTotalMessages);
//        column(out, nanosBetweenPublishAndReceived / dTotalMessages);
//        column(out, nanosBetweenPublishAndAfterAck / dTotalMessages);
//        column(out, nanosBetweenReceivedAndAfterAck / dTotalMessages);
        out.write(lineSeparator().getBytes());

        out.write("Min".getBytes());
        column(out, elapsedMillisBetweenPublishAndServerMin);
        column(out, elapsedMillisBetweenServerAndReceivedMin);
        column(out, elapsedMillisBetweenReceivedAndAfterAckMin);
        column(out, elapsedMillisBetweenPublishAndReceivedMin);
        column(out, elapsedMillisBetweenPublishAndAfterAckMin);
//        column(out, nanosBetweenPublishAndReceivedMin);
//        column(out, nanosBetweenPublishAndAfterAckMin);
//        column(out, nanosBetweenReceivedAndAfterAckMin);
        out.write(lineSeparator().getBytes());

        out.write("Max".getBytes());
        column(out, elapsedMillisBetweenPublishAndServerMax);
        column(out, elapsedMillisBetweenServerAndReceivedMax);
        column(out, elapsedMillisBetweenReceivedAndAfterAckMax);
        column(out, elapsedMillisBetweenPublishAndReceivedMax);
        column(out, elapsedMillisBetweenPublishAndAfterAckMax);
//        column(out, nanosBetweenPublishAndReceivedMax);
//        column(out, nanosBetweenPublishAndAfterAckMax);
//        column(out, nanosBetweenReceivedAndAfterAckMax);
        out.write(lineSeparator().getBytes());

        out.write("bytes/ms".getBytes());
        column(out, bytesPerMillis(elapsedMillisBetweenPublishAndServer));
        column(out, bytesPerMillis(elapsedMillisBetweenServerAndReceived));
        out.write(',');
        column(out, bytesPerMillis(elapsedMillisBetweenPublishAndReceived));
        out.write(lineSeparator().getBytes());

        out.write("bytes/sec".getBytes());
        column(out, bytesPerSecond(elapsedMillisBetweenPublishAndServer));
        column(out, bytesPerSecond(elapsedMillisBetweenServerAndReceived));
        out.write(',');
        column(out, bytesPerSecond(elapsedMillisBetweenPublishAndReceived));
        out.write(lineSeparator().getBytes());

        out.write("kb/sec".getBytes());
        column(out, kbPerSecond(elapsedMillisBetweenPublishAndServer));
        column(out, kbPerSecond(elapsedMillisBetweenServerAndReceived));
        out.write(',');
        column(out, kbPerSecond(elapsedMillisBetweenPublishAndReceived));
        out.write(lineSeparator().getBytes());

        out.write("message/sec".getBytes());
        column(out, messagesPerSecond(elapsedMillisBetweenPublishAndServer));
        column(out, messagesPerSecond(elapsedMillisBetweenServerAndReceived));
        out.write(',');
        column(out, messagesPerSecond(elapsedMillisBetweenPublishAndReceived));
        out.write(lineSeparator().getBytes());
    }

    private double bytesPerMillis(double elapsedMillis) {
        return (double)totalBytes / elapsedMillis;
    }

    private double bytesPerSecond(double elapsedMillis) {
        return (double)totalBytes * 1000d / elapsedMillis;
    }

    private double kbPerSecond(double elapsedMillis) {
        return (double)totalBytes * 1000d / 1024d / elapsedMillis;
    }

    private double messagesPerSecond(double elapsedMillis) {
        return (double)totalMessages * 1000d / elapsedMillis;
    }
}
