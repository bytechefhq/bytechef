/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.listener;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Objects;
import org.springframework.boot.actuate.security.AuthenticationAuditListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CredentialRedactingAuthenticationAuditListener extends AuthenticationAuditListener {

    static final String REDACTED_PRINCIPAL = "[credential]";

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        if (event instanceof AbstractAuthenticationFailureEvent authenticationFailureEvent &&
            isCredentialPrincipal(authenticationFailureEvent.getAuthentication())) {

            super.onApplicationEvent(new RedactedAuthenticationFailureEvent(authenticationFailureEvent));

            return;
        }

        super.onApplicationEvent(event);
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
