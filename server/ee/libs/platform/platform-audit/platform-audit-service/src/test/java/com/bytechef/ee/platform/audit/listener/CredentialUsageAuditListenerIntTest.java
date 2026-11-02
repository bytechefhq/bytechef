/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.web.authentication.CredentialAuthenticationSuccessEvent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class CredentialUsageAuditListenerIntTest {

    private static final long API_KEY_ID = 7L;
    private static final long CONNECTED_USER_ID = 100L;
    private static final long ENVIRONMENT_ID = 2L;
    private static final String LOGIN = "admin@localhost.com";

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testFirstUseRecordsTheCredentialWithItsContext() {
        CredentialUsageAuditListener credentialUsageAuditListener = new CredentialUsageAuditListener(
            new ApplicationProperties(), auditEventRepository, fixedClock,
            CredentialUsageAuditListener.MAX_TRACKED_CREDENTIALS);

        credentialUsageAuditListener.onApplicationEvent(createEvent(LOGIN));

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(CredentialUsageAuditListener.CREDENTIAL_USED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isEqualTo(PlatformType.EMBEDDED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("credentialType", "EmbeddedApiKey")
            .containsEntry("apiKeyId", String.valueOf(API_KEY_ID))
            .containsEntry("connectedUserId", String.valueOf(CONNECTED_USER_ID))
            .containsEntry("environmentId", String.valueOf(ENVIRONMENT_ID))
            .doesNotContainKey("details");
    }

    @Test
    void testPublishedCredentialEventIsRecordedOnceAndNotAsALogin() {
        applicationEventPublisher.publishEvent(createEvent("published@localhost.com"));

        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents)
            .extracting(PersistentAuditEvent::getEventType)
            .containsExactly(CredentialUsageAuditListener.CREDENTIAL_USED);
    }

    private static CredentialAuthenticationSuccessEvent createEvent(String login) {
        return new CredentialAuthenticationSuccessEvent(
            new TestEmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, login, API_KEY_ID, CONNECTED_USER_ID));
    }

    private List<PersistentAuditEvent> findAll() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        return page.getContent();
    }

    private PersistentAuditEvent findSingle() {
        List<PersistentAuditEvent> persistentAuditEvents = findAll();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }
}
