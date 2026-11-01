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

package com.bytechef.config.actuate.info;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Oauth2.ResourceServer.Issuer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

/**
 * @author Ivica Cardic
 */
class Oauth2AuthorizationServerContributorTest {

    @Test
    void testContributeEnabledWhenAuthorizationServerAndSelfIssuerAreConfigured() {
        assertThat(contribute(true, List.of(createIssuer(false), createIssuer(true)))).isEqualTo("true");
    }

    @Test
    void testContributeDisabledWithoutSelfIssuer() {
        assertThat(contribute(true, List.of(createIssuer(false)))).isEqualTo("false");
        assertThat(contribute(true, List.of())).isEqualTo("false");
    }

    @Test
    void testContributeDisabledWhenAuthorizationServerIsOff() {
        assertThat(contribute(false, List.of(createIssuer(true)))).isEqualTo("false");
    }

    private static Issuer createIssuer(boolean self) {
        Issuer issuer = new Issuer();

        issuer.setSelf(self);
        issuer.setUri("https://bytechef.example.com");

        return issuer;
    }

    @SuppressWarnings("unchecked")
    private static Object contribute(boolean authorizationServerEnabled, List<Issuer> issuers) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Oauth2 oauth2 = applicationProperties.getOauth2();

        ApplicationProperties.Oauth2.AuthorizationServer authorizationServer = oauth2.getAuthorizationServer();
        ApplicationProperties.Oauth2.ResourceServer resourceServer = oauth2.getResourceServer();

        authorizationServer.setEnabled(authorizationServerEnabled);
        resourceServer.setIssuers(issuers);

        Oauth2AuthorizationServerContributor oauth2AuthorizationServerContributor =
            new Oauth2AuthorizationServerContributor(applicationProperties);
        Info.Builder builder = new Info.Builder();

        oauth2AuthorizationServerContributor.contribute(builder);

        Info info = builder.build();

        Map<String, Object> oauth2Detail = (Map<String, Object>) Objects.requireNonNull(info.get("oauth2"));
        Map<String, Object> authorizationServerDetail = (Map<String, Object>) Objects.requireNonNull(
            oauth2Detail.get("authorizationServer"));

        return authorizationServerDetail.get("enabled");
    }
}
