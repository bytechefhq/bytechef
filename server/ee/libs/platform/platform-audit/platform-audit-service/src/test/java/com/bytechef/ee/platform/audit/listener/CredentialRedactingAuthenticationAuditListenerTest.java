/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * @version ee
 * @author Ivica Cardic
 */
class CredentialRedactingAuthenticationAuditListenerTest {

    private static final String BEARER_TOKEN = String.join(
        ".", base64Url("{\"alg\":\"RS256\"}"), base64Url("{\"sub\":\"user\"}"), base64Url("signature"));

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final CredentialRedactingAuthenticationAuditListener credentialRedactingAuthenticationAuditListener =
        new CredentialRedactingAuthenticationAuditListener();

    @BeforeEach
    void beforeEach() {
        credentialRedactingAuthenticationAuditListener.setApplicationEventPublisher(applicationEventPublisher);
    }

    @Test
    void testFailedBearerTokenAuthenticationIsRecordedWithoutTheToken() {
        credentialRedactingAuthenticationAuditListener.onApplicationEvent(
            new AuthenticationFailureBadCredentialsEvent(
                new BearerTokenAuthenticationToken(BEARER_TOKEN), new InvalidBearerTokenException("Jwt expired")));

        AuditEvent auditEvent = getPublishedAuditEvent();

        assertThat(auditEvent.getPrincipal())
            .isEqualTo(CredentialRedactingAuthenticationAuditListener.REDACTED_PRINCIPAL);
        assertThat(auditEvent.getType()).isEqualTo("AUTHENTICATION_FAILURE");
        assertThat(auditEvent.getData()).containsEntry("type", InvalidBearerTokenException.class.getName());
        assertThat(auditEvent.getData()
            .values()).noneMatch(
                value -> String.valueOf(value)
                    .contains(BEARER_TOKEN));
    }

    @Test
    void testFailedUsernamePasswordAuthenticationKeepsTheLogin() {
        credentialRedactingAuthenticationAuditListener.onApplicationEvent(
            new AuthenticationFailureBadCredentialsEvent(
                UsernamePasswordAuthenticationToken.unauthenticated("user@localhost.com", "wrong-password"),
                new BadCredentialsException("Bad credentials")));

        AuditEvent auditEvent = getPublishedAuditEvent();

        assertThat(auditEvent.getPrincipal()).isEqualTo("user@localhost.com");
        assertThat(auditEvent.getType()).isEqualTo("AUTHENTICATION_FAILURE");
    }

    private AuditEvent getPublishedAuditEvent() {
        ArgumentCaptor<AuditApplicationEvent> auditApplicationEventArgumentCaptor =
            ArgumentCaptor.forClass(AuditApplicationEvent.class);

        verify(applicationEventPublisher).publishEvent(auditApplicationEventArgumentCaptor.capture());

        AuditApplicationEvent auditApplicationEvent = auditApplicationEventArgumentCaptor.getValue();

        return auditApplicationEvent.getAuditEvent();
    }

    private static String base64Url(String value) {
        Base64.Encoder encoder = Base64.getUrlEncoder()
            .withoutPadding();

        return encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
