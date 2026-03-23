/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.service;

import java.security.PublicKey;
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
public interface JwtTokenService {

    String generateJwtToken(String externalUserId, long integrationId, int environmentId, String tenantId);

    @Nullable
    PublicKey getPublicKey(String keyId);
}
