package ru.rmm.rremote.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SSLManager {

    private static final Logger logger = LoggerFactory.getLogger(SSLManager.class);

    private static final String CA_PATH = "ca.cer";
    private static final String DEVICE_PATH = "device.pfx";
    private static final char[] password = {'1','2','3','4','5'};



    public static boolean isCACertPresent(){
        return isFilePresent(CA_PATH);
    }
    public static boolean isDeviceCertPresent(){
        return isFilePresent(DEVICE_PATH);
    }
    public static void installCACert(String path) throws IOException {
        installCACert(Files.readAllBytes(Paths.get(path)));
    }
    public static void installCACert(byte[] certificate) throws IOException {
        Files.write(Paths.get(CA_PATH), certificate);
    }

    public static void registerDevice(String name, String host) throws Exception {
        logger.debug("Registering device {} with {}", name , host);
        var keyStore = RRemoteClientService.reqisterAtRRemote(name, host, getSSLContext());
        if(keyStore == null) throw new CertificateException("Error registering certificate");
        logger.debug("Registered. Saving to file...");
        saveKeyStoreToFile(keyStore);
        logger.debug("Registration successful");
    }

    public static SSLContext getSSLContext() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(getKeyManagers(), getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static void saveKeyStoreToFile(KeyStore deviceKeyStore) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        FileOutputStream f = new FileOutputStream(DEVICE_PATH);
        deviceKeyStore.store(f, password);
    }

    private static KeyManager[] getKeyManagers() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
        String keyStorePath = DEVICE_PATH;
        if(!isFilePresent(DEVICE_PATH)){
            logger.warn("No device certificate found");
            return new KeyManager[0];
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(keyStorePath), password);
        kmf.init(keystore, password);
        return kmf.getKeyManagers();
    }

    private static TrustManager[] getTrustManagers() throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException {
        if(!isFilePresent(CA_PATH)){
            throw new FileNotFoundException("No CA cert");
        }
        String trustStorePath = CA_PATH;
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(null, null);
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        FileInputStream is = new FileInputStream (trustStorePath);
        X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
        truststore.setCertificateEntry("ca", cer);
        tmf.init(truststore);
        return tmf.getTrustManagers();
    }

    private static boolean isFilePresent(String name){
        return Files.exists(Paths.get(name));
    }
}
