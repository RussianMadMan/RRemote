package ru.rmm.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.rmm.server.ca.CertificateAuthority;


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
                   .antMatchers("/user/**").hasRole("USER")
                   .antMatchers("/admin/**").access("hasRole(\"ADMIN\") or hasIpAddress(\"127.0.0.1\")")
                   .and()
                   .x509()
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
