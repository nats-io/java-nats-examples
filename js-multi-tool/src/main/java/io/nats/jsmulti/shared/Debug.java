package io.nats.jsmulti.shared;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import io.nats.client.api.StreamInfo;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.impl.NatsMessage;
import io.nats.client.support.JsonSerializable;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

// MODIFIED 07/05/2024 1

@SuppressWarnings("SameParameterValue")
public abstract class Debug {

    public interface DebugPrinter {
        void println(String s);
    }

    public static final String SEP = " | ";
    public static final String PAD = "                                                                                                                                                                                                                                                                                                                                                                                                                                    ";
    public static boolean DO_NOT_TRUNCATE = true;
    public static boolean PRINT_THREAD_ID = true;
    public static boolean PRINT_TIME = true;
    public static boolean PAUSE = false;
    public static DebugPrinter DEBUG_PRINTER = System.out::println;

    private Debug() {}  /* ensures cannot be constructed */

    public static void msg(Message msg) {
        info(null, msg, true, null);
    }

    public static void msg(Message msg, Object... extras) {
        info(null, msg, true, stringify(extras, false));
    }

    public static void msg(String label, Message msg, Object... extras) {
        info(label, msg, true, stringify(extras, false));
    }

    public static void stackTrace(String label) {
        if (PAUSE) { return; }
        try {
            throw new Exception();
        }
        catch (Exception e) {
            stackTrace(label, e);
        }
    }

