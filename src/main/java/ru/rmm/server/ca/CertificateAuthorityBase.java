package ru.rmm.server.ca;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class CertificateAuthorityBase {

    @Value("${rremote.server.ca.cn:RRemote CA}")
    public String defaultCN;

    @Value("${rremote.server.ca.castorage:}")
    public String storage;

    @Value("${rremote.server.ca.password:}")
    public String storePassword;

    @Value("${rremote.server.ca.storename:CA}")
    public String storeName;

    @Value("${rremote.server.SSLStore:SSL}")
    public String sslStore;

    @Value("${rremote.server.SSLTrustStore:TrustSSL}")
    public String trustStore;

    //https://gamlor.info/posts-output/2019-10-29-java-create-certs-bouncy/en/
    protected static CertificateWithKey generateCertificate(String cnName,
                                                            @Nullable String domain,
                                                            boolean isCA,
                                                            @Nullable CertificateWithKey issuer,
                                                            @Nullable String role)
            throws IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPair certKeyPair = keyGen.generateKeyPair();
        String subject = "CN=" + cnName;
        if(role != null){
            subject = subject + ",OU=" + role;
        }
        X500Name name = new X500Name(subject);

        BigInteger serialNumber = new BigInteger(64, new Random(System.currentTimeMillis()));
        Instant validFrom = Instant.now();
        Instant validUntil = validFrom.plus(10 * 360, ChronoUnit.DAYS);
        X500Name issuerName;
        PrivateKey issuerKey;
        if (issuer == null) {
            issuerName = name;
            issuerKey = certKeyPair.getPrivate();
        } else {
            issuerName = new X500Name(issuer.certificate.getSubjectDN().getName());
            issuerKey = issuer.privateKey;
        }
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                Date.from(validFrom), Date.from(validUntil),
                name, certKeyPair.getPublic());

        if (isCA) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCA));
        }
        if (domain != null) {
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.dNSName, domain)));
        }
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(issuerKey);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
        return new CertificateWithKey(certKeyPair.getPrivate(), cert);
    }

    protected void saveToStore(String filename, CertificateWithKey cert) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        char[] password = storePassword.toCharArray();
        String path = getStorePath(filename);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        // Key store expects a load first to initialize.
        keyStore.load(null, password);
        keyStore.setKeyEntry(filename, cert.privateKey, password, new X509Certificate[]{cert.certificate});
        try (FileOutputStream store = new FileOutputStream(path)) {
            keyStore.store(store, password);
        }
    }

    protected CertificateWithKey getCertFromStore(String filename) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException {
        char[] password = storePassword.toCharArray();
        String path = getStorePath(filename);
        File fileStore = new File(path);
        if(!fileStore.exists() || fileStore.isDirectory()){
            return null;
        }
        KeyStore keystore = KeyStore.getInstance(fileStore, password);
        X509Certificate cert = (X509Certificate)keystore.getCertificate(filename);
        PrivateKey key = (PrivateKey)keystore.getKey(filename, password);
        return new CertificateWithKey(key, cert);
    }


    protected String getStorePath(String filename){
        return  Paths.get(storage, filename + ".pfx").toString();
    }
}
