/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.guardrails.config.AiGuardrailViolationIntTestConfiguration;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailTokenSession;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGuardrailViolationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class AiGuardrailTokenSessionRepositoryIntTest {

    @Autowired
    private AiGuardrailTokenSessionRepository aiGuardrailTokenSessionRepository;

    @Test
    public void testTheChangelogCreatesATableTheEntityCanRoundTrip() {
        AiGuardrailTokenSession saved = aiGuardrailTokenSessionRepository.save(
            new AiGuardrailTokenSession(42L, 7L, "thread-1", "abcd", "enc:{\"a\":\"b\"}"));

        assertThat(saved.getId()).isNotNull();

        AiGuardrailTokenSession reloaded = aiGuardrailTokenSessionRepository
            .findByWorkspaceIdAndUserIdAndConversationId(42L, 7L, "thread-1")
            .orElseThrow();

        assertThat(reloaded.getSessionId()).isEqualTo("abcd");
        assertThat(reloaded.getTokens()).isEqualTo("enc:{\"a\":\"b\"}");
        assertThat(reloaded.getWorkspaceId()).isEqualTo(42L);
        assertThat(reloaded.getUserId()).isEqualTo(7L);
        assertThat(reloaded.getCreatedDate()).isNotNull();
        assertThat(reloaded.getLastModifiedDate()).isNotNull();
    }

    @Test
    public void testAUserCannotHaveTwoStoredSessionsForTheSameConversation() {
        aiGuardrailTokenSessionRepository.save(
            new AiGuardrailTokenSession(11L, 21L, "thread-dup", "abcd", "enc:{}"));

        assertThatThrownBy(
            () -> aiGuardrailTokenSessionRepository.save(
                new AiGuardrailTokenSession(11L, 21L, "thread-dup", "wxyz", "enc:{}")))
                    .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testTheLastModifiedDateQueryFindsAStaleSession() {
        AiGuardrailTokenSession saved = aiGuardrailTokenSessionRepository.save(
            new AiGuardrailTokenSession(51L, 61L, "thread-ttl", "abcd", "enc:{}"));

        Instant future = Instant.now()
            .plus(1, ChronoUnit.DAYS);

        assertThat(aiGuardrailTokenSessionRepository.findByLastModifiedDateBefore(future))
            .extracting(AiGuardrailTokenSession::getId)
            .contains(saved.getId());
    }
}
