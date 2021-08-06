package io.nats.ft;

import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.nats.ft.Constants.DEBUG_DIR;
import static io.nats.ft.Constants.PART_STREAM_NAME;

@SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions"})
public class Debug
{
    static boolean FILE = true;
    static boolean CONSOLE = true;
    static boolean DEBUG = FILE || CONSOLE;
    static boolean PUB_FILE = DEBUG && true;
    static boolean PUB_PART = DEBUG && true;
    static boolean PUB_PART_PAYLOAD = PUB_PART && true;
    static boolean DOWN_FILE = DEBUG && true;
    static boolean DOWN_PART = DEBUG && true;
    static boolean DOWN_PART_PAYLOAD = DOWN_PART && true;
    static boolean DOWN_CONSUMER = DEBUG && true;
    static boolean DOWN_SUB_CONFIG = DEBUG && true;
    static int MAX_PUB_PARTS = Integer.MAX_VALUE;

    static int PAYLOAD_BYTES = 30;
    static boolean PAYLOAD_HEX = false;

    static FileOutputStream out;
    private static void write(String... strings) {
        if (FILE && out == null) {
            try {
                out = new FileOutputStream(DEBUG_DIR + "debug-" + System.currentTimeMillis() + ".log");
            } catch (FileNotFoundException e) {
                System.exit(-1);
            }
        }
        if (FILE) {
            try {
                for (String s : strings) {
                    out.write(s.getBytes(StandardCharsets.UTF_8));
                }
                out.write("\r\n".getBytes());
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

    public static void pubFile(FileMeta fm) {
        if (PUB_FILE) {
            write("Up Pub File: " + fm);
        }
    }

    public static void pubPart(FileMeta fm, PartMeta pm, byte[] payload) {
        if (PUB_PART && pm.getPartNumber() < MAX_PUB_PARTS) {
            write("Up Pub Part: " + fm.getName() + " " + pm);
            payloadBytes("Up Pub Part: ", PUB_PART_PAYLOAD, payload);
        }
    }

    private static void payloadBytes(String label, boolean flag, byte[] payload) {
        if (flag) {
            StringBuilder sb = new StringBuilder(label);
            if (PAYLOAD_HEX) {
                for (int x = 0; x < PAYLOAD_BYTES; x++) {
                    sb.append(String.format("%02x ", payload[x]));
                }
            }
            else {
                for (int x = 0; x < PAYLOAD_BYTES; x++) {
                    if (payload[x] == '\r' || payload[x] == '\n') {
                        sb.append('+');
                    }
                    else {
                        sb.append((char) payload[x]);
                    }
                }
            }
            write(sb.toString());
        }
    }

    public static void downFile(FileMeta fm, Digester fileDigester, long totalBytes, long totalParts) {
        if (DOWN_FILE) {
            write("Down File: D-" + fm.getDigestValue().equals(fileDigester.getDigestValue()) + " " + totalParts + " " + totalBytes);
            write("    " + fm);
        }
    }

    public static void downPart(long expecting, long expectingAdjustment, Message m, PartMeta pm, boolean ematch, boolean pmatch, Boolean dmatch, byte[] partBytes) {
        if (DOWN_PART) {
            write("Down Part: E-" + ematch + " P-" + pmatch + " D-" + dmatch);
            write("    " + pm);
            write("     Expecting " + expecting + "/" + (expecting + expectingAdjustment) + " Got " + m.metaData().consumerSequence());
            payloadBytes("Down Part: ", DOWN_PART_PAYLOAD, partBytes);
        }
    }

    public static void downConsumer(JetStreamManagement jsm) throws IOException, JetStreamApiException {
        if (DOWN_CONSUMER) {
            List<ConsumerInfo> cis = jsm.getConsumers(PART_STREAM_NAME);
            write("Down Consumer: " + cis.get(cis.size() - 1));
        }
    }

    public static void downSubConfig(ConsumerConfiguration cc) {
        if (DOWN_SUB_CONFIG) {
            write("Down Sub: " + cc.toString());
        }
    }
}
