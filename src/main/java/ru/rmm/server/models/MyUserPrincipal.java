package ru.rmm.server.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;


@Data
@AllArgsConstructor
public class MyUserPrincipal implements UserDetails {
    public static MyUserPrincipal extract(Principal p){
        PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken)p;
        return (MyUserPrincipal)token.getPrincipal();
    }
    public RRemoteUser user;

    Collection<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return user.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
