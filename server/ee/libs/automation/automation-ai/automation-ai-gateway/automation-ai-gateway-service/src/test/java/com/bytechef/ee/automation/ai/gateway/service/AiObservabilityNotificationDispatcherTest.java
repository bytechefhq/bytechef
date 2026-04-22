/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertEvent;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertMetric;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRule;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityAlertRuleChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannel;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityNotificationChannelType;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityNotificationChannelRepository;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Unit tests for channel-dispatch behavior of {@link AiObservabilityNotificationDispatcher}. Webhook-delivery is
 * exercised by {@code AiObservabilityWebhookDeliveryServiceImplTest}. This class focuses on: disabled-channel skip,
 * successful-dispatch clearing {@code lastError}, and persistence of {@code lastError} when a channel throws during
 * dispatch.
 *
 * @version ee
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class AiObservabilityNotificationDispatcherTest {

    @Mock
    private AiObservabilityNotificationChannelRepository aiObservabilityNotificationChannelRepository;

    @Mock
    private JavaMailSender javaMailSender;

    private AiObservabilityNotificationDispatcher aiObservabilityNotificationDispatcher;

    @BeforeEach
    void setUp() {
        aiObservabilityNotificationDispatcher = new AiObservabilityNotificationDispatcher(
            aiObservabilityNotificationChannelRepository, javaMailSender, "no-reply@example.com");
    }

    @Test
    void testDispatchSkipsDisabledChannelWithoutSendingMail() {
        AiObservabilityNotificationChannel channel = newEmailChannel(10L);

        channel.setEnabled(false);

        when(aiObservabilityNotificationChannelRepository.findById(10L)).thenReturn(Optional.of(channel));

        AiObservabilityAlertRule rule = newRule();
        AiObservabilityAlertEvent event = new AiObservabilityAlertEvent(1L, BigDecimal.ONE, "breach");

        rule.setChannels(Set.of(new AiObservabilityAlertRuleChannel(10L)));

        aiObservabilityNotificationDispatcher.dispatch(rule, event);

        verify(javaMailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
        // Save should NOT be invoked — no prior lastError to clear, and no new failure to persist.
        verify(aiObservabilityNotificationChannelRepository, never()).save(any());
    }

    @Test
    void testDispatchClearsPriorLastErrorOnSuccessfulDelivery() throws Exception {
        AiObservabilityNotificationChannel channel = newEmailChannel(20L);

        // Pre-seed a prior failure — successful dispatch must clear it and save the channel.
        channel.setLastError("stale-failure", Instant.now());

        when(aiObservabilityNotificationChannelRepository.findById(20L)).thenReturn(Optional.of(channel));

        AiObservabilityAlertRule rule = newRule();
        AiObservabilityAlertEvent event = new AiObservabilityAlertEvent(1L, BigDecimal.ONE, "breach");

        rule.setChannels(Set.of(new AiObservabilityAlertRuleChannel(20L)));

        aiObservabilityNotificationDispatcher.dispatch(rule, event);

        assertThat(channel.getLastError()).isNull();
        assertThat(channel.getLastErrorDate()).isNull();

        verify(javaMailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(aiObservabilityNotificationChannelRepository).save(channel);
    }

    @Test
    void testDispatchPersistsLastErrorWhenChannelDispatchThrows() {
        AiObservabilityNotificationChannel channel = newEmailChannel(30L);

        when(aiObservabilityNotificationChannelRepository.findById(30L)).thenReturn(Optional.of(channel));

        // Corrupt the channel config JSON so parseChannelConfig throws before sendEmailNotification's own
        // IllegalStateException wrapping — surfaces dispatch()'s outer catch, which is the path under test.
        try {
            Field configField = AiObservabilityNotificationChannel.class.getDeclaredField("config");

            configField.setAccessible(true);
            configField.set(channel, "not-json");
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed corrupt config", reflectiveOperationException);
        }

        AiObservabilityAlertRule rule = newRule();
        AiObservabilityAlertEvent event = new AiObservabilityAlertEvent(1L, BigDecimal.ONE, "breach");

        rule.setChannels(Set.of(new AiObservabilityAlertRuleChannel(30L)));

        aiObservabilityNotificationDispatcher.dispatch(rule, event);

        // lastError should now be populated with the exception class name prefix and a timestamp.
        assertThat(channel.getLastError())
            .as("lastError should be persisted so admins can see broken integrations in the UI")
            .isNotNull();
        assertThat(channel.getLastErrorDate()).isNotNull();

        verify(aiObservabilityNotificationChannelRepository).save(channel);
    }

    private static AiObservabilityAlertRule newRule() {
        return new AiObservabilityAlertRule(
            1L, "rule", AiObservabilityAlertMetric.ERROR_RATE, AiObservabilityAlertCondition.GREATER_THAN,
            BigDecimal.valueOf(1), 5, 0);
    }

    private static AiObservabilityNotificationChannel newEmailChannel(long id) {
        AiObservabilityNotificationChannel channel = new AiObservabilityNotificationChannel(
            100L, "ops-email", AiObservabilityNotificationChannelType.EMAIL,
            "{\"recipients\":[\"ops@example.com\"]}");

        try {
            Field idField = AiObservabilityNotificationChannel.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(channel, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }

        return channel;
    }

}
