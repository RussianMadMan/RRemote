package ru.rmm.server;

import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import ru.rmm.server.ca.CertificateAuthority;
import ru.rmm.server.models.MyUserPrincipal;
import ru.rmm.server.models.RRemoteUserRepo;

import javax.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


@EnableWebSecurity
@Configuration
public class Security extends WebSecurityConfigurerAdapter {
    @Autowired
    CertificateAuthority ca;

    @Autowired
    RRemoteUserRepo repo;

    Logger logger = LoggerFactory.getLogger(Security.class);

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if(ca.getSSLKeyStore() == null){
            http.authorizeRequests()
            .antMatchers("/admin/*").hasIpAddress("127.0.0.1");
        }else {
            http.csrf().disable().authorizeRequests()
                .antMatchers("/reg").permitAll()
                .antMatchers("/user/**").access("hasRole(\"USER\")")
                .antMatchers("/admin/**").access("hasRole(\"ADMIN\")")
                .antMatchers("/device/**").access("hasRole(\"DEVICE\")")
                .and()
                .x509()
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                .authenticationUserDetailsService(token -> {
                    var roles = getRolesFromCert((X509Certificate)token.getCredentials());
                    var username = (String)token.getPrincipal();
                    var user = repo.findByUsername(username);
                    if(user == null)
                        throw new UsernameNotFoundException(username);
                    return new MyUserPrincipal(user, roles);
                });

       }
    }


    private List<GrantedAuthority> getRolesFromCert(X509Certificate cert){
        List<GrantedAuthority> auths = null;
        try {
            var x509name = new JcaX509CertificateHolder(cert).getSubject();
            var rdns = x509name.getRDNs();
            var OUs = Arrays.stream(rdns).filter(rdn -> rdn.getFirst().getType().equals(BCStyle.OU)).collect(toList());
            if(OUs.size() == 0){
                auths = Collections.emptyList();
                logger.warn("Входящий сертификат не имел OU ({})", cert.toString());
            }else{
                var stringRole = OUs.get(0).getFirst().getValue().toString();
                try {
                    var role = ClientRoles.valueOf(stringRole);
                    auths = AuthorityUtils.commaSeparatedStringToAuthorityList(role.name());
                }catch (Exception ex){
                    logger.warn("Входящий сертификат имел OU не совместимый с ролями ({})", cert.toString());
                    auths = Collections.emptyList();
                }
            }
        }catch(Exception ex){
            logger.error("Ошибка декодирования сертификата клиента", ex);
            auths = Collections.emptyList();
        }
       return auths;
    }
}