    public static void stackTrace(String label, Throwable t) {
        if (PAUSE) { return; }
        StackTraceElement[] elements = t.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            String ts = elements[i].toString();
            if (i == 0) {
                info(label, ts);
            }
            else {
                info(label, ">   " + ts);
            }
            if (ts.startsWith("java")) {
                break;
            }
        }
    }

    public static void info(String label, Object... extras) {
        if (PAUSE) { return; }
        if (extras == null || extras.length == 0) {
            info(label, null, false, null);
        }
        else if (extras[0] instanceof NatsMessage) {
            info(label, (NatsMessage)extras[0], true, stringify(extras, true));
        }
        else {
            info(label, null, false, stringify(extras, false));
        }
    }

    public static void info(String label, Message msg, boolean forMsg, String extra) {
        if (PAUSE) { return; }
        String start;
        if (PRINT_TIME && PRINT_THREAD_ID) {
            start = "[" + Thread.currentThread().getName() + "@" + time() + "] ";
        }
        else if (PRINT_TIME){
            start = "[" + time() + "] ";
        }
        else if (PRINT_THREAD_ID){
            start = "[" + Thread.currentThread().getName() + "] ";
        }
        else {
            start = "";
        }

        if (extra == null) {
            extra = "";
        }
        else {
            extra = SEP + extra;
        }

        if (label != null) {
            label = label.trim();
        }
        if (label == null || label.isEmpty()) {
            label = start;
        }
        else {
            label = start + label;
        }

        if (msg == null) {
            if (forMsg) {
                DEBUG_PRINTER.println(label + "<nullmsg>" + extra);
            }
            else {
                DEBUG_PRINTER.println(label + extra);
            }
            return;
        }

        if (msg.isStatusMessage()) {
            DEBUG_PRINTER.println(label + sidString(msg) + subjString(msg) + msg.getStatus() + extra);
        }
        else if (msg.isJetStream()) {
            DEBUG_PRINTER.println(label + sidString(msg) + subjString(msg) + dataString(msg) + replyToString(msg) + extra);
        }
        else if (msg.getSubject() == null) {
            DEBUG_PRINTER.println(label + sidString(msg) + msg + extra);
        }
        else {
            DEBUG_PRINTER.println(label + sidString(msg) + subjString(msg) + dataString(msg) + replyToString(msg) + extra);
        }
        debugHdr(label.length() + 1, msg);
    }

    public static void warn(String label, Object... extras) {
        info(label, extras);
    }

    public static void warn(String label, Message msg, boolean forMsg, String extra) {
        info(label, msg, forMsg, extra);
    }

    public static String sidString(Message msg) {
        return msg.getSID() == null ? SEP : " sid:" + msg.getSID() + SEP;
    }

    public static String subjString(Message msg) {
        return msg.getSubject() + SEP;
    }

    public static String replyToString(Message msg) {
        if (msg.isJetStream()) {
            NatsJetStreamMetaData meta = msg.metaData();
            return "ss:" + meta.streamSequence() + ' '
                + "cc:" + meta.consumerSequence() + ' '
                + "dlvr:" + meta.deliveredCount() + ' '
                + "pnd:" + meta.pendingCount()
                + SEP;
        }
        if (msg.getReplyTo() == null) {
            return "<no reply>";
        }
        return msg.getReplyTo();
    }

    public static String time() {
        String t = "" + System.currentTimeMillis();
        return t.substring(t.length() - 9);
    }

    public static String dataString(Message msg) {
        byte[] data = msg.getData();
        if (data == null || data.length == 0) {
            return "<no data>" + SEP;
        }
        String s = new String(data, UTF_8);
        if (DO_NOT_TRUNCATE) {
            return s + SEP;
        }

        int at = s.indexOf("io.nats.jetstream.api");
        if (at == -1) {
            return s.length() > 27 ? s.substring(0, 27) + "..." : s;
        }
        int at2 = s.indexOf('"', at);
        return s.substring(at, at2) + SEP;
    }

    public static String stringify(Object[] extras, boolean skipFirst) {
        if (extras == null || extras.length == 0) {
            return null;
        }

        if (extras.length == 1) {
            return skipFirst ? null : getString(extras[0]);
        }

        boolean notFirst = false;
        StringBuilder sb = new StringBuilder();
        for (int i = (skipFirst ? 1 : 0); i < extras.length; i++) {
            if (notFirst) {
                sb.append(SEP);
            }
            else {
                notFirst = true;
            }

            String xtra = getString(extras[i]);
            while (xtra.contains("{}")) {
                xtra = xtra.replaceFirst("\\Q{}\\E", getString(extras[++i]));
            }
            sb.append(xtra);
        }

        return sb.length() == 0 ? null : sb.toString();
    }

    public static String getString(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof ConsumerInfo) {
            o = ((ConsumerInfo)o).getConsumerConfiguration();
        }
        if (o instanceof ConsumerConfiguration) {
            return formatted((ConsumerConfiguration)o);
        }
        if (o instanceof Headers) {
            Headers h = (Headers)o;
            boolean notFirst = false;
            StringBuilder sb = new StringBuilder("[");
            for (String key : h.keySet()) {
                if (notFirst) {
                    sb.append(',');
                }
                else {
                    notFirst = true;
                }
                sb.append(key).append("=").append(h.get(key));
            }
            return sb.append(']').toString();
        }
        if (o instanceof JsonSerializable) {
            return ((JsonSerializable)o).toJson();
        }
        if (o instanceof byte[]) {
            byte[] bytes = (byte[])o;
            if (bytes.length == 0) {
                return "<byte[0]>";
            }
            return new String((byte[])o);
        }
        String s = o.toString().trim();
        return s.isEmpty() ? "<empty>" : s;
    }

    public static void debugHdr(int indent, Message msg) {
        Headers h = msg.getHeaders();
        if (h != null && !h.isEmpty()) {
            String pad = PAD.substring(0, indent);
            for (String key : h.keySet()) {
                DEBUG_PRINTER.println(pad + key + "=" + h.get(key));
            }
        }
    }

    public static void streamAndConsumer(Connection nc, String stream, String conName) throws IOException, JetStreamApiException {
        streamAndConsumer(nc.jetStreamManagement(), stream, conName);
    }

    public static void streamAndConsumer(JetStreamManagement jsm, String stream, String conName) throws IOException, JetStreamApiException {
        printStreamInfo(jsm.getStreamInfo(stream));
        printConsumerInfo(jsm.getConsumerInfo(stream, conName));
    }

    public static void consumer(Connection nc, String stream, String conName) throws IOException, JetStreamApiException {
        consumer(nc.jetStreamManagement(), stream, conName);
    }

    public static void consumer(JetStreamManagement jsm, String stream, String conName) throws IOException, JetStreamApiException {
        ConsumerInfo ci = jsm.getConsumerInfo(stream, conName);
        DEBUG_PRINTER.println("Consumer numPending=" + ci.getNumPending() + " numWaiting=" + ci.getNumWaiting() + " numAckPending=" + ci.getNumAckPending());
    }

    public static void printStreamInfo(StreamInfo si) {
        printObject(si, "StreamConfiguration", "StreamState", "ClusterInfo", "Mirror", "subjects", "sources");
    }

    public static void printStreamInfoList(List<StreamInfo> list) {
        printObject(list, "!StreamInfo", "StreamConfiguration", "StreamState");
    }

    public static void printConsumerInfo(ConsumerInfo ci) {
        printObject(ci, "ConsumerConfiguration", "Delivered", "AckFloor");
    }

    public static void printConsumerInfoList(List<ConsumerInfo> list) {
        printObject(list, "!ConsumerInfo", "ConsumerConfiguration", "Delivered", "AckFloor");
    }

    public static void printObject(Object o, String... subObjectNames) {
        String s = o.toString();
        for (String sub : subObjectNames) {
            boolean noIndent = sub.startsWith("!");
            String sb = noIndent ? sub.substring(1) : sub;
            String rx1 = ", " + sb;
            String repl1 = (noIndent ? ",\n": ",\n    ") + sb;
            s = s.replace(rx1, repl1);
        }
        DEBUG_PRINTER.println(s);
    }

    public static String pad2(int n) {
        return n < 10 ? " " + n : "" + n;
    }

    public static String pad3(int n) {
        return n < 10 ? "  " + n : (n < 100 ? " " + n : "" + n);
    }

    public static String yn(boolean b) {
        return b ? "Yes" : "No ";
    }

    public static String FN = "\n  ";
    public static String FBN = "{\n  ";
    public static String formatted(JsonSerializable j) {
        return j.getClass().getSimpleName() + j.toJson()
            .replace("{\"", FBN + "\"").replace(",", "," + FN);
    }

    public static String formatted(Object o) {
        return formatted(o.toString());
    }

    public static String formatted(String s) {
        return s.replace("{", FBN).replace(", ", "," + FN);
    }
}
