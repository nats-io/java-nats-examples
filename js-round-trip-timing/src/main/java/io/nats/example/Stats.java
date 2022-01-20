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

package io.nats.example;

import java.io.IOException;
import java.io.OutputStream;

import static java.lang.System.lineSeparator;

public class Stats {

//    static final String HEADER = ",Pub-Server(ms),Server-Consumer(ms),Consumer-Ack(ms),Pub-Consumer(ms),Pub-Ack(ms),Pub-Consumer(nano),Consumer-Ack(nano),Pub-Ack(nano)" + lineSeparator();
    static final String HEADER = ",Pub-Server(ms),Server-Consumer(ms),Consumer-Ack(ms),Pub-Consumer(ms),Pub-Ack(ms)" + lineSeparator();

    long elapsedBetweenPublishAndServer;
    long elapsedBetweenServerAndReceived;
    long elapsedBetweenReceivedAndAfterAck;
    long elapsedBetweenPublishAndReceived;
    long elapsedBetweenPublishAndAfterAck;
    long nanosBetweenPublishAndReceived;
    long nanosBetweenPublishAndAfterAck;
    long nanosBetweenReceivedAndAfterAck;

    long elapsedBetweenPublishAndServerMin = Long.MAX_VALUE;
    long elapsedBetweenServerAndReceivedMin = Long.MAX_VALUE;
    long elapsedBetweenReceivedAndAfterAckMin = Long.MAX_VALUE;
    long elapsedBetweenPublishAndReceivedMin = Long.MAX_VALUE;
    long elapsedBetweenPublishAndAfterAckMin = Long.MAX_VALUE;
    long nanosBetweenPublishAndReceivedMin = Long.MAX_VALUE;
    long nanosBetweenPublishAndAfterAckMin = Long.MAX_VALUE;
    long nanosBetweenReceivedAndAfterAckMin = Long.MAX_VALUE;

    long elapsedBetweenPublishAndServerMax = Long.MIN_VALUE;
    long elapsedBetweenServerAndReceivedMax = Long.MIN_VALUE;
    long elapsedBetweenReceivedAndAfterAckMax = Long.MIN_VALUE;
    long elapsedBetweenPublishAndReceivedMax = Long.MIN_VALUE;
    long elapsedBetweenPublishAndAfterAckMax = Long.MIN_VALUE;
    long nanosBetweenPublishAndReceivedMax = Long.MIN_VALUE;
    long nanosBetweenPublishAndAfterAckMax = Long.MIN_VALUE;
    long nanosBetweenReceivedAndAfterAckMax = Long.MIN_VALUE;

    int totalMessages;
    long totalBytes;

    int payloadSize;
    int messageCount;

    public Stats(int payloadSize, int messageCount) {
        this.payloadSize = payloadSize;
        this.messageCount = messageCount;
    }

    public void update(Result r) {
        ++totalMessages;
        totalBytes += payloadSize;

        elapsedBetweenPublishAndServer += r.elapsedBetweenPublishAndServer();
        elapsedBetweenServerAndReceived += r.elapsedBetweenServerAndReceived();
        elapsedBetweenReceivedAndAfterAck += r.elapsedBetweenReceivedAndAfterAck();
        elapsedBetweenPublishAndReceived += r.elapsedBetweenPublishAndReceived();
        elapsedBetweenPublishAndAfterAck += r.elapsedBetweenPublishAndAfterAck();
        nanosBetweenPublishAndReceived += r.nanosBetweenPublishAndReceived();
        nanosBetweenReceivedAndAfterAck += r.nanosBetweenReceivedAndAfterAck();
        nanosBetweenPublishAndAfterAck += r.nanosBetweenPublishAndAfterAck();

        // min
        elapsedBetweenPublishAndServerMin = Math.min(elapsedBetweenPublishAndServerMin, r.elapsedBetweenPublishAndServer());
        elapsedBetweenServerAndReceivedMin = Math.min(elapsedBetweenServerAndReceivedMin, r.elapsedBetweenServerAndReceived());
        elapsedBetweenReceivedAndAfterAckMin = Math.min(elapsedBetweenReceivedAndAfterAckMin, r.elapsedBetweenReceivedAndAfterAck());
        elapsedBetweenPublishAndReceivedMin = Math.min(elapsedBetweenPublishAndReceivedMin, r.elapsedBetweenPublishAndReceived());
        elapsedBetweenPublishAndAfterAckMin = Math.min(elapsedBetweenPublishAndAfterAckMin, r.elapsedBetweenPublishAndAfterAck());
        nanosBetweenPublishAndReceivedMin = Math.min(nanosBetweenPublishAndReceivedMin, r.nanosBetweenPublishAndReceived());
        nanosBetweenReceivedAndAfterAckMin = Math.min(nanosBetweenReceivedAndAfterAckMin, r.nanosBetweenReceivedAndAfterAck());
        nanosBetweenPublishAndAfterAckMin = Math.min(nanosBetweenPublishAndAfterAckMin, r.nanosBetweenPublishAndAfterAck());

        // max
        elapsedBetweenPublishAndServerMax = Math.max(elapsedBetweenPublishAndServerMax, r.elapsedBetweenPublishAndServer());
        elapsedBetweenServerAndReceivedMax = Math.max(elapsedBetweenServerAndReceivedMax, r.elapsedBetweenServerAndReceived());
        elapsedBetweenReceivedAndAfterAckMax = Math.max(elapsedBetweenReceivedAndAfterAckMax, r.elapsedBetweenReceivedAndAfterAck());
        elapsedBetweenPublishAndReceivedMax = Math.max(elapsedBetweenPublishAndReceivedMax, r.elapsedBetweenPublishAndReceived());
        elapsedBetweenPublishAndAfterAckMax = Math.max(elapsedBetweenPublishAndAfterAckMax, r.elapsedBetweenPublishAndAfterAck());
        nanosBetweenPublishAndReceivedMax = Math.max(nanosBetweenPublishAndReceivedMax, r.nanosBetweenPublishAndReceived());
        nanosBetweenReceivedAndAfterAckMax = Math.max(nanosBetweenReceivedAndAfterAckMax, r.nanosBetweenReceivedAndAfterAck());
        nanosBetweenPublishAndAfterAckMax = Math.max(nanosBetweenPublishAndAfterAckMax, r.nanosBetweenPublishAndAfterAck());
    }

