// Copyright 2020 The NATS Authors
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

package io.nats;

import io.nats.client.*;
import io.nats.client.api.*;
import io.nats.client.impl.ErrorListenerConsoleImpl;
import io.nats.client.impl.NatsKeyValueWatchSubscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class KvWatcherTester {
    static String BUCKET_NAME = "KvWatcherTester";
    static long DATA_DELAY = 250;
    static int KEY_COUNT = 20;
    static int DELETE_PERCENT = 25;

    public static void main(String[] args) throws InterruptedException {
        report("STARTUP", "CLIENT_VERSION", Nats.CLIENT_VERSION);

        setupBucket();

        Thread tData = new Thread(data());
        tData.start();

        Thread tWatch = new Thread(watch());
        tWatch.start();

        tWatch.join();
    }

    private static Runnable data() {
        return () -> {
            try (Connection connection = Nats.connect(options("DATA"))) {
                KeyValue kv = connection.keyValue(BUCKET_NAME);

                while (true) {
                    try {
                        String key = "key-" + ThreadLocalRandom.current().nextInt(KEY_COUNT);
                        if (ThreadLocalRandom.current().nextInt(100) <= DELETE_PERCENT) {
                            KeyValueEntry existing = kv.get(key);
                            if (existing != null) {
                                kv.delete(key);
                            }
                        }
                        else {
                            kv.put(key, NUID.nextGlobalSequence());
                        }
                    }
                    catch (Exception e) {
                        report("DATA", "Error", e.toString());
                    }
                    Thread.sleep(DATA_DELAY);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Runnable watch() {
        return () -> {
            try (Connection connection = Nats.connect(options("WATCH"))) {
                KeyValue kv = connection.keyValue(BUCKET_NAME);

                AtomicInteger puts = new AtomicInteger();
                AtomicInteger dels = new AtomicInteger();
                AtomicInteger purges = new AtomicInteger();
                AtomicInteger eod = new AtomicInteger();

                KeyValueWatcher watcher = new KeyValueWatcher() {
                    @Override
                    public void watch(KeyValueEntry keyValueEntry) {
                        if (keyValueEntry.getOperation() == KeyValueOperation.PUT) {
                            report("WATCH", "PUT", puts.incrementAndGet() + " [" + keyValueEntry.getKey() + "]");
                        }
                        else if (keyValueEntry.getOperation() == KeyValueOperation.DELETE) {
                            report("WATCH", "DELETE", dels.incrementAndGet() + " [" + keyValueEntry.getKey() + "]");
                        }
                        else if (keyValueEntry.getOperation() == KeyValueOperation.PURGE) {
                            report("WATCH", "PURGE", purges.incrementAndGet() + " [" + keyValueEntry.getKey() + "]");
                        }
                    }

                    @Override
                    public void endOfData() {
                        report("WATCH", "EOD", "" + eod.incrementAndGet());
                    }
                };

                CountDownLatch latch = new CountDownLatch(1);
                NatsKeyValueWatchSubscription watch = kv.watchAll(watcher);
                latch.await();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Options options(String optionsLabel) {
        ErrorListenerConsoleImpl el = new ErrorListenerConsoleImpl() {
            @Override
            public String supplyMessage(String label, Connection conn, Consumer consumer, Subscription sub, Object... pairs) {
                return messagePrefix("EL", optionsLabel) + super.supplyMessage(label, conn, consumer, sub, pairs);
            }
        };

        return Options.builder()
            .connectionListener((c, e) -> System.out.println(messagePrefix("CL", optionsLabel) + e.name()))
            .errorListener(el)
            .build();
    }

    private static String messagePrefix(String area, String label) {
        return "[" + System.currentTimeMillis() + "] " + area + "/" + label + " | ";
    }

    private static void report(String area, String label, String message) {
        System.out.println(messagePrefix(area, label) + " " + message);
    }

    private static void setupBucket() {
        try (Connection connection = Nats.connect(options("SETUP"))) {
            KeyValueManagement kvm = connection.keyValueManagement();
            try {
                kvm.delete(BUCKET_NAME);
            }
            catch (Exception ignore) {
            }
            KeyValueConfiguration kvc = KeyValueConfiguration.builder(BUCKET_NAME)
                .storageType(StorageType.File)
                .build();
            KeyValueStatus kvs = kvm.create(kvc);
            report("SETUP", "Bucket Created", kvs.toString());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}