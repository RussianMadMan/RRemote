package ru.rmm.server.ca;


import org.springframework.context.annotation.Bean;

public class CertificateAuthority extends CertificateAuthorityBase {

    private static CertificateWithKey ca;
    public static boolean sslActive = false;


    private void loadCA() throws CAException {

        synchronized (CertificateAuthority.class){
            if(ca != null) return;
            try {
                var cacert = getCertFromStore(this.storeName);
                if(cacert == null){
                    cacert = generateCertificate(this.defaultCN, null, true, null, null);
                    saveToStore(this.storeName, cacert);
                    ca = cacert;
                }
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

    public String getSSLKeyPass() throws CAException {
        return this.storePassword;
    }

    public void generateSSLKeyStore(String domain) throws CAException {
        loadCA();
        try {
            var sslCert = generateCertificate(domain, domain, false, ca, null);
            saveToStore(this.sslStore, sslCert);
        }catch(Exception ex){
            throw new CAException("Ошибка формирования хранилища сертиифкатов SSL: " + ex.getMessage(), ex);
        }
    }
}
