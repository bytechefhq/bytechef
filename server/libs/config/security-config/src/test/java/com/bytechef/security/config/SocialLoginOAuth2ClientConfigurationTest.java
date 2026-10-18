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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin;
import com.bytechef.config.ApplicationProperties.Security.SocialLogin.Provider;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * @author Ivica Cardic
 */
class SocialLoginOAuth2ClientConfigurationTest {

    private final SocialLoginOAuth2ClientConfiguration socialLoginOAuth2ClientConfiguration =
        new SocialLoginOAuth2ClientConfiguration();

    @Test
    void testUsesThePublicEndpointsByDefault() {
        ApplicationProperties applicationProperties = applicationProperties();
        SocialLogin socialLogin = applicationProperties.getSecurity()
            .getSocialLogin();

        socialLogin.getGoogle()
            .setClientId("google-client");
        socialLogin.getGithub()
            .setClientId("github-client");

        InMemoryClientRegistrationRepository repository =
            socialLoginOAuth2ClientConfiguration.clientRegistrationRepository(applicationProperties);

        assertThat(providerDetails(repository, "google").getIssuerUri()).isEqualTo("https://accounts.google.com");
        assertThat(providerDetails(repository, "github").getUserInfoEndpoint()
            .getUri()).isEqualTo("https://api.github.com/user");
    }

    @Test
    void testAppliesEndpointOverrides() {
        ApplicationProperties applicationProperties = applicationProperties();
        Provider github = applicationProperties.getSecurity()
            .getSocialLogin()
            .getGithub();

        github.setAuthorizationUri("https://github.acme.test/login/oauth/authorize");
        github.setClientId("github-client");
        github.setTokenUri("https://github.acme.test/login/oauth/access_token");
        github.setUserInfoUri("https://github.acme.test/api/v3/user");

        ProviderDetails providerDetails = providerDetails(
            socialLoginOAuth2ClientConfiguration.clientRegistrationRepository(applicationProperties), "github");

        assertThat(providerDetails.getAuthorizationUri()).isEqualTo("https://github.acme.test/login/oauth/authorize");
        assertThat(providerDetails.getTokenUri()).isEqualTo("https://github.acme.test/login/oauth/access_token");
        assertThat(providerDetails.getUserInfoEndpoint()
            .getUri()).isEqualTo("https://github.acme.test/api/v3/user");
    }

    @Test
    void testRefusesToStartWithoutAnyProvider() {
        assertThatThrownBy(
            () -> socialLoginOAuth2ClientConfiguration.clientRegistrationRepository(applicationProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no provider is configured");
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setSecurity(new Security());

        return applicationProperties;
    }

    private static ProviderDetails providerDetails(
        InMemoryClientRegistrationRepository repository, String registrationId) {

        ClientRegistration clientRegistration = Objects.requireNonNull(
            repository.findByRegistrationId(registrationId), registrationId);

        return clientRegistration.getProviderDetails();
    }
}
