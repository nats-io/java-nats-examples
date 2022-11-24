package io.nats.ft;

import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Debug
{
    public static final byte[] NEWLINE = "\r\n".getBytes();
    public static final String INDENT = "    ";

    static boolean FILE = true;
    static boolean CONSOLE = true;
    static int MAX_PUB_PARTS = Integer.MAX_VALUE;
    static int PAYLOAD_BYTES = 30;
    static boolean HEX_OUT = true;
    static boolean ON = true;
    static boolean OFF = true;

    static boolean DEBUG()              { return FILE || CONSOLE; }
    static boolean PUB_FILE()           { return DEBUG() && ON; }
    static boolean PUB_PART()           { return DEBUG() && ON; }
    static boolean PUB_PART_PAYLOAD()   { return PUB_PART() && ON; }
    static boolean CON_FILE()           { return DEBUG() && ON; }
    static boolean CON_PART()           { return DEBUG() && ON; }
    static boolean DOWN_PART_PAYLOAD()  { return CON_PART() && ON; }
    static boolean CONSUMER()           { return DEBUG() && ON; }
    static boolean SUB_CONFIG()         { return DEBUG() && ON; }

    static FileOutputStream out;
    public static void write(String... strings) {
        if (FILE && out == null) {
            try {
                out = new FileOutputStream(Constants.DEBUG_OUTPUT_DIR + "debug.log");
            } catch (FileNotFoundException e) {
                System.exit(-1);
            }
        }
        if (FILE) {
            try {
                for (String s : strings) {
                    out.write(s.getBytes(StandardCharsets.UTF_8));
                }
                out.write(NEWLINE);
            } catch (IOException e) {
                System.exit(-2);
            }
        }
        if (CONSOLE) {
            for (String s : strings) {
                System.out.print(s);
            }
            System.out.println();
        }
    }

    public static void info(String... strings) {
        if (DEBUG()) {
            write(strings);
        }
    }

    public static void pubFile(FileMeta fm) {
        if (PUB_FILE()) {
            write("Published File: " + fm);
        }
    }

    public static void conFile(FileMeta fm, Digester fileDigester, long totalBytes, long totalParts) {
        if (CON_FILE()) {
            write("Consumed File: D-" + fm.getDigestValue().equals(fileDigester.getDigestValue()) + " " + totalParts + " " + totalBytes);
            write(INDENT + fm);
        }
    }

    public static void payloadBytes(String label, boolean flag, byte[] payload) {
        if (flag) {
            StringBuilder sb = new StringBuilder(label).append("Bytes: ").append(payload.length).append(" | ");
            for (int x = 0; x < PAYLOAD_BYTES && x < payload.length; x++) {
                if (x > 0) {
                    sb.append(' ');
                }
                if (HEX_OUT || payload[x] < 33 || payload[x] > 126) {
                    sb.append(String.format("%02x", payload[x]));
                }
                else {
                    sb.append((char) payload[x]);
                }
            }
            sb.append(" ...");
            write(sb.toString());
        }
    }

    public static void pubPart(FileMeta fm, PartMeta pm, byte[] payload, boolean summary) {
        if (PUB_PART() && pm.getPartNumber() < MAX_PUB_PARTS) {
            if (summary) {
                payloadBytes("Published Part: " + fm.getName() + " " + pm.toSummaryString() + " ", PUB_PART_PAYLOAD(), payload);
            }
            else {
                write("Published Part: " + fm.getName() + " " + pm);
                payloadBytes("                ", PUB_PART_PAYLOAD(), payload);
            }
        }
    }

    public static void conPartFull(long expecting, long expectingAdjustment, Message m, PartMeta pm, boolean ematch, boolean pmatch, Boolean dmatch) {
        if (CON_PART()) {
            write("Consumed Part: " + "E-" + ematch + " P-" + pmatch + " D-" + dmatch + " Expecting " + expecting + "/" + (expecting + expectingAdjustment) + " -> " + m.metaData().consumerSequence());
            write(INDENT + pm);
            payloadBytes(INDENT, DOWN_PART_PAYLOAD(), m.getData());
        }
    }

    public static void conPartSummary(long expecting, long expectingAdjustment, Message m) {
        if (CON_PART()) {
            payloadBytes("Consumed Part: Expecting " + expecting + "/" + (expecting + expectingAdjustment) + " -> " + m.metaData().consumerSequence() + " ",
                    DOWN_PART_PAYLOAD(), m.getData());
        }
    }

//    public static void consumer(JetStreamManagement jsm) throws IOException, JetStreamApiException {
//        if (CONSUMER()) {
//            List<ConsumerInfo> cis = jsm.getConsumers(PART_STREAM_NAME);
//            ConsumerInfo newest = null;
//            for (ConsumerInfo ci : cis) {
//                if (newest == null) {
//                    newest = ci;
//                }
//                else if (ci.getCreationTime().isAfter(newest.getCreationTime())){
//                    newest = ci;
//                }
//            }
//            write("Consumer: " + cis.size() + " | " + newest);
//        }
//    }

    public static void consumer(JetStreamSubscription sub) throws IOException, JetStreamApiException {
        if (CONSUMER()) {
            ConsumerInfo ci = sub.getConsumerInfo();
            write("Consumer: " + ci);
        }
    }

    public static void subConfig(ConsumerConfiguration cc) {
        if (SUB_CONFIG()) {
            write("Sub Config: " + cc.toString());
        }
    }
}
