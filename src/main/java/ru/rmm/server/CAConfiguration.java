package ru.rmm.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.rmm.server.ca.CertificateAuthority;

@Configuration
public class CAConfiguration {
    @Bean
    CertificateAuthority getCertificateAuthority(){
        return new CertificateAuthority();
    }


}
