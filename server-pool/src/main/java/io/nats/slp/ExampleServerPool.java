package io.nats.slp;

import io.nats.client.Options;
import io.nats.client.impl.NatsServerPool;
import io.nats.client.support.NatsUri;

import java.util.List;

// By extending NatsServerPool you get a jump start on
// the implementation for ServerPool
class ExampleServerPool extends NatsServerPool {

    @Override
    public void initialize(Options opts) {
        // here is the opportunity to inspect any Options
        // you are already using in your connection
        super.initialize(opts);
    }

    @Override
    public boolean acceptDiscoveredUrls(List<String> discoveredServers) {
        // Whenever the client receives ServerInfo from the server
        // it provides a list of available servers to the pool
        // -----
        // If you are implementing custom behavior, for instance
        // you already know about all the servers you want,
        // so you might not care about these.
        return super.acceptDiscoveredUrls(discoveredServers);
    }

    @Override
    protected void afterListChanged() {
        // This method is part of NatsServerPool, not the ServerPool interface
        // This is called in NatsServerPool at the end of the initialize() method
        // and at the end of the acceptDiscoveredUrls() method
        super.afterListChanged();
    }

    @Override
    public NatsUri peekNextServer() {
        // this allows the connection handle to peek at the next server
        // you will give it via the nextServer() method
        return super.peekNextServer();
    }

    @Override
    public NatsUri nextServer() {
        // give the connection the next server to try to connect to
        // from your pool
        return super.nextServer();
    }

    @Override
    public List<String> resolveHostToIps(String host) {
        // the connection will try to resolve hosts to ip addresses
        // if Options.isNoResolveHostnames is not set (default is to resolve)
        return super.resolveHostToIps(host);
    }

    @Override
    public void connectSucceeded(NatsUri nuri) {
        // The connection lets you know if it was able to
        // connect to the nextServer() you gave it
        super.connectSucceeded(nuri);
    }

    @Override
    public void connectFailed(NatsUri nuri) {
        // The connection lets you know if it could not
        // connect to the nextServer() you gave it
        super.connectFailed(nuri);
    }

    @Override
    public List<String> getServerList() {
        // This method is not used by the connection other than to pass on
        // information to the user
        return super.getServerList();
    }

    @Override
    public boolean hasSecureServer() {
        // This asks your pool if any of your connection are secure
        // so it knows whether to upgrade the connection
        return super.hasSecureServer();
    }

    @Override
    protected int findEquivalent(List<NatsUri> list, NatsUri toFind) {
        // This method is part of NatsServerPool, not the ServerPool interface
        // It's used to keep its own server list unique
        return super.findEquivalent(list, toFind);
    }
}
