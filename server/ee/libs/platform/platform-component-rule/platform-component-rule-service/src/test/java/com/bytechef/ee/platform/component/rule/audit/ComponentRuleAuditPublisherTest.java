/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.component.rule.audit.ComponentRuleAuditPublisher.ComponentRuleAuditPayload;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ComponentRuleAuditPublisherTest {

    private static final String TEST_USER = "testuser@example.com";

    private static final ComponentRuleAuditPayload PAYLOAD = new ComponentRuleAuditPayload(
        9L, "slack", "sendMessage", "SLACK_SEND_MESSAGE", "BEFORE", 42L, 7L, null, null, false);

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ComponentRuleAuditPublisher componentRuleAuditPublisher;

    @BeforeEach
    void setUp() {
        componentRuleAuditPublisher = new ComponentRuleAuditPublisher(applicationEventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPublishCarriesTheEventTypeAndPayload() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(TEST_USER, null));

        AtomicReference<AuditApplicationEvent> capturedEvent = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedEvent.set(invocation.getArgument(0));

            return null;
        }).when(applicationEventPublisher)
            .publishEvent(any());

        componentRuleAuditPublisher.publish(ComponentRuleAuditEvent.RULE_BLOCKED, PAYLOAD);

        verify(applicationEventPublisher).publishEvent(any(AuditApplicationEvent.class));

        AuditApplicationEvent auditApplicationEvent = capturedEvent.get();

        assertThat(auditApplicationEvent).isNotNull();
        assertThat(auditApplicationEvent.getAuditEvent()
            .getType()).isEqualTo(ComponentRuleAuditEvent.RULE_BLOCKED.name());
        assertThat(auditApplicationEvent.getAuditEvent()
            .getPrincipal()).isEqualTo(TEST_USER);

        Map<String, Object> data = auditApplicationEvent.getAuditEvent()
            .getData();

        assertThat(data.get("ruleId")).isEqualTo("9");
        assertThat(data.get("componentName")).isEqualTo("slack");
        assertThat(data.get("toolName")).isEqualTo("sendMessage");
        assertThat(data.get("toolCallName")).isEqualTo("SLACK_SEND_MESSAGE");
        assertThat(data.get("phase")).isEqualTo("BEFORE");
        assertThat(data.get("jobId")).isEqualTo("42");
        assertThat(data.get("taskExecutionId")).isEqualTo("7");
    }

    @Test
    void testPublishFallsBackToSystemPrincipalWhenNoUserIsAuthenticated() {
        AtomicReference<AuditApplicationEvent> capturedEvent = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedEvent.set(invocation.getArgument(0));

            return null;
        }).when(applicationEventPublisher)
            .publishEvent(any());

        componentRuleAuditPublisher.publish(ComponentRuleAuditEvent.RULE_TAGGED, PAYLOAD);

        AuditApplicationEvent auditApplicationEvent = capturedEvent.get();

        assertThat(auditApplicationEvent).isNotNull();
        assertThat(auditApplicationEvent.getAuditEvent()
            .getPrincipal()).isEqualTo("SYSTEM");
    }

    @Test
    void testPublishDoesNotPropagateWhenEventPublishingFails() {
        doThrow(new RuntimeException("broker unavailable")).when(applicationEventPublisher)
            .publishEvent(any());

        assertThatCode(() -> componentRuleAuditPublisher.publish(ComponentRuleAuditEvent.RULE_BLOCKED, PAYLOAD))
            .doesNotThrowAnyException();
    }
}
