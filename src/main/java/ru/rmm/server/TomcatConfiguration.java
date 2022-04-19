package ru.rmm.server;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import ru.rmm.server.ca.CertificateAuthority;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.Paths;

@Configuration
public class TomcatConfiguration {

    Logger logger = LoggerFactory.getLogger(TomcatConfiguration.class);

    @Autowired
    ResourcePatternResolver resolver;

    @Bean
    public ConfigurableServletWebServerFactory servContainer(@Autowired CertificateAuthority certificateAuthority) {

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        TomcatConnectorCustomizer tomcatConnectorCustomizer = connector -> {

            try{
                var store = certificateAuthority.getSSLKeyStore();

                if(store == null){
                    certificateAuthority.setSLLActive(false);
                    connector.setPort(8080);
                    connector.setProperty("address", "0.0.0.0");
                    connector.setScheme("http");
                    connector.setSecure(false);
                    Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                    //protocol.setAddress(InetAddress.getLocalHost());
                    protocol.setSSLEnabled(false);
                }else{
                    String userDirectory = new File("").getAbsolutePath();
                    var keystore = Paths.get(userDirectory, store);
                    var truststore = Paths.get(userDirectory, certificateAuthority.getTrustStore());
                    certificateAuthority.setSLLActive(true);
                    var pass = certificateAuthority.getSSLKeyPass();
                    connector.setPort(443);
                    connector.setScheme("https");
                    connector.setSecure(true);

                    Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                    protocol.setSSLVerifyClient("optional");
                    protocol.setSSLEnabled(true);
                    protocol.setKeystoreType("PKCS12");
                    protocol.setKeystoreFile(keystore.toString());
                    protocol.setKeystorePass(pass);
                    protocol.setKeyAlias(certificateAuthority.sslStore);
                    protocol.setKeyPass(pass);
                    protocol.setTruststoreFile(truststore.toString());
                    protocol.setTruststorePass(pass);
                    protocol.setTruststoreType("PKCS12");
                }

            }catch(Exception ex){
                logger.error("Ошибка загрузки хранилищ сертифиактов", ex);
            }

        };
        tomcat.addConnectorCustomizers(tomcatConnectorCustomizer);
        return tomcat;
    }
}
