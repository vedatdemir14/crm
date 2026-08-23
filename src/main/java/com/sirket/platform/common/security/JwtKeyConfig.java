package com.sirket.platform.common.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * RS256 signing keys per Kriptografi ve Güvenlik Standartları §3.
 */
@Configuration
public class JwtKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyConfig.class);

    @Bean
    RSAKey rsaKey(SecurityProperties properties) {
        SecurityProperties.Jwt jwt = properties.getJwt();
        if (jwt.getPrivateKey().isBlank() || jwt.getPublicKey().isBlank()) {
            log.warn("No JWT key pair configured — generating an ephemeral one. "
                    + "Tokens will not survive a restart. Set JWT_PRIVATE_KEY / JWT_PUBLIC_KEY outside development.");
            return generateEphemeralKey();
        }
        RSAPublicKey publicKey = PemKeys.readPublicKey(jwt.getPublicKey());
        RSAPrivateKey privateKey = PemKeys.readPrivateKey(jwt.getPrivateKey());
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey rsaKey) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey rsaKey) {
        try {
            return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable to build JWT decoder from the configured key", ex);
        }
    }

    private RSAKey generateEphemeralKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        }
        catch (Exception ex) {
            throw new IllegalStateException("Unable to generate an RSA key pair", ex);
        }
    }
}
