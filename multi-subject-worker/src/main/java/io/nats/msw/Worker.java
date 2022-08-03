package io.nats.msw;

import io.nats.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Worker implements Runnable {
    public static int MESSAGE_COUNT = 5000;

    public static void main(String[] args) {
        try (Connection nc = Nats.connect(Options.DEFAULT_URL)) {
            List<Worker> workers = new ArrayList<>();
            workers.add(new Worker(nc, 1, "msw.A.segment"));
            workers.add(new Worker(nc, 2, "msw.B.segment"));
            workers.add(new Worker(nc, 3, "msw.C.segment"));

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
                System.out.println("Worker " + w.id + " processed " + w.processed);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Connection nc;
    private final String subject;
    public final int id;
    public int processed = 0;

    public Worker(Connection nc, int id, String subject) {
        this.nc = nc;
        this.subject = subject;
        this.id = id;
        System.out.println("Init worker " + this.id);
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
                    Thread.sleep(ThreadLocalRandom.current().nextLong(id * 3L)); // add a jitter to simulate actual processing
                    m.ack();
                    if (++processed % 100 == 0) {
                        System.out.println("Worker " + id + " processed " + processed);
                    }
                }
            }
        }
        catch (IOException | JetStreamApiException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
