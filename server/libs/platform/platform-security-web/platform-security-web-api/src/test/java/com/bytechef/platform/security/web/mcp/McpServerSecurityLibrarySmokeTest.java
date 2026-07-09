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

package com.bytechef.platform.security.web.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.ApiKey;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntityRepository;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationProvider;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Validates that mcp-server-security authenticates against ByteChef's intended usage pattern (opaque secret as key id,
 * {@code {noop}}-prefixed stored secret) on this project's Spring Boot version.
 *
 * @author Ivica Cardic
 */
class McpServerSecurityLibrarySmokeTest {

    private static final String SECRET_KEY = "bytechef_sk_smoke_test";

    private final ApiKeyEntityRepository<StubApiKeyEntity> apiKeyEntityRepository =
        keyId -> SECRET_KEY.equals(keyId) ? new StubApiKeyEntity(SECRET_KEY, "{noop}" + SECRET_KEY) : null;

    private final ApiKeyAuthenticationProvider<StubApiKeyEntity> apiKeyAuthenticationProvider =
        new ApiKeyAuthenticationProvider<>(apiKeyEntityRepository);

    @Test
    void testAuthenticateWithNoopEncodedSecretSucceeds() {
        Authentication authentication = apiKeyAuthenticationProvider.authenticate(
            ApiKeyAuthenticationToken.unauthenticated(new StubApiKey(SECRET_KEY)));

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void testAuthenticateWithUnknownSecretFails() {
        assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(
            () -> apiKeyAuthenticationProvider.authenticate(
                ApiKeyAuthenticationToken.unauthenticated(new StubApiKey("unknown"))));
    }

    private record StubApiKey(String secretKey) implements ApiKey {

        @Override
        public String getId() {
            return secretKey;
        }

        @Override
        public String getSecret() {
            return secretKey;
        }
    }

    private static final class StubApiKeyEntity implements ApiKeyEntity {

        private final String id;
        private String secret;

        private StubApiKeyEntity(String id, String secret) {
            this.id = id;
            this.secret = secret;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getSecret() {
            return secret;
        }

        @Override
        public List<GrantedAuthority> getAuthorities() {
            return List.of();
        }

        @Override
        public void eraseCredentials() {
            secret = null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ApiKeyEntity> T copy() {
            return (T) new StubApiKeyEntity(id, secret);
        }
    }
}
