package io.nats.ocsp;

import io.nats.client.*;
import nl.altindag.ssl.util.CertificateUtils;
import nl.altindag.ssl.util.KeyStoreUtils;
import nl.altindag.ssl.util.PemUtils;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.util.EnumSet;
import java.util.List;

/**
 * This example will run against the test "TestOCSPServerExample" in the "ocsp-test" branch in the NATS server
 */
public class OcspExample
{

    public static final String OCSP_SERVER_URI = "tls://localhost:4222";
    public static final String STD_TLS_SERVER_URI = "tls://localhost:60991";

    public static void main(String[] args) throws Exception {
//        connect(createStandardContext(), STD_TLS_SERVER_URI);
        connect(createVmWideOcspCheckRevocationContext(), OCSP_SERVER_URI);
//        connect(createSiloedContextCheckRevocation(), OCSP_SERVER_URI);
//        connect(createVmWideOcspDontCheckRevocationContext(), OCSP_SERVER_URI);
    }

    public static SSLContext createVmWideOcspCheckRevocationContext() throws NoSuchAlgorithmException, KeyManagementException {
        return createVmWideOcspContext(true);
    }

    public static SSLContext createVmWideOcspDontCheckRevocationContext() throws NoSuchAlgorithmException, KeyManagementException {
        return createVmWideOcspContext(false);
    }

    public static SSLContext createVmWideOcspContext(boolean checkRevocation) throws NoSuchAlgorithmException, KeyManagementException {
        // enableStatusRequestExtension and checkRevocation are turned on
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", checkRevocation ? "true" : "false");

        // The example client cert and key are in pem files. This PemUtils makes a Key Manager from them.
        // You likely will have to figure out how to make your own Key Manager
        X509ExtendedKeyManager keyManager =
                PemUtils.loadIdentityMaterial(ocspClientCertPath(), ocspClientKeyPath());

        // The example ca cert and key is in a pem file. This PemUtils makes a Trust Manager from it.
        // You likely will have to figure out how to make your own Trust Manager
        X509ExtendedTrustManager trustManager =
                PemUtils.loadTrustMaterial(ocspCaCertPath());

        // Construct and initialize the sslContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(new KeyManager[]{keyManager}, new TrustManager[]{trustManager}, new SecureRandom());

        return sslContext;
    }

    public static SSLContext createSiloedContextCheckRevocation()
            throws KeyStoreException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, KeyManagementException
    {
        // enableStatusRequestExtension is turned on but checkRevocation is not, otherwise it
        // would affect the entire system. This example builds a siloed SSLContext that
        // does not need the checkRevocation system wide setting.
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");

        // The example client cert and key are in pem files. This PemUtils makes a Key Manager from them.
        // You likely will have to figure out how to make your own Key Manager
        X509ExtendedKeyManager keyManager =
                PemUtils.loadIdentityMaterial(ocspClientCertPath(), ocspClientKeyPath());

        // The example ca cert is in a pem file. loadCertificate loads the certificate from
        // that pem file. Then add the certificates to a Key Store.
        // You likely will have to figure out how to load your own certificates and/or
        // have a different way to make or load your own Key Store
        List<Certificate> certificates = CertificateUtils.loadCertificate(ocspCaCertPath());
        KeyStore keyStore = KeyStoreUtils.createTrustStore(certificates);

        // The PKIXRevocationChecker is the class that does the work of checking the revocation
        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker rc = (PKIXRevocationChecker)cpb.getRevocationChecker();
        rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        // PKIXBuilderParameters uses the Key Store and the Revocation Checker
        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(keyStore, new X509CertSelector());
        pkixParams.addCertPathChecker(rc);

        // The CertPathTrustManagerParameters is made from the PKIX Builder parameters
        CertPathTrustManagerParameters certPathTrustManagerParameters = new CertPathTrustManagerParameters(pkixParams);

        // The Trust Manager Factory is made from the CertPathTrustManagerParameters
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(certPathTrustManagerParameters);

        // Construct and initialize the sslContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(new KeyManager[]{keyManager}, tmf.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    public static SSLContext createStandardContext() throws NoSuchAlgorithmException, KeyManagementException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException {
        KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance("SunX509");
        keyMgrFactory.init(standardKeyStore(), password());

        TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance("SunX509");
        trustMgrFactory.init(standardTruststore());

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyMgrFactory.getKeyManagers(), trustMgrFactory.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    // ----------------------------------------------------------------------------------------------------
    // Try to connect to the server
    // ----------------------------------------------------------------------------------------------------
    public static void connect(SSLContext sslContext, String uri) {
        ErrorListener el = new ErrorListener() {
            @Override
            public void errorOccurred(Connection conn, String error) {

            }

            @Override
            public void exceptionOccurred(Connection conn, Exception exp) {
                exp.printStackTrace();
            }

            @Override
            public void slowConsumerDetected(Connection conn, Consumer consumer) {

            }
        };

        Options options = new Options.Builder()
                .server(uri)
                .maxReconnects(0)
                .sslContext(sslContext)
                .errorListener(el)
                .build();

        try (Connection nc = Nats.connect(options)) {
            nc.publish("subject", "data".getBytes());
            System.out.println("Connected");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Cert / Store Utilities
    //     These files are specific to the server being run by the developer of the example.
    //     You will need your own way to load certs / stores.
    // ----------------------------------------------------------------------------------------------------

    private static Path ocspClientCertPath() {
        return getFile("ocsp-client-cert.pem").toPath();
    }

    private static Path ocspClientKeyPath() {
        return getFile("ocsp-client-key.pem").toPath();
    }

    private static Path ocspCaCertPath() {
        return getFile("ocsp-ca-cert.pem").toPath();
    }

    private static File getFile(String fileName) {
        ClassLoader classLoader = OcspExample.class.getClassLoader();
        //noinspection ConstantConditions
        return new File(classLoader.getResource(fileName).getFile());
    }

    private static KeyStore standardTruststore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        return loadKeystore("std-truststore.jks");
    }

    private static KeyStore standardKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        return loadKeystore("std-keystore.jks");
    }

    private static KeyStore loadKeystore(String file) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore store = KeyStore.getInstance("JKS");
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(getFile(file)))) {
            store.load(in, password());
        }
        return store;
    }

    private static char[] password() {
        return "password".toCharArray();
    }
}
