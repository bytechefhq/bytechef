/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyService apiKeyService;
    private final ConnectedUserService connectedUserService;

    @SuppressFBWarnings("EI")
    public EmbeddedApiKeyAuthenticationProvider(
        ApiKeyService apiKeyService, ConnectedUserService connectedUserService) {

        this.apiKeyService = apiKeyService;
        this.connectedUserService = connectedUserService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        EmbeddedApiKeyAuthenticationToken embeddedApiKeyAuthenticationToken =
            (EmbeddedApiKeyAuthenticationToken) authentication;

        long environmentId = embeddedApiKeyAuthenticationToken.getEnvironmentId();

        if (embeddedApiKeyAuthenticationToken.getSecretKey() != null &&
            !apiKeyService.exists(embeddedApiKeyAuthenticationToken.getSecretKey(), environmentId)) {

            throw new BadCredentialsException("Invalid API key");
        }

        String externalUserId = embeddedApiKeyAuthenticationToken.getExternalUserId();

        ConnectedUser connectedUser = connectedUserService.fetchConnectedUser(externalUserId, environmentId)
            .orElseGet(() -> connectedUserService.createConnectedUser(externalUserId, environmentId));

        // Carries environmentId and externalUserId into the authenticated token. ApiKeyAuthenticationFilter stores
        // THIS token in the SecurityContext, not the converter's, so anything downstream that needs the caller's
        // environment can only get it from here -- and dropping it left the principal silently claiming ordinal 0
        // (DEVELOPMENT) for every connected user in every environment.
        return new EmbeddedApiKeyAuthenticationToken(
            environmentId, createSpringSecurityUser(externalUserId, connectedUser));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(EmbeddedApiKeyAuthenticationToken.class);
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(
        String externalUserId, ConnectedUser connectedUser) {

        if (!connectedUser.isEnabled()) {
            throw new UserNotActivatedException("Connected User " + externalUserId + " was not enabled");
        }

        return new org.springframework.security.core.userdetails.User(connectedUser.getExternalId(), "", List.of());
    }
}
