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

package com.bytechef.platform.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionImpl;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(ObjectMapperSetupExtension.class)
class OAuth2AuthorizationContextServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-13T10:00:00Z");
    private static final OAuth2AuthorizationContext PKCE_CONTEXT = OAuth2AuthorizationContext.ofPkce(
        "canva", 1, "verifier");
    private static final OAuth2AuthorizationContext DISCOVERY_CONTEXT = OAuth2AuthorizationContext.ofDiscovery(
        "mcpClient", 1, "verifier", new RegisteredClient("client-1", "secret-1", "client_secret_basic"),
        new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/a", "https://auth.example.com/t",
            "https://auth.example.com/r", "https://mcp.example.com/mcp", List.of(), List.of(), List.of()));

    private Encryption encryption;
    private MutableClock clock;
    private OAuth2AuthorizationContextServiceImpl service;

    @BeforeEach
    void beforeEach() {
        encryption = mock(Encryption.class);

        when(encryption.encrypt(anyString())).thenAnswer(invocation -> "v2:" + invocation.getArgument(0));
        when(encryption.decrypt(anyString())).thenAnswer(invocation -> {
            String sealed = invocation.getArgument(0);

            if (!sealed.startsWith("v2:")) {
                throw new IllegalStateException("tampered");
            }

            return sealed.substring("v2:".length());
        });

        clock = new MutableClock(NOW);
        service = new OAuth2AuthorizationContextServiceImpl(encryption, clock);

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("user@localhost.com", "n/a"));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSealAndUnsealDiscoveryContextRoundTrip() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(DISCOVERY_CONTEXT));

        OAuth2AuthorizationContext unsealed = TenantContext.callWithTenantId(
            "tenant-a", () -> service.unseal(sealed, "mcpClient"));

        assertThat(unsealed).isEqualTo(DISCOVERY_CONTEXT);
    }

    @Test
    void testUnsealRejectsTamperedContext() {
        assertInvalid(() -> service.unseal("garbage", "canva"));
    }

    @Test
    void testUnsealRejectsContextWithoutAuthenticatedEncryptionPrefixBeforeDecrypting() {
        String legacyCiphertext = "bGVnYWN5LWVjYi1jaXBoZXJ0ZXh0";

        doReturn(
            "{\"componentName\":\"canva\",\"connectionVersion\":1,\"codeVerifier\":\"verifier\","
                + "\"tenantId\":\"tenant-a\",\"principal\":\"user@localhost.com\",\"expiresAt\":1900000000}")
                    .when(encryption)
                    .decrypt(legacyCiphertext);

        assertInvalid(
            () -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(legacyCiphertext, "canva")));

        verify(encryption, never()).decrypt(legacyCiphertext);
    }

    @Test
    void testUnsealRejectsDecryptableContextWithoutExpiresAt() {
        String sealed = "v2:{\"componentName\":\"canva\",\"connectionVersion\":1,\"codeVerifier\":\"verifier\","
            + "\"tenantId\":\"tenant-a\",\"principal\":\"user@localhost.com\"}";

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsDecryptableContextWithWrongTypes() {
        String sealed = "v2:{\"componentName\":\"canva\",\"connectionVersion\":\"x\",\"codeVerifier\":5,"
            + "\"tenantId\":\"tenant-a\",\"principal\":\"user@localhost.com\",\"expiresAt\":1900000000}";

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsExpiredDiscoveryContext() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(DISCOVERY_CONTEXT));

        clock.advance(Duration.ofMinutes(10)
            .plusSeconds(1));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "mcpClient")));
    }

    @Test
    void testPkceContextStaysValidForAnHour() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        clock.advance(Duration.ofMinutes(59));

        OAuth2AuthorizationContext unsealed = TenantContext.callWithTenantId(
            "tenant-a", () -> service.unseal(sealed, "canva"));

        assertThat(unsealed).isEqualTo(PKCE_CONTEXT);

        clock.advance(Duration.ofMinutes(2));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsOtherTenant() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-b", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsOtherPrincipal() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("other@localhost.com", "n/a"));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsMissingPrincipal() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        SecurityContextHolder.clearContext();

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "canva")));
    }

    @Test
    void testUnsealRejectsOtherComponent() throws Exception {
        String sealed = TenantContext.callWithTenantId("tenant-a", () -> service.seal(PKCE_CONTEXT));

        assertInvalid(() -> TenantContext.callWithTenantId("tenant-a", () -> service.unseal(sealed, "mcpClient")));
    }

    @Nested
    class WithRealEncryption {

        private static final String GCM_PREFIX = "v2:";

        private OAuth2AuthorizationContextServiceImpl realEncryptionService;

        @BeforeEach
        void beforeEach() {
            realEncryptionService = new OAuth2AuthorizationContextServiceImpl(
                new EncryptionImpl(() -> "AAECAwQFBgcICQoLDA0ODw=="), clock);
        }

        @Test
        void testSealAndUnsealRoundTrip() throws Exception {
            String sealed = TenantContext.callWithTenantId(
                "tenant-a", () -> realEncryptionService.seal(DISCOVERY_CONTEXT));

            assertThat(sealed).startsWith(GCM_PREFIX);

            OAuth2AuthorizationContext unsealed = TenantContext.callWithTenantId(
                "tenant-a", () -> realEncryptionService.unseal(sealed, "mcpClient"));

            assertThat(unsealed).isEqualTo(DISCOVERY_CONTEXT);
        }

        @Test
        void testUnsealRejectsCiphertextWithOneFlippedCharacter() throws Exception {
            String sealed = TenantContext.callWithTenantId(
                "tenant-a", () -> realEncryptionService.seal(DISCOVERY_CONTEXT));

            int flippedIndex = GCM_PREFIX.length() + 20;
            char flippedCharacter = sealed.charAt(flippedIndex) == 'A' ? 'B' : 'A';

            String tampered = sealed.substring(0, flippedIndex) + flippedCharacter +
                sealed.substring(flippedIndex + 1);

            assertInvalid(
                () -> TenantContext.callWithTenantId(
                    "tenant-a", () -> realEncryptionService.unseal(tampered, "mcpClient")));
        }
    }

    private static void assertInvalid(ThrowingCallable callable) {
        Throwable throwable = catchThrowable(callable);

        if (throwable.getClass() == RuntimeException.class && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }

        assertThat(throwable).isInstanceOf(ConfigurationException.class);

        ConfigurationException configurationException = (ConfigurationException) throwable;

        assertThat(configurationException.getEntityClass()).isEqualTo(OAuth2ErrorType.class);
        assertThat(configurationException.getErrorKey())
            .isEqualTo(OAuth2ErrorType.INVALID_AUTHORIZATION_CONTEXT.getErrorKey());
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
