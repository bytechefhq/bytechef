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

package com.bytechef.platform.security.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class ApiKeyAuditPublisherTest {

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final ApiKeyAuditPublisher apiKeyAuditPublisher = new ApiKeyAuditPublisher(applicationEventPublisher);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPublishUsesCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("admin@localhost.com", ""));

        SecurityContextHolder.setContext(securityContext);

        apiKeyAuditPublisher.publish(ApiKeyAuditEvent.API_KEY_DELETED, 5L);

        AuditEvent auditEvent = captureAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(auditEvent.getType()).isEqualTo("API_KEY_DELETED");
        assertThat(auditEvent.getData()).containsExactly(Map.entry("apiKeyId", "5"));
    }

    @Test
    void testPublishUsesSystemWithoutAuthentication() {
        apiKeyAuditPublisher.publish(ApiKeyAuditEvent.API_KEY_DELETED, 5L);

        AuditEvent auditEvent = captureAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("SYSTEM");
    }

    @Test
    void testPublishFallsBackToSystemWhenPrincipalResolutionFails() {
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenThrow(new IllegalStateException("context unavailable"));

        SecurityContextHolder.setContext(securityContext);

        apiKeyAuditPublisher.publish(ApiKeyAuditEvent.API_KEY_CREATED, 5L, Map.of("name", "key"));

        AuditEvent auditEvent = captureAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("SYSTEM");
        assertThat(auditEvent.getData()).containsEntry("name", "key")
            .containsEntry("apiKeyId", "5");
    }

    private AuditEvent captureAuditEvent() {
        ArgumentCaptor<AuditApplicationEvent> auditApplicationEventArgumentCaptor =
            ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(auditApplicationEventArgumentCaptor.capture());

        AuditApplicationEvent auditApplicationEvent = auditApplicationEventArgumentCaptor.getValue();

        return auditApplicationEvent.getAuditEvent();
    }
}
