package io.nats.factory;

import io.nats.client.impl.SSLContextFactory;
import io.nats.client.impl.SSLContextFactoryProperties;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

class FactoryUsesPropertiesFromEnvironment implements SSLContextFactory {
    public static String PROPERTY_NATS_TLS_KEY_STORE            = "NATS_TLS_KEY_STORE";
    public static String PROPERTY_NATS_TLS_KEY_STORE_PASSWORD   = "NATS_TLS_KEY_STORE_PASSWORD";
    public static String PROPERTY_NATS_TLS_TRUST_STORE          = "NATS_TLS_TRUST_STORE";
    public static String PROPERTY_NATS_TLS_TRUST_STORE_PASSWORD = "NATS_TLS_TRUST_STORE_PASSWORD";
    public static String PROPERTY_NATS_TLS_ALGO                 = "NATS_TLS_ALGO";

    public static final String KEYSTORE_TYPE = "JKS";
    public static final String SSL_PROTOCOL = "TLSv1.2";

    @Override
    public SSLContext createSSLContext(SSLContextFactoryProperties properties) {
        String keystorePath = System.getProperty(PROPERTY_NATS_TLS_KEY_STORE);
        String keystorePassword = System.getProperty(PROPERTY_NATS_TLS_KEY_STORE_PASSWORD);
        String truststorePath = System.getProperty(PROPERTY_NATS_TLS_TRUST_STORE);
        String truststorePassword = System.getProperty(PROPERTY_NATS_TLS_KEY_STORE_PASSWORD);
        String tlsAlgorithm = System.getProperty(PROPERTY_NATS_TLS_ALGO);

        System.out.println("Calling FactoryUsesPropertiesFromEnvironment.createSSLContext(...)");
        System.out.println("  These properties are read from the environment.");
        System.out.println("    keystorePath:       " + keystorePath);
        System.out.println("    keystorePassword:   " + keystorePassword);
        System.out.println("    truststorePath:     " + truststorePath);
        System.out.println("    truststorePassword: " + truststorePassword);
        System.out.println("    tlsAlgorithm:       " + tlsAlgorithm);

        try {
            // get the key managers
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (BufferedInputStream in =
                     new BufferedInputStream(
                         Files.newInputStream(
                             Paths.get(keystorePath)))) {
                keyStore.load(in, keystorePassword.toCharArray());
            }

            KeyManagerFactory kmFactory = KeyManagerFactory.getInstance(tlsAlgorithm);
            kmFactory.init(keyStore, keystorePassword.toCharArray());
            KeyManager[] keyManagers = kmFactory.getKeyManagers();

            // get the trust managers
            KeyStore trustStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (BufferedInputStream in =
                     new BufferedInputStream(
                         Files.newInputStream(
                             Paths.get(truststorePath)))) {
                trustStore.load(in, truststorePassword.toCharArray());
            }
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(tlsAlgorithm);
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
