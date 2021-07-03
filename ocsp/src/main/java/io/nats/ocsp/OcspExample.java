package io.nats.ocsp;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import nl.altindag.ssl.exception.CertificateParseException;
import nl.altindag.ssl.exception.GenericIOException;
import nl.altindag.ssl.util.IOUtils;
import nl.altindag.ssl.util.PemUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.X509TrustedCertificateBlock;

import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public class OcspExample
{
    private static final String KEY_STORE_PASSWORD = "kspassword";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
    private static final JcaX509CertificateConverter CERTIFICATE_CONVERTER = new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE_PROVIDER);

    public static void main(String[] args) throws Exception {
        connect(getVmWideContext());
        connect(getSiloedContext());
    }

    public static SSLContext getVmWideContext() throws NoSuchAlgorithmException, KeyManagementException {
        // enableStatusRequestExtension and checkRevocation are turned on
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", "true");

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

    public static SSLContext getSiloedContext()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, KeyManagementException
    {
        // enableStatusRequestExtension is turned on but checkRevocation is not otherwise it
        // would affect the entire system. This example builds a siloed SSLContext that
        // does not need that system wide setting.
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");

        // The example client cert and key are in pem files. This PemUtils makes a Key Manager from them.
        // You likely will have to figure out how to make your own Key Manager
        X509ExtendedKeyManager keyManager =
                PemUtils.loadIdentityMaterial(ocspClientCertPath(), ocspClientKeyPath());

        // The example ca cert and key is in a pem file. loadCertificates loads the certificates from
        // that pem file. You likely will have to figure out how to load your own certificates
        // Then add the certificates to a Key Store. You will likely have a different way to make or
        // load your own Key Store
        List<X509Certificate> certificates = loadCertificates(ocspCaCertPath());
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null, KEY_STORE_PASSWORD.toCharArray());
        for (X509Certificate certificate : certificates) {
            String alias = certificate.getSubjectX500Principal().getName();
            keyStore.setCertificateEntry(alias, certificate);
        }

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

    // ----------------------------------------------------------------------------------------------------
    // Try to connect to the server
    // ----------------------------------------------------------------------------------------------------
    public static void connect(SSLContext sslContext) {
        Options options = new Options.Builder()
                .server("tls://localhost:4222")
                .maxReconnects(0)
                .sslContext(sslContext)
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
    // Cert Utilities - These files are specific to the server being run by the developer of the example.
    //                  You will need your own way to load certs, and you may already have a Key Store.
    // ----------------------------------------------------------------------------------------------------
    private static final String OCSP_CLIENT_CERT = "client-cert.pem";
    private static final String OCSP_CLIENT_KEY = "client-key.pem";
    private static final String OCSP_CA_CERT = "ca-cert.pem";

    private static Path ocspClientCertPath() {
        return getOcspCertFilePath(OCSP_CLIENT_CERT);
    }

    private static Path ocspClientKeyPath() {
        return getOcspCertFilePath(OCSP_CLIENT_KEY);
    }

    private static Path ocspCaCertPath() {
        return getOcspCertFilePath(OCSP_CA_CERT);
    }

    private static Path getOcspCertFilePath(String fileName) {
        ClassLoader classLoader = OcspExample.class.getClassLoader();
        String spec = "nats-server/test/configs/certs/ocsp/" + fileName;
        //noinspection ConstantConditions
        return new File(classLoader.getResource(spec).getFile()).toPath();
    }

    // ----------------------------------------------------------------------------------------------------
    // Modified or copied Hakky54/sslcontext-kickstart code due to private scope access
    // ----------------------------------------------------------------------------------------------------
    private static List<X509Certificate> loadCertificates(Path... certificatePaths) {
        return loadCertificate(certificatePath -> {
            try {
                return Files.newInputStream(certificatePath, StandardOpenOption.READ);
            } catch (IOException exception) {
                throw new GenericIOException(exception);
            }
        }, certificatePaths);
    }

    private static <T> List<X509Certificate> loadCertificate(Function<T, InputStream> resourceMapper, T[] resources) {
        List<X509Certificate> certificates = new ArrayList<>();
        for (T resource : resources) {
            try(InputStream certificateStream = resourceMapper.apply(resource)) {
                certificates.addAll(parseCertificate(certificateStream));
            } catch (Exception e) {
                throw new GenericIOException(e);
            }
        }

        return Collections.unmodifiableList(certificates);
    }

    private static List<X509Certificate> parseCertificate(InputStream certificateStream) {
        String content = IOUtils.getContent(certificateStream);
        return parseCertificate(content);
    }

    private static List<X509Certificate> parseCertificate(String certContent) {
        try {
            Reader stringReader = new StringReader(certContent);
            PEMParser pemParser = new PEMParser(stringReader);
            List<X509Certificate> certificates = new ArrayList<>();

            Object object = pemParser.readObject();
            while (object != null) {
                if (object instanceof X509CertificateHolder) {
                    X509Certificate certificate = CERTIFICATE_CONVERTER.getCertificate((X509CertificateHolder) object);
                    certificates.add(certificate);
                } else if (object instanceof X509TrustedCertificateBlock) {
                    X509CertificateHolder certificateHolder = ((X509TrustedCertificateBlock) object).getCertificateHolder();
                    X509Certificate certificate = CERTIFICATE_CONVERTER.getCertificate(certificateHolder);
                    certificates.add(certificate);
                }

                object = pemParser.readObject();
            }

            pemParser.close();
            stringReader.close();

            if (certificates.isEmpty()) {
                throw new CertificateParseException("Received an unsupported certificate type");
            }

            return certificates;
        } catch (IOException | CertificateException e) {
            throw new CertificateParseException(e);
        }
    }
}