    public Stats update(Stats s) {
        totalMessages += s.totalMessages;
        totalBytes += s.totalBytes;

        elapsedBetweenPublishAndServer += s.elapsedBetweenPublishAndServer;
        elapsedBetweenServerAndReceived += s.elapsedBetweenServerAndReceived;
        elapsedBetweenReceivedAndAfterAck += s.elapsedBetweenReceivedAndAfterAck;
        elapsedBetweenPublishAndReceived += s.elapsedBetweenPublishAndReceived;
        elapsedBetweenPublishAndAfterAck += s.elapsedBetweenPublishAndAfterAck;
        nanosBetweenPublishAndReceived += s.nanosBetweenPublishAndReceived;
        nanosBetweenReceivedAndAfterAck += s.nanosBetweenReceivedAndAfterAck;
        nanosBetweenPublishAndAfterAck += s.nanosBetweenPublishAndAfterAck;

        // min
        elapsedBetweenPublishAndServerMin = Math.min(elapsedBetweenPublishAndServerMin, s.elapsedBetweenPublishAndServerMin);
        elapsedBetweenServerAndReceivedMin = Math.min(elapsedBetweenServerAndReceivedMin, s.elapsedBetweenServerAndReceivedMin);
        elapsedBetweenReceivedAndAfterAckMin = Math.min(elapsedBetweenReceivedAndAfterAckMin, s.elapsedBetweenReceivedAndAfterAckMin);
        elapsedBetweenPublishAndReceivedMin = Math.min(elapsedBetweenPublishAndReceivedMin, s.elapsedBetweenPublishAndReceivedMin);
        elapsedBetweenPublishAndAfterAckMin = Math.min(elapsedBetweenPublishAndAfterAckMin, s.elapsedBetweenPublishAndAfterAckMin);
        nanosBetweenPublishAndReceivedMin = Math.min(nanosBetweenPublishAndReceivedMin, s.nanosBetweenPublishAndReceivedMin);
        nanosBetweenReceivedAndAfterAckMin = Math.min(nanosBetweenReceivedAndAfterAckMin, s.nanosBetweenReceivedAndAfterAckMin);
        nanosBetweenPublishAndAfterAckMin = Math.min(nanosBetweenPublishAndAfterAckMin, s.nanosBetweenPublishAndAfterAckMin);

        // max
        elapsedBetweenPublishAndServerMax = Math.max(elapsedBetweenPublishAndServerMax, s.elapsedBetweenPublishAndServerMax);
        elapsedBetweenServerAndReceivedMax = Math.max(elapsedBetweenServerAndReceivedMax, s.elapsedBetweenServerAndReceivedMax);
        elapsedBetweenReceivedAndAfterAckMax = Math.max(elapsedBetweenReceivedAndAfterAckMax, s.elapsedBetweenReceivedAndAfterAckMax);
        elapsedBetweenPublishAndReceivedMax = Math.max(elapsedBetweenPublishAndReceivedMax, s.elapsedBetweenPublishAndReceivedMax);
        elapsedBetweenPublishAndAfterAckMax = Math.max(elapsedBetweenPublishAndAfterAckMax, s.elapsedBetweenPublishAndAfterAckMax);
        nanosBetweenPublishAndReceivedMax = Math.max(nanosBetweenPublishAndReceivedMax, s.nanosBetweenPublishAndReceivedMax);
        nanosBetweenReceivedAndAfterAckMax = Math.max(nanosBetweenReceivedAndAfterAckMax, s.nanosBetweenReceivedAndAfterAckMax);
        nanosBetweenPublishAndAfterAckMax = Math.max(nanosBetweenPublishAndAfterAckMax, s.nanosBetweenPublishAndAfterAckMax);

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
        double dtb = totalBytes;
        double dtm = totalMessages;

        writeHeader(out, totalMessages, payloadSize);
        out.write("Total".getBytes());
        column(out, elapsedBetweenPublishAndServer);
        column(out, elapsedBetweenServerAndReceived);
        column(out, elapsedBetweenReceivedAndAfterAck);
        column(out, elapsedBetweenPublishAndReceived);
        column(out, elapsedBetweenPublishAndAfterAck);
//        column(out, nanosBetweenPublishAndReceived);
//        column(out, nanosBetweenPublishAndAfterAck);
//        column(out, nanosBetweenReceivedAndAfterAck);
        out.write(lineSeparator().getBytes());

        out.write("Average".getBytes());
        column(out, elapsedBetweenPublishAndServer/ dtm);
        column(out, elapsedBetweenServerAndReceived/ dtm);
        column(out, elapsedBetweenReceivedAndAfterAck/ dtm);
        column(out, elapsedBetweenPublishAndReceived/ dtm);
        column(out, elapsedBetweenPublishAndAfterAck/ dtm);
//        column(out, nanosBetweenPublishAndReceived/ dtm);
//        column(out, nanosBetweenPublishAndAfterAck/ dtm);
//        column(out, nanosBetweenReceivedAndAfterAck/ dtm);
        out.write(lineSeparator().getBytes());

        out.write("Min".getBytes());
        column(out, elapsedBetweenPublishAndServerMin);
        column(out, elapsedBetweenServerAndReceivedMin);
        column(out, elapsedBetweenReceivedAndAfterAckMin);
        column(out, elapsedBetweenPublishAndReceivedMin);
        column(out, elapsedBetweenPublishAndAfterAckMin);
//        column(out, nanosBetweenPublishAndReceivedMin);
//        column(out, nanosBetweenPublishAndAfterAckMin);
//        column(out, nanosBetweenReceivedAndAfterAckMin);
        out.write(lineSeparator().getBytes());

        out.write("Max".getBytes());
        column(out, elapsedBetweenPublishAndServerMax);
        column(out, elapsedBetweenServerAndReceivedMax);
        column(out, elapsedBetweenReceivedAndAfterAckMax);
        column(out, elapsedBetweenPublishAndReceivedMax);
        column(out, elapsedBetweenPublishAndAfterAckMax);
//        column(out, nanosBetweenPublishAndReceivedMax);
//        column(out, nanosBetweenPublishAndAfterAckMax);
//        column(out, nanosBetweenReceivedAndAfterAckMax);
        out.write(lineSeparator().getBytes());

        out.write("bytes/ms".getBytes());
        column(out, dtb / elapsedBetweenPublishAndServer);
        column(out, dtb / elapsedBetweenServerAndReceived);
        out.write(',');
        column(out, dtb / elapsedBetweenPublishAndReceived);
        out.write(lineSeparator().getBytes());

        out.write("bytes/sec".getBytes());
        column(out, dtb * 1000 / elapsedBetweenPublishAndServer);
        column(out, dtb * 1000 / elapsedBetweenServerAndReceived);
        out.write(',');
        column(out, dtb * 1000 / elapsedBetweenPublishAndReceived);
        out.write(lineSeparator().getBytes());

        out.write("kb/sec".getBytes());
        column(out, dtb * 1000 / elapsedBetweenPublishAndServer / 1024);
        column(out, dtb * 1000 / elapsedBetweenServerAndReceived / 1024);
        out.write(',');
        column(out, dtb * 1000 / elapsedBetweenPublishAndReceived / 1024);
        out.write(lineSeparator().getBytes());

        out.write("message/sec".getBytes());
        column(out, dtm * 1000 / elapsedBetweenPublishAndServer);
        column(out, dtm * 1000 / elapsedBetweenServerAndReceived);
        out.write(',');
        column(out, dtm * 1000 / elapsedBetweenPublishAndReceived);
        out.write(lineSeparator().getBytes());
    }
}
