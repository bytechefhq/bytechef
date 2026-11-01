/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.repository.CustomAuditEventRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.web.authentication.CredentialAuthenticationSuccessEvent;
import com.bytechef.platform.security.web.authentication.TenantPrincipal;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CredentialUsageAuditListenerTest {

    private static final String LOGIN = "admin@localhost.com";

    private final InMemoryAuditEventRepository auditEventRepository = new InMemoryAuditEventRepository();
    private final MutableClock mutableClock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));

    @Test
    void testRepeatedUseWithinTheIntervalIsRecordedOnce() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        mutableClock.advance(Duration.ofMinutes(59));

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        assertThat(findAll()).hasSize(1);

        mutableClock.advance(Duration.ofMinutes(1));

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        assertThat(findAll()).hasSize(2);
    }

    @Test
    void testTwoApiKeysOfOneUserAreRecordedSeparately() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));
        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 8L, null));

        assertThat(findAll()).extracting(auditEvent -> auditEvent.getData()
            .get("apiKeyId"))
            .containsExactlyInAnyOrder(7L, 8L);
    }

    @Test
    void testConnectedUsersAreRecordedSeparately() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, 100L));
        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, 101L));

        assertThat(findAll()).extracting(auditEvent -> auditEvent.getData()
            .get("connectedUserId"))
            .containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    void testEnvironmentsAreRecordedSeparately() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        credentialUsageAuditListener.onApplicationEvent(createEvent(1L, 7L, null));
        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        assertThat(findAll()).extracting(auditEvent -> auditEvent.getData()
            .get("environmentId"))
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void testTenantsAreRecordedSeparately() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        TenantContext.runWithTenantId(
            "000002", () -> credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null)));
        TenantContext.runWithTenantId(
            "000003", () -> credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null)));

        assertThat(findAll()).extracting(auditEvent -> auditEvent.getData()
            .get(CustomAuditEventRepository.TENANT_ID_DATA_KEY))
            .containsExactlyInAnyOrder("000002", "000003");
    }

    @Test
    void testTenantPrincipalTakesPrecedenceOverTheCurrentTenant() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
            new TestTenantUser(LOGIN, "000004"), null, List.of());

        TenantContext.runWithTenantId(
            "000002",
            () -> credentialUsageAuditListener.onApplicationEvent(
                new CredentialAuthenticationSuccessEvent(authentication)));

        assertThat(findAll()).singleElement()
            .satisfies(auditEvent -> assertThat(auditEvent.getData())
                .containsEntry(CustomAuditEventRepository.TENANT_ID_DATA_KEY, "000004"));
    }

    @Test
    void testTokenTenantTakesPrecedenceOverTheCurrentTenant() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        Authentication authentication = new TestEmbeddedApiKeyAuthenticationToken(2L, LOGIN, 7L, null) {

            @Override
            public String getTenantId() {
                return "000005";
            }
        };

        TenantContext.runWithTenantId(
            "000002",
            () -> credentialUsageAuditListener.onApplicationEvent(
                new CredentialAuthenticationSuccessEvent(authentication)));

        assertThat(findAll()).singleElement()
            .satisfies(auditEvent -> assertThat(auditEvent.getData())
                .containsEntry(CustomAuditEventRepository.TENANT_ID_DATA_KEY, "000005"));
    }

    @Test
    void testDefaultTenantIsNotRecorded() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        TenantContext.runWithTenantId(
            TenantContext.DEFAULT_TENANT_ID,
            () -> credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null)));

        assertThat(findAll()).singleElement()
            .satisfies(auditEvent -> assertThat(auditEvent.getData())
                .doesNotContainKey(CustomAuditEventRepository.TENANT_ID_DATA_KEY));
    }

    @Test
    void testJwtCredentialIsRecordedWithoutApiKeyContext() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("opaque-subject")
            .build();

        credentialUsageAuditListener.onApplicationEvent(
            new CredentialAuthenticationSuccessEvent(new JwtAuthenticationToken(jwt, List.of(), LOGIN)));
        credentialUsageAuditListener.onApplicationEvent(
            new CredentialAuthenticationSuccessEvent(new JwtAuthenticationToken(jwt, List.of(), LOGIN)));

        List<AuditEvent> auditEvents = findAll();

        assertThat(auditEvents).hasSize(1);

        AuditEvent auditEvent = auditEvents.getFirst();

        assertThat(auditEvent.getPrincipal()).isEqualTo(LOGIN);
        assertThat(auditEvent.getData())
            .containsEntry("credentialType", "Jwt")
            .doesNotContainKeys("apiKeyId", "connectedUserId", "environmentId",
                CustomAuditEventRepository.PLATFORM_DATA_KEY);
    }

    @Test
    void testMcpApiKeyIsRecordedWithItsApiKeyAndEnvironment() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofHours(1));

        String secretKey = String.valueOf(UUID.randomUUID());

        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(Environment.STAGING);
        apiKey.setId(9L);
        apiKey.setSecretKey(secretKey);
        apiKey.setType(PlatformType.AUTOMATION);

        McpApiKeyEntity mcpApiKeyEntity = new McpApiKeyEntity(apiKey, LOGIN, List.of());

        credentialUsageAuditListener.onApplicationEvent(
            new CredentialAuthenticationSuccessEvent(
                UsernamePasswordAuthenticationToken.authenticated(mcpApiKeyEntity, null, List.of())));

        assertThat(findAll()).singleElement()
            .satisfies(auditEvent -> {
                assertThat(auditEvent.getPrincipal()).isEqualTo(LOGIN);
                assertThat(auditEvent.getData())
                    .containsEntry("apiKeyId", 9L)
                    .containsEntry("environmentId", (long) Environment.STAGING.ordinal())
                    .containsEntry(CustomAuditEventRepository.PLATFORM_DATA_KEY, PlatformType.AUTOMATION)
                    .doesNotContainValue(secretKey);
            });
    }

    @Test
    void testZeroIntervalRecordsEveryUse() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ZERO);

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));
        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        assertThat(findAll()).hasSize(2);
    }

    @Test
    void testNegativeIntervalRecordsEveryUse() {
        CredentialUsageAuditListener credentialUsageAuditListener = createListener(Duration.ofMinutes(-1));

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));
        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        assertThat(findAll()).hasSize(2);
    }

    @Test
    void testDefaultIntervalIsOneHour() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        assertThat(audit.getCredentialUsageInterval()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void testFailedWriteDoesNotFailTheRequestAndIsRetried() {
        AtomicInteger remainingFailures = new AtomicInteger(1);

        InMemoryAuditEventRepository failingAuditEventRepository = new InMemoryAuditEventRepository() {

            @Override
            public void add(AuditEvent auditEvent) {
                if (remainingFailures.getAndDecrement() > 0) {
                    throw new IllegalStateException("audit store unavailable");
                }

                auditEventRepository.add(auditEvent);
            }
        };

        CredentialUsageAuditListener credentialUsageAuditListener = createListener(
            Duration.ofHours(1), failingAuditEventRepository);

        assertThatCode(() -> credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null)))
            .doesNotThrowAnyException();

        assertThat(findAll()).isEmpty();

        credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

        assertThat(findAll()).hasSize(1);
    }

    @Test
    void testRepeatedWriteFailuresAreLoggedOncePerInterval() {
        InMemoryAuditEventRepository failingAuditEventRepository = new InMemoryAuditEventRepository() {

            @Override
            public void add(AuditEvent auditEvent) {
                throw new IllegalStateException("audit store unavailable");
            }
        };

        CredentialUsageAuditListener credentialUsageAuditListener = createListener(
            Duration.ZERO, failingAuditEventRepository);

        Logger logger = (Logger) LoggerFactory.getLogger(CredentialUsageAuditListener.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            for (int index = 0; index < 3; index++) {
                credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));
            }

            assertThat(listAppender.list).hasSize(1);

            mutableClock.advance(CredentialUsageAuditListener.FAILURE_LOG_INTERVAL);

            credentialUsageAuditListener.onApplicationEvent(createEvent(2L, 7L, null));

            assertThat(listAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .satisfiesExactly(
                    message -> assertThat(message).contains("0 similar failures suppressed"),
                    message -> assertThat(message).contains("2 similar failures suppressed"));
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    private CredentialUsageAuditListener createListener(Duration credentialUsageInterval) {
        return createListener(credentialUsageInterval, auditEventRepository);
    }

    private CredentialUsageAuditListener createListener(
        Duration credentialUsageInterval, InMemoryAuditEventRepository inMemoryAuditEventRepository) {

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        audit.setCredentialUsageInterval(credentialUsageInterval);

        return new CredentialUsageAuditListener(
            applicationProperties, inMemoryAuditEventRepository, mutableClock,
            CredentialUsageAuditListener.MAX_TRACKED_CREDENTIALS);
    }

    private static CredentialAuthenticationSuccessEvent createEvent(
        long environmentId, Long apiKeyId, Long connectedUserId) {

        Authentication authentication = new TestEmbeddedApiKeyAuthenticationToken(
            environmentId, LOGIN, apiKeyId, connectedUserId);

        return new CredentialAuthenticationSuccessEvent(authentication);
    }

    private List<AuditEvent> findAll() {
        return auditEventRepository.find(null, null, null);
    }

    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    private static final class TestTenantUser extends User implements TenantPrincipal {

        private final String tenantId;

        private TestTenantUser(String login, String tenantId) {
            super(login, "", List.of());

            this.tenantId = tenantId;
        }

        @Override
        public String getTenantId() {
            return tenantId;
        }
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
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public Clock withZone(ZoneId zoneId) {
            return this;
        }
    }
}
