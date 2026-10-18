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

package com.bytechef.security.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin.Provider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.security.social-login", name = "enabled", havingValue = "true")
class SocialLoginOAuth2ClientConfiguration {

    @Bean
    InMemoryClientRegistrationRepository clientRegistrationRepository(ApplicationProperties applicationProperties) {
        SocialLogin socialLogin = applicationProperties.getSecurity()
            .getSocialLogin();

        List<ClientRegistration> clientRegistrations = new ArrayList<>();

        Provider google = socialLogin.getGoogle();

        if (google != null && StringUtils.hasText(google.getClientId())) {
            ClientRegistration.Builder builder = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(google.getClientId())
                .clientSecret(google.getClientSecret())
                .scope("openid", "profile", "email");

            applyEndpointOverrides(builder, google);

            clientRegistrations.add(builder.build());
        }

        Provider github = socialLogin.getGithub();

        if (github != null && StringUtils.hasText(github.getClientId())) {
            ClientRegistration.Builder builder = CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId(github.getClientId())
                .clientSecret(github.getClientSecret())
                .scope("user:email", "read:user")
                .userInfoUri("https://api.github.com/user");

            applyEndpointOverrides(builder, github);

            clientRegistrations.add(builder.build());
        }

        if (clientRegistrations.isEmpty()) {
            throw new IllegalStateException(
                "bytechef.security.social-login.enabled is true but no provider is configured: set " +
                    "bytechef.security.social-login.google.client-id and/or " +
                    "bytechef.security.social-login.github.client-id");
        }

        return new InMemoryClientRegistrationRepository(clientRegistrations);
    }

    private static void applyEndpointOverrides(ClientRegistration.Builder builder, Provider provider) {
        if (StringUtils.hasText(provider.getAuthorizationUri())) {
            builder.authorizationUri(provider.getAuthorizationUri());
        }

        if (StringUtils.hasText(provider.getIssuerUri())) {
            builder.issuerUri(provider.getIssuerUri());
        }

        if (StringUtils.hasText(provider.getJwkSetUri())) {
            builder.jwkSetUri(provider.getJwkSetUri());
        }

        if (StringUtils.hasText(provider.getTokenUri())) {
            builder.tokenUri(provider.getTokenUri());
        }

        if (StringUtils.hasText(provider.getUserInfoUri())) {
            builder.userInfoUri(provider.getUserInfoUri());
        }
    }
}
