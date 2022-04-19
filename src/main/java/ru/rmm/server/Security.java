package ru.rmm.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.vote.AbstractAccessDecisionManager;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import ru.rmm.server.ca.CertificateAuthority;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;


@EnableWebSecurity
@Configuration
public class Security extends WebSecurityConfigurerAdapter {
    @Autowired
    CertificateAuthority ca;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if(ca.getSSLKeyStore() == null){
            http.authorizeRequests()
            // restrict all requests unless coming from localhost IP4 or IP6
            .antMatchers("/admin/*").hasIpAddress("127.0.0.1");
        }else {
            http.authorizeRequests()
                .antMatchers("/reg").permitAll()
                .antMatchers("/user/**").hasRole("USER")
                .antMatchers("/admin/**").access("hasRole(\"ADMIN\")")
                .and()
                .x509()
                .authenticationDetailsSource((AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails>) context -> {
                   X509Certificate[] certs = (X509Certificate[])context.getAttribute("javax.servlet.request.X509Certificate");
                   PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails details;
                   var clientIP = context.getRemoteAddr();
                   if(certs!= null) {
                       details = null;
                   }else{
                       if(clientIP.equals("127.0.0.1")){
                           details = new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(context, AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_ADMIN"));
                       }else {
                           details = new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(context, Collections.emptyList());
                       }
                   }
                   return details;
                })
                .subjectPrincipalRegex("(.*?)")
                .userDetailsService(userDetailsService());

       }
    }

    @Bean
    public static BeanFactoryPostProcessor removeErrorSecurityFilter() {
        return beanFactory -> ((DefaultListableBeanFactory) beanFactory).removeBeanDefinition("errorPageSecurityInterceptor");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> new User(username, "", AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
    }
}
