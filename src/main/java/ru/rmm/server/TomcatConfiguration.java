package ru.rmm.server;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

@Configuration
public class TomcatConfiguration {

    @Autowired
    ResourcePatternResolver resolver;

    @Bean
    public ConfigurableServletWebServerFactory servContainer(@Autowired CertificateAuthority certificateAuthority) {

        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        TomcatConnectorCustomizer tomcatConnectorCustomizer = new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {

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
                        certificateAuthority.setSLLActive(true);
                        var pass = certificateAuthority.getSSLKeyPass();
                        connector.setPort(8443);
                        connector.setScheme("https");
                        connector.setSecure(true);
                        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                        protocol.setSSLEnabled(true);
                        protocol.setKeystoreType("PKCS12");
                        protocol.setKeystoreFile(store);
                        protocol.setKeystorePass(pass);
                        protocol.setKeyAlias(certificateAuthority.sslStore);
                        protocol.setKeyPass(pass);
                        protocol.setTruststoreFile(store);
                        protocol.setTruststorePass(pass);
                    }

                }catch(Exception ex){

                }

                //client must be authenticated (the cert he sends should be in our trust store)
                /*protocol.setSSLVerifyClient(Boolean.toString(true));
                protocol.setTruststoreFile(truststorePath);
                protocol.setTruststorePass(truststorePass);
                protocol.setKeyAlias("APP");*/
            }
        };
        tomcat.addConnectorCustomizers(tomcatConnectorCustomizer);
        return tomcat;
    }
}
