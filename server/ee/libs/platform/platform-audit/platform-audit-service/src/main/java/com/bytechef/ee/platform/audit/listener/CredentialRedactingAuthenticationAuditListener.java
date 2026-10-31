/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.ee.platform.audit.repository.CustomAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.authentication.TenantPrincipal;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CredentialRedactingAuthenticationAuditListener extends AuthenticationAuditListener {

    static final String REDACTED_PRINCIPAL = "[credential]";

    private static final boolean OAUTH2_AUTHORIZATION_SERVER_PRESENT = ClassUtils.isPresent(
        "org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken",
        CredentialRedactingAuthenticationAuditListener.class.getClassLoader());

    private final ThreadLocal<String> eventTenantId = new ThreadLocal<>();

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        String tenantId = getEventTenantId(event.getAuthentication());

        if (tenantId != null) {
            eventTenantId.set(tenantId);
        }

        try {
            if (event instanceof AbstractAuthenticationFailureEvent authenticationFailureEvent &&
                isCredentialPrincipal(authenticationFailureEvent.getAuthentication())) {

                super.onApplicationEvent(new RedactedAuthenticationFailureEvent(authenticationFailureEvent));

                return;
            }

            super.onApplicationEvent(event);
        } finally {
            eventTenantId.remove();
        }
    }

    @Override
    protected void publish(AuditEvent auditEvent) {
        String tenantId = eventTenantId.get();

        if (tenantId == null) {
            super.publish(auditEvent);

            return;
        }

        Map<String, Object> data = new HashMap<>(auditEvent.getData());

        data.put(CustomAuditEventRepository.TENANT_ID_DATA_KEY, tenantId);

        super.publish(
            new AuditEvent(auditEvent.getTimestamp(), auditEvent.getPrincipal(), auditEvent.getType(), data));
    }

    private static @Nullable String getEventTenantId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof TenantPrincipal tenantPrincipal) {
            return tenantPrincipal.getTenantId();
        }

        if (OAUTH2_AUTHORIZATION_SERVER_PRESENT && authentication instanceof OAuth2ClientAuthenticationToken) {
            return TenantContext.DEFAULT_TENANT_ID;
        }

        return null;
    }

    private static boolean isCredentialPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        return principal instanceof String && Objects.equals(principal, authentication.getCredentials());
    }

    private static final class RedactedAuthenticationFailureEvent extends AbstractAuthenticationFailureEvent {

        private RedactedAuthenticationFailureEvent(AbstractAuthenticationFailureEvent authenticationFailureEvent) {
            super(redact(authenticationFailureEvent.getAuthentication()), authenticationFailureEvent.getException());
        }

        private static Authentication redact(Authentication authentication) {
            UsernamePasswordAuthenticationToken redactedAuthentication =
                UsernamePasswordAuthenticationToken.unauthenticated(REDACTED_PRINCIPAL, null);

            redactedAuthentication.setDetails(authentication.getDetails());

            return redactedAuthentication;
        }
    }
}
