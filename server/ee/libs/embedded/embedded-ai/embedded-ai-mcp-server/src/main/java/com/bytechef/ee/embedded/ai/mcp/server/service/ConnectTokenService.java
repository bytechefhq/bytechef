/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.service;

import com.bytechef.commons.util.EncodingUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * Manages short-lived JWTs for MCP connection setup links. When a tool requires authentication, a JWT is generated and
 * embedded in a setup URL. The connected user can visit this URL, and the frontend decodes the JWT to get the
 * connection parameters. Uses an in-memory RSA key pair for JWT signing and stores the public key locally so the
 * authentication converter can verify tokens without going through {@code SigningKeyService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class ConnectTokenService {

    private static final String CONNECT_KEY_SUFFIX = "connect";
    private static final long JWT_TTL_MILLIS = 10 * 60 * 1000;

    private final PrivateKey privateKey;
    private final Map<String, PublicKey> publicKeys = new ConcurrentHashMap<>();
    private final PublicKey publicKey;

    public ConnectTokenService() {
        KeyPair keyPair = generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public String generateJwtToken(int environmentId, String externalUserId, long integrationId, String tenantId) {
        org.apache.commons.lang3.Validate.notBlank(externalUserId, "externalUserId");
        org.apache.commons.lang3.Validate.notBlank(tenantId, "tenantId");

        String keyId = EncodingUtils.base64EncodeToString(tenantId + ":" + CONNECT_KEY_SUFFIX);

        publicKeys.putIfAbsent(keyId, publicKey);

        return Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .subject(externalUserId)
            .claim("env", environmentId)
            .claim("integrationId", integrationId)
            .expiration(new Date(System.currentTimeMillis() + JWT_TTL_MILLIS))
            .signWith(privateKey)
            .compact();
    }

    public @Nullable PublicKey getPublicKey(String keyId) {
        return publicKeys.get(keyId);
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException(
                "Failed to generate RSA key pair for ConnectTokenService", noSuchAlgorithmException);
        }
    }
}
