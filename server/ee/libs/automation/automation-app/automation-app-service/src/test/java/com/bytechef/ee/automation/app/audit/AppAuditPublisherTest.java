/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Test for {@link AppAuditPublisher}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AppAuditPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void publishesAuditApplicationEventWithAppIdAndSystemPrincipalWhenNoUser() {
        AppAuditPublisher publisher = new AppAuditPublisher(applicationEventPublisher);

        publisher.publish(AppAuditEvent.APP_CREATED, 5L, Map.of("name", "crm"));

        ArgumentCaptor<AuditApplicationEvent> captor = ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(captor.capture());

        AuditEvent auditEvent = captor.getValue()
            .getAuditEvent();

        assertThat(auditEvent.getType()).isEqualTo("APP_CREATED");
        assertThat(auditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(auditEvent.getData()).containsEntry("appId", "5")
            .containsEntry("name", "crm");
    }
}
