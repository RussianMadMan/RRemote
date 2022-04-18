package ru.rmm.server.ca;

import lombok.Data;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

@Data
public class CertificateWithKey {
    public final PrivateKey privateKey;
    public final X509Certificate certificate;
}
