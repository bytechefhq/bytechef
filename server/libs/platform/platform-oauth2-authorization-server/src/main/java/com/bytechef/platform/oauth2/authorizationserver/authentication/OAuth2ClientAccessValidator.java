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

package com.bytechef.platform.oauth2.authorizationserver.authentication;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.StringUtils;

/**
 * @author Ivica Cardic
 */
public class OAuth2ClientAccessValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientAccessValidator.class);

    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1";

    private final ObjectProvider<OAuth2ClientAccessChecker> clientAccessCheckerProvider;
    private final Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> delegate;

    @SuppressFBWarnings("EI2")
    public OAuth2ClientAccessValidator(
        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> delegate,
        ObjectProvider<OAuth2ClientAccessChecker> clientAccessCheckerProvider) {

        this.clientAccessCheckerProvider = clientAccessCheckerProvider;
        this.delegate = delegate;
    }

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
        delegate.accept(authenticationContext);

        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
            authenticationContext.getAuthentication();

        Authentication principal = (Authentication) authorizationCodeRequestAuthentication.getPrincipal();

        if (!isSignedInUser(principal)) {
            return;
        }

        RegisteredClient registeredClient = authenticationContext.getRegisteredClient();

        List<OAuth2ClientAccessChecker> clientAccessCheckers = clientAccessCheckerProvider.orderedStream()
            .toList();

        for (OAuth2ClientAccessChecker clientAccessChecker : clientAccessCheckers) {
            if (!isAllowed(clientAccessChecker, registeredClient, principal)) {
                throw createAccessDeniedException(authorizationCodeRequestAuthentication, registeredClient);
            }
        }
    }

    private static boolean isAllowed(
        OAuth2ClientAccessChecker clientAccessChecker, RegisteredClient registeredClient, Authentication principal) {

        try {
            if (!clientAccessChecker.supports(registeredClient)) {
                return true;
            }

            return clientAccessChecker.isAllowed(registeredClient, principal);
        } catch (RuntimeException exception) {
            if (log.isWarnEnabled()) {
                log.warn(
                    "OAuth2 client access checker {} failed for client {}, denying access",
                    clientAccessChecker.getClass()
                        .getName(),
                    registeredClient.getClientId(), exception);
            }

            return false;
        }
    }

    private static boolean isSignedInUser(@Nullable Authentication principal) {
        return principal != null && principal.isAuthenticated() &&
            !(principal instanceof AnonymousAuthenticationToken) &&
            !(principal instanceof OAuth2ClientAuthenticationToken);
    }

    private static OAuth2AuthorizationCodeRequestAuthenticationException createAccessDeniedException(
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication,
        RegisteredClient registeredClient) {

        String redirectUri = authorizationCodeRequestAuthentication.getRedirectUri();

        if (!StringUtils.hasText(redirectUri)) {
            Set<String> registeredRedirectUris = registeredClient.getRedirectUris();

            redirectUri = registeredRedirectUris.iterator()
                .next();
        }

        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthenticationResult =
            new OAuth2AuthorizationCodeRequestAuthenticationToken(
                authorizationCodeRequestAuthentication.getAuthorizationUri(),
                authorizationCodeRequestAuthentication.getClientId(),
                (Authentication) authorizationCodeRequestAuthentication.getPrincipal(), redirectUri,
                authorizationCodeRequestAuthentication.getState(), authorizationCodeRequestAuthentication.getScopes(),
                authorizationCodeRequestAuthentication.getAdditionalParameters());

        authorizationCodeRequestAuthenticationResult.setAuthenticated(true);

        return new OAuth2AuthorizationCodeRequestAuthenticationException(
            new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "The user may not use this client", ERROR_URI),
            authorizationCodeRequestAuthenticationResult);
    }
}
