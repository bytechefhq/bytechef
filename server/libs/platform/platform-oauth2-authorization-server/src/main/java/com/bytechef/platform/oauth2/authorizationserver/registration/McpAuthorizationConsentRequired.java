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

package com.bytechef.platform.oauth2.authorizationserver.registration;

import java.util.Set;
import java.util.function.Predicate;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * @author Ivica Cardic
 */
public class McpAuthorizationConsentRequired implements Predicate<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    @Override
    public boolean test(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
        RegisteredClient registeredClient = authenticationContext.getRegisteredClient();

        if (!registeredClient.getClientSettings()
            .isRequireAuthorizationConsent()) {

            return false;
        }

        OAuth2AuthorizationRequest authorizationRequest = authenticationContext.getAuthorizationRequest();

        if (authorizationRequest == null) {
            return true;
        }

        Set<String> requestedScopes = authorizationRequest.getScopes();

        if (requestedScopes.isEmpty()) {
            return true;
        }

        OAuth2AuthorizationConsent authorizationConsent = authenticationContext.getAuthorizationConsent();

        return authorizationConsent == null || !authorizationConsent.getScopes()
            .containsAll(requestedScopes);
    }

    public static ObjectPostProcessor<OAuth2AuthorizationCodeRequestAuthenticationProvider> postProcessor() {
        return new ObjectPostProcessor<>() {

            @Override
            public <O extends OAuth2AuthorizationCodeRequestAuthenticationProvider> O postProcess(
                O authenticationProvider) {

                authenticationProvider.setAuthorizationConsentRequired(new McpAuthorizationConsentRequired());

                return authenticationProvider;
            }
        };
    }
}
