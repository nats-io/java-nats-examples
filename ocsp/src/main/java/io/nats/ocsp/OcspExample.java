package io.nats.ocsp;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import nl.altindag.ssl.exception.CertificateParseException;
import nl.altindag.ssl.exception.GenericIOException;
import nl.altindag.ssl.util.IOUtils;
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
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public class OcspExample
{
    private static final String OCSP_CLIENT_CERT = "client-cert.pem";
    private static final String OCSP_CLIENT_KEY = "client-key.pem";
    private static final String OCSP_CA_CERT = "ca-cert.pem";
    private static final String KEY_STORE_PASSWORD = "kspassword";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final BouncyCastleProvider BOUNCY_CASTLE_PROVIDER = new BouncyCastleProvider();
    private static final JcaX509CertificateConverter CERTIFICATE_CONVERTER = new JcaX509CertificateConverter().setProvider(BOUNCY_CASTLE_PROVIDER);

    public static void main(String[] args) throws Exception {
        connect(getSiloedContext());
        connect(getVmWideContext());
    }

    public static SSLContext getVmWideContext() {
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", "true");

        X509ExtendedKeyManager keyManager =
                nl.altindag.ssl.util.PemUtils.loadIdentityMaterial(ocspClientCertPath(), ocspClientKeyPath());

        X509ExtendedTrustManager trustManager =
                nl.altindag.ssl.util.PemUtils.loadTrustMaterial(ocspCaCertPath());

        nl.altindag.ssl.SSLFactory sslFactory = nl.altindag.ssl.SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .withTrustMaterial(trustManager)
                .build();

        return sslFactory.getSslContext();
    }

    public static SSLContext getSiloedContext() throws Exception {
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "true");
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");

        X509ExtendedKeyManager keyManager =
                nl.altindag.ssl.util.PemUtils.loadIdentityMaterial(ocspClientCertPath(), ocspClientKeyPath());

        List<X509Certificate> certificates = loadCertificates(ocspCaCertPath());

        KeyStore trustStore = KeyStore.getInstance(KEYSTORE_TYPE);
        trustStore.load(null, KEY_STORE_PASSWORD.toCharArray());
        for (X509Certificate certificate : certificates) {
            String alias = certificate.getSubjectX500Principal().getName();
            trustStore.setCertificateEntry(alias, certificate);
        }

        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker rc = (PKIXRevocationChecker)cpb.getRevocationChecker();
        rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.NO_FALLBACK));

        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
        pkixParams.addCertPathChecker(rc);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(new CertPathTrustManagerParameters(pkixParams));

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
            nc.publish("foo", "bar".getBytes());
            System.out.println("Connected");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Cert Utilities
    // ----------------------------------------------------------------------------------------------------
    public static Path ocspClientCertPath() {
        return getOcspCertFilePath(OCSP_CLIENT_CERT);
    }

    public static Path ocspClientKeyPath() {
        return getOcspCertFilePath(OCSP_CLIENT_KEY);
    }

    public static Path ocspCaCertPath() {
        return getOcspCertFilePath(OCSP_CA_CERT);
    }

    public static Path getOcspCertFilePath(String fileName) {
        ClassLoader classLoader = OcspExample.class.getClassLoader();
        String spec = "nats-server/test/configs/certs/ocsp/" + fileName;
        //noinspection ConstantConditions
        return new File(classLoader.getResource(spec).getFile()).toPath();
    }

    // PUBLIC VERSION From Hakky54/sslcontext-kickstart code
    public static List<X509Certificate> loadCertificates(Path... certificatePaths) {
        List<X509Certificate> certificates = loadCertificate(certificatePath -> {
            try {
                return Files.newInputStream(certificatePath, StandardOpenOption.READ);
            } catch (IOException exception) {
                throw new GenericIOException(exception);
            }
        }, certificatePaths);
        return certificates;
    }

    // From Hakky54/sslcontext-kickstart code
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

    // From Hakky54/sslcontext-kickstart code
    private static List<X509Certificate> parseCertificate(InputStream certificateStream) {
        String content = IOUtils.getContent(certificateStream);
        return parseCertificate(content);
    }

    // From Hakky54/sslcontext-kickstart code
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
