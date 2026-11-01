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

package com.bytechef.component.neon.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyFunction;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ConnectionDefinition.BaseUriFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NeonConnectionTest {

    @Test
    void testBaseUriStripsTrailingSlash() {
        BaseUriFunction baseUriFunction = NeonConnection.CONNECTION_DEFINITION.getBaseUri()
            .orElseThrow();

        Parameters connectionParameters = MockParametersFactory.create(
            Map.of("baseUri", "https://example.apirest.neon.tech/neondb/rest/v1/"));

        assertEquals(
            "https://example.apirest.neon.tech/neondb/rest/v1", baseUriFunction.apply(connectionParameters, null));
    }

    @Test
    void testBaseUriKeepsUriWithoutTrailingSlash() {
        BaseUriFunction baseUriFunction = NeonConnection.CONNECTION_DEFINITION.getBaseUri()
            .orElseThrow();

        Parameters connectionParameters = MockParametersFactory.create(
            Map.of("baseUri", "https://example.apirest.neon.tech/neondb/rest/v1"));

        assertEquals(
            "https://example.apirest.neon.tech/neondb/rest/v1", baseUriFunction.apply(connectionParameters, null));
    }

    @Test
    void testApplySelfSignedJwt() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        keyPairGenerator.initialize(2048);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getEncoder()
                .encodeToString(keyPair.getPrivate()
                    .getEncoded())
            +
            "\n-----END PRIVATE KEY-----";

        Parameters connectionParameters = MockParametersFactory.create(
            Map.of(
                "privateKey", pem,
                "subject", "service-account",
                "keyId", "key-1",
                "audience", "neon-data-api",
                "expirationSeconds", 120));

        ApplyFunction applyFunction = getAuthorization(AuthorizationType.CUSTOM).getApply()
            .orElseThrow();

        ApplyResponse applyResponse = applyFunction.apply(connectionParameters, null);

        Map<String, List<String>> headers = applyResponse.getHeaders();
        String authorizationHeader = headers.get(Authorization.AUTHORIZATION)
            .get(0);

        assertTrue(authorizationHeader.startsWith("Bearer "));

        DecodedJWT decodedJWT = JWT
            .require(Algorithm.RSA256((RSAPublicKey) keyPair.getPublic(), null))
            .build()
            .verify(authorizationHeader.substring("Bearer ".length()));

        assertEquals("service-account", decodedJWT.getSubject());
        assertEquals("key-1", decodedJWT.getKeyId());
        assertEquals(List.of("neon-data-api"), decodedJWT.getAudience());
    }

    private static Authorization getAuthorization(AuthorizationType type) {
        List<? extends Authorization> authorizations = NeonConnection.CONNECTION_DEFINITION.getAuthorizations();

        return authorizations.stream()
            .filter(authorization -> authorization.getType() == type)
            .findFirst()
            .orElseThrow();
    }
}
