package ru.rmm.server.models;

import lombok.Data;
import lombok.ToString;
import ru.rmm.server.ClientRoles;

import javax.persistence.*;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
public class RRemoteUser {
    @Id
    @GeneratedValue
    public long id;

    public String username;

    @Enumerated(EnumType.STRING)
    public ClientRoles type;

    @ManyToMany(fetch = FetchType.LAZY)
    @ToString.Exclude
    public Set<RRemoteUser> friends;

    @Transient
    @ToString.Exclude
    public X509Certificate currentCert;
}
