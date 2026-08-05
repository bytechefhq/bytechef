/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubAuditPublisherTest {

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AiHubAuditPublisher newPublisher() {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(meterRegistry);

        return new AiHubAuditPublisher(applicationEventPublisher, provider);
    }

    @Test
    void testPublishesAuditApplicationEventWithSystemPrincipalWhenUnauthenticated() {
        AiHubAuditPublisher publisher = newPublisher();

        publisher.publish(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED, Map.of("scheduleId", "42"));

        ArgumentCaptor<AuditApplicationEvent> captor = ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        AuditEvent auditEvent = captor.getValue()
            .getAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(auditEvent.getType()).isEqualTo("AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED");
        assertThat(auditEvent.getData()).containsEntry("scheduleId", "42");
    }

    @Test
    void testNullDataIsTreatedAsEmpty() {
        AiHubAuditPublisher publisher = newPublisher();

        publisher.publish(AiHubAuditEvent.AI_HUB_WORKSPACE_SETTINGS_UPDATED, null);

        ArgumentCaptor<AuditApplicationEvent> captor = ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue()
            .getAuditEvent()
            .getData()).isEmpty();
    }

    @Test
    void testPublisherFailureTicksCounter() {
        AiHubAuditPublisher publisher = newPublisher();

        doThrow(new RuntimeException("downstream blew up")).when(applicationEventPublisher)
            .publishEvent(any(AuditApplicationEvent.class));

        publisher.publish(AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED, Map.of("workspaceId", "1"));

        Counter counter = meterRegistry.find("bytechef_ai_hub_audit_failed")
            .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
