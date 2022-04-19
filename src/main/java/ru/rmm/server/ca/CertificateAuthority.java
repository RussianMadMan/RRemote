package ru.rmm.server.ca;


import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import ru.rmm.server.ClientRoles;

import javax.print.DocFlavor;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class CertificateAuthority extends CertificateAuthorityBase {

    private static CertificateWithKey ca;
    public static boolean sslActive = false;

    Logger logger = LoggerFactory.getLogger(CertificateAuthority.class);

    private void loadCA() throws CAException {

        synchronized (CertificateAuthority.class){
            if(ca != null) return;
            try {
                var cacert = getCertFromStore(this.storeName);
                if(cacert == null){
                    cacert = generateCertificate(this.defaultCN, null, true, null, null);
                    saveToStore(this.storeName, cacert, true, null);

                }
                ca = cacert;
            }catch(Exception ex){
                throw new CAException("Ошибка формирования хранилища сертификатов УЦ: " + ex.getMessage(), ex);
            }
        }
    }
    public void setSLLActive(boolean bool){
        sslActive = bool;
    }
    public String getSSLKeyStore() throws CAException {
        loadCA();
        try {
            var store = getCertFromStore(this.sslStore);
            if(store == null){
                return null;
            }else{
                return getStorePath(this.sslStore);
            }

        }catch(Exception ex){
            throw new CAException("Ошибка открытия хранилища сертификатов SSL: " + ex.getMessage(), ex);
        }
    }

    public String getTrustStore() throws CAException {
        loadCA();
        try{
            var store = getCertFromStore(this.trustStore);
            if(store == null){
                return null;
            }else{
                return getStorePath(this.trustStore);
            }
        }catch(Exception ex){
            throw new CAException("Ошибка открытия хранилища доверенных сертификатов: " + ex.getMessage(), ex);
        }
    }

    public String getSSLKeyPass() {
        return this.storePassword;
    }

    public void generateSSLKeyStore(String domain) throws CAException {
        loadCA();
        try {
            var sslCert = generateCertificate(domain, domain, false, ca, null);
            saveToStore(this.sslStore, sslCert, true, ca);
            saveToStore(this.trustStore, ca, false, null);
        }catch(Exception ex){
            throw new CAException("Ошибка формирования хранилища сертиифкатов SSL: " + ex.getMessage(), ex);
        }
    }

    public String getPemCACert() {

        CharArrayWriter out = new CharArrayWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(out);
        try {
            loadCA();
            pemWriter.writeObject(ca.certificate);
            pemWriter.flush();
            pemWriter.close();
            String result = out.toString();
            out.close();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public CertificateWithKey generateCertificateAndKey(String username, ClientRoles role) throws CAException {
        loadCA();
        var stringRole = role.name();
        try {
            return generateCertificate(username, null, false, ca, stringRole);
        }catch (Exception ex){
            throw new CAException("Ошибка генерации пользавательского сертификата", ex);
        }
    }

    public byte[] storeAsPKCS12(CertificateWithKey cert) throws CAException {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            // Key store expects a load first to initialize.
            keyStore.load(null, null);
            keyStore.setKeyEntry("mycert", cert.privateKey,"12345".toCharArray(), new X509Certificate[]{cert.certificate});
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            keyStore.store(out, "12345".toCharArray());
            return out.toByteArray();
        }catch (Exception ex){
            throw new CAException("Ошибка генерации PKCS12 хранилища", ex);
        }
    }
}
