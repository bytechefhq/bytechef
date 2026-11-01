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

package com.bytechef.platform.security.web.configurer;

import com.bytechef.platform.security.web.authentication.CredentialAuthenticationEventPublisher;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * @author Ivica Cardic
 */
public final class ApiKeyAuthenticationManagerFactory {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationManagerFactory.class);

    private ApiKeyAuthenticationManagerFactory() {
    }

    public static AuthenticationManager create(HttpSecurity http, AuthenticationProvider authenticationProvider) {
        return create(authenticationProvider, createAuthenticationEventPublisher(http));
    }

    public static AuthenticationManager create(
        AuthenticationProvider authenticationProvider,
        @Nullable AuthenticationEventPublisher authenticationEventPublisher) {

        ProviderManager providerManager = new ProviderManager(authenticationProvider);

        if (authenticationEventPublisher != null) {
            providerManager.setAuthenticationEventPublisher(authenticationEventPublisher);
        }

        return providerManager;
    }

    public static @Nullable AuthenticationEventPublisher createAuthenticationEventPublisher(HttpSecurity http) {
        ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);

        if (applicationContext == null) {
            log.warn("No ApplicationContext shared with HttpSecurity; API key authentications are not audited");

            return null;
        }

        ObjectProvider<AuthenticationEventPublisher> authenticationEventPublisherProvider =
            applicationContext.getBeanProvider(AuthenticationEventPublisher.class);

        AuthenticationEventPublisher authenticationEventPublisher = authenticationEventPublisherProvider.getIfUnique();

        if (authenticationEventPublisher == null) {
            List<AuthenticationEventPublisher> authenticationEventPublishers = authenticationEventPublisherProvider
                .stream()
                .toList();

            if (!authenticationEventPublishers.isEmpty()) {
                throw new IllegalStateException(
                    "Expected a single or primary AuthenticationEventPublisher bean, found " +
                        authenticationEventPublishers.size() + "; API key authentication failures cannot be audited");
            }

            log.warn("No AuthenticationEventPublisher bean; API key authentication failures are not audited");
        }

        return new CredentialAuthenticationEventPublisher(applicationContext, authenticationEventPublisher);
    }
}
