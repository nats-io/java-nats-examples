package io.nats.slp;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.ServerInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

class ProvideServersList { // implements ServerListProvider {
    // TO DEMONSTRATE I STARTED A 3 CLUSTER SERVER LOCALLY on ports 4222, 5222 and 6222
    // The ServerListProvider implementation in this case is a simple randomization

    static String[] BOOTSTRAP = new String[] {"0.0.0.0:4222", "0.0.0.0:5222", "0.0.0.0:6222"};

    public static void main(String[] args) {
        Options options = new Options.Builder()
            .servers(BOOTSTRAP)
//            .serverListProvider(new ProvideServersList())
            .build();

        System.out.println("CONNECTING");
        try (Connection nc = Nats.connect(options)) {
            ServerInfo si = nc.getServerInfo();
            System.out.println("CONNECTED 1");
            System.out.println("  to: " + si.getHost() + " " + si.getPort());
            System.out.println("  discovered: " + si.getConnectURLs());

            // WHILE THE THREAD IS SLEEPING, KILL THE SERVER WE ARE CONNECTED TO SO A RECONNECT OCCURS
            Thread.sleep(10_000);

            System.out.println("CONNECTED 2");
            si = nc.getServerInfo();
            System.out.println("  to: " + si.getHost() + " " + si.getPort());
            System.out.println("  discovered: " + si.getConnectURLs());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getServerList(String currentServer, List<String> optionsServersUnprocessed, List<String> discoveredServersUnprocessed) {
        System.out.println("GET SERVER LIST");
        System.out.println("  current server: " + currentServer);
        System.out.println("  servers as bootstrapped: " + optionsServersUnprocessed);
        System.out.println("  servers as discovered: " + discoveredServersUnprocessed);

        if (discoveredServersUnprocessed.size() == 0) {
            return randomize(currentServer, Arrays.asList(BOOTSTRAP));
        }

        return randomize(currentServer, discoveredServersUnprocessed);
    }

    private static List<String> randomize(String currentServer, List<String> servers) {
        if (servers.size() > 1) {
            if (currentServer != null) {
                servers.remove(currentServer);
            }
            Collections.shuffle(servers, ThreadLocalRandom.current());
            if (currentServer != null) {
                servers.add(currentServer);
            }
        }
        return servers;
    }
}
