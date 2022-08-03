package io.nats.msw;

import io.nats.client.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Worker implements Runnable {
    public static int MESSAGE_COUNT = 5000;

    public static void main(String[] args) {
        try (Connection nc = Nats.connect(Options.DEFAULT_URL)) {
            List<Worker> workers = new ArrayList<>();
            workers.add(new Worker(nc, 1, "msw.one.EAST.segment"));
            workers.add(new Worker(nc, 2, "msw.one.WEST.segment"));
            workers.add(new Worker(nc, 3, "msw.two.*.segment", "msw.two.EAST.segment", "msw.two.WEST.segment"));

            List<Thread> threads = new ArrayList<>();
            for (Worker w : workers) {
                threads.add(new Thread(w));
            }

            for (Thread t : threads) {
                t.start();
            }

            for (Thread t : threads) {
                t.join();
            }

            System.out.println("\nSummary");
            for (Worker w : workers) {
                System.out.println("Worker " + w.id + ". Processed " + w.processed + ". Matched: " + w.matched + ". " + matchCounterToString(w.matchList, w.matchMap));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Connection nc;
    public final String subject;
    public final List<String> matchList;
    public final Map<String, AtomicInteger> matchMap;
    public final int id;
    public int processed = 0;
    public boolean matched = true;

    public static String matchCounterToString(List<String> subjects, Map<String, AtomicInteger> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String subject : subjects) {
            if (first) {
                first = false;
            }
            else {
                sb.append(", ");
            }
            sb.append(subject);
            sb.append(" x ");
            sb.append(map.get(subject).get());
        }
        return sb.toString();
    }

    public Worker(Connection nc, int id, String subject, String... matches) {
        this.nc = nc;
        this.subject = subject;
        this.id = id;
        matchList = new ArrayList<>();
        matchMap = new HashMap<>();
        if (matches.length == 0) {
            matchList.add(subject);
            matchMap.put(subject, new AtomicInteger());
        }
        else {
            for (String s : matches) {
                matchList.add(s);
                matchMap.put(s, new AtomicInteger());
            }
            Collections.sort(matchList);
        }

        System.out.println("Init worker " + this.id + " " + matchMap);
    }

    @Override
    public void run() {
        System.out.println("Run worker: " + id);
        try {
            JetStream js = nc.jetStream();
            PullSubscribeOptions pullOpts = PullSubscribeOptions.builder().build();
            JetStreamSubscription sub = js.subscribe(subject, pullOpts);
            JetStreamReader reader = sub.reader(20, 15);
            while (processed < MESSAGE_COUNT) {

                Message m = reader.nextMessage(1000);
                if (m != null) {
                    AtomicInteger matchCounter = matchMap.get(m.getSubject());
                    if (matchCounter == null) {
                        matched = false;
                        return;
                    }

                    matchCounter.incrementAndGet();
                    m.ack();
                    Thread.sleep(ThreadLocalRandom.current().nextLong(id * 2L)); // add a jitter to simulate actual processing
                    if (++processed % 100 == 0) {
                        System.out.println("Worker " + id + " processed " + processed + " " + matchCounterToString(matchList, matchMap));
                    }
                }
            }
        }
        catch (IOException | JetStreamApiException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
