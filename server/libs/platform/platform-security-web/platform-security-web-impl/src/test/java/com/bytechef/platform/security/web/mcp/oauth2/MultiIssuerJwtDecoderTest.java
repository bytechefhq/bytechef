/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * @author Ivica Cardic
 */
final class MultiIssuerJwtDecoderTest {

    private static final String ISSUER_A = "https://as.example.com";
    private static final String ISSUER_B = "https://idp.customer.com";

    private final KeyPair keyPairA = generateRsaKeyPair();
    private final KeyPair keyPairB = generateRsaKeyPair();

    @Test
    void testDecodesTokenFromFirstIssuer() {
        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(
            issuer -> perIssuerDecoder(issuer));

        String token = signToken(ISSUER_A, keyPairA);

        Jwt jwt = multiIssuerJwtDecoder.decode(token);

        assertThat(jwt.getIssuer()
            .toString()).isEqualTo(ISSUER_A);
    }

    @Test
    void testDecodesTokenFromSecondIssuer() {
        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(
            issuer -> perIssuerDecoder(issuer));

        String token = signToken(ISSUER_B, keyPairB);

        Jwt jwt = multiIssuerJwtDecoder.decode(token);

        assertThat(jwt.getIssuer()
            .toString()).isEqualTo(ISSUER_B);
    }

    @Test
    void testRejectsTokenFromUnknownIssuer() {
        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(
            issuer -> perIssuerDecoder(issuer));

        String token = signToken("https://evil.example.com", keyPairA);

        assertThatExceptionOfType(JwtException.class)
            .isThrownBy(() -> multiIssuerJwtDecoder.decode(token));
    }

    @Test
    void testRejectsTokenWithForgedSignatureForKnownIssuer() {
        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(
            issuer -> perIssuerDecoder(issuer));

        // Claims say ISSUER_A but the token is signed with issuer B's key; issuer A's decoder must reject it.
        String token = signToken(ISSUER_A, keyPairB);

        assertThatExceptionOfType(JwtException.class)
            .isThrownBy(() -> multiIssuerJwtDecoder.decode(token));
    }

    @Test
    void testRejectsUnparseableToken() {
        MultiIssuerJwtDecoder multiIssuerJwtDecoder = new MultiIssuerJwtDecoder(
            issuer -> perIssuerDecoder(issuer));

        assertThatExceptionOfType(JwtException.class)
            .isThrownBy(() -> multiIssuerJwtDecoder.decode("not-a-jwt"));
    }

    private JwtDecoder perIssuerDecoder(String issuer) {
        RSAPublicKey publicKey = switch (issuer) {
            case ISSUER_A -> (RSAPublicKey) keyPairA.getPublic();
            case ISSUER_B -> (RSAPublicKey) keyPairB.getPublic();
            default -> null;
        };

        if (publicKey == null) {
            return null;
        }

        NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
            .build();

        nimbusJwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

        return nimbusJwtDecoder;
    }

    private static String signToken(String issuer, KeyPair signingKeyPair) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("user@localhost.com")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now()
                    .plus(5, ChronoUnit.MINUTES)))
                .claim("scope", "mcp:automation")
                .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);

            signedJWT.sign(new RSASSASigner((RSAPrivateKey) signingKeyPair.getPrivate()));

            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
