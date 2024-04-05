package io.nats.factory;

import io.nats.client.impl.SSLContextFactory;
import io.nats.client.impl.SSLContextFactoryProperties;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

class FactoryUsesPropertiesFromConnectionOptions implements SSLContextFactory {
    public static final String KEYSTORE_TYPE = "JKS";
    public static final String SSL_PROTOCOL = "TLSv1.2";

    @Override
    public SSLContext createSSLContext(SSLContextFactoryProperties properties) {
        System.out.println("Calling FactoryUsesPropertiesFromConnectionOptions.createSSLContext(...)");
        System.out.println("  These properties are passed in from the Options being used to create the connection:");
        System.out.println("    keystorePath:       " + properties.keystorePath);
        System.out.println("    keystorePassword:   " + new String(properties.keystorePassword));
        System.out.println("    truststorePath:     " + properties.truststorePath);
        System.out.println("    truststorePassword: " + new String(properties.truststorePassword));
        System.out.println("    tlsAlgorithm:       " + properties.tlsAlgorithm);

        try {
            // get the key managers
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (BufferedInputStream in =
                     new BufferedInputStream(
                         Files.newInputStream(
                             Paths.get(properties.keystorePath)))) {
                keyStore.load(in, properties.keystorePassword);
            }

            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(properties.tlsAlgorithm);
            kmFactory.init(keyStore, properties.keystorePassword);
            KeyManager[] keyManagers = kmFactory.getKeyManagers();

            // get the trust managers
            KeyStore trustStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (BufferedInputStream in =
                     new BufferedInputStream(
                         Files.newInputStream(
                             Paths.get(properties.truststorePath)))) {
                trustStore.load(in, properties.truststorePassword);
            }
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(properties.tlsAlgorithm);
            tmFactory.init(trustStore);
            TrustManager[] trustManagers = tmFactory.getTrustManagers();

            // create the context
            SSLContext ctx = SSLContext.getInstance(SSL_PROTOCOL);
            ctx.init(keyManagers, trustManagers, new SecureRandom());

            // return the context
            return ctx;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
