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

package com.bytechef.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties.Oauth2;
import com.bytechef.config.ApplicationProperties.Oauth2.ResourceServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * @author Ivica Cardic
 */
class ApplicationPropertiesTest {

    @Test
    void testAuthorizationServerDisabledByDefault() {
        Oauth2 oauth2 = new Oauth2();

        assertThat(oauth2.getAuthorizationServer()
            .isEnabled()).isFalse();
    }

    @Test
    void testAuthorizationServerEnabledBinds() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(
            Map.of("bytechef.oauth2.authorization-server.enabled", "true"));

        Oauth2 oauth2 = new Binder(source)
            .bind("bytechef.oauth2", Oauth2.class)
            .get();

        assertThat(oauth2.getAuthorizationServer()
            .isEnabled()).isTrue();
    }

    @Test
    void testResourceServerEmptyByDefault() {
        Oauth2 oauth2 = new Oauth2();

        assertThat(oauth2.getResourceServer()
            .getIssuers()).isEmpty();
    }

    @Test
    void testResourceServerIssuerBinds() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.oauth2.resource-server.issuers[0].uri", "https://as.example.com",
                "bytechef.oauth2.resource-server.issuers[0].tenant-claim", "tenant_id",
                "bytechef.oauth2.resource-server.issuers[1].uri", "https://idp.customer.com",
                "bytechef.oauth2.resource-server.issuers[1].tenant-claim", "org",
                "bytechef.oauth2.resource-server.issuers[1].authorities-claim", "groups",
                "bytechef.oauth2.resource-server.issuers[1].authorities[0]", "ROLE_USER"));

        Oauth2 oauth2 = new Binder(source)
            .bind("bytechef.oauth2", Oauth2.class)
            .get();

        ResourceServer resourceServer = oauth2.getResourceServer();

        assertThat(resourceServer.getIssuers()).hasSize(2);

        ResourceServer.Issuer embeddedIssuer = resourceServer.getIssuers()
            .get(0);

        assertThat(embeddedIssuer.getUri()).isEqualTo("https://as.example.com");
        assertThat(embeddedIssuer.getTenantClaim()).isEqualTo("tenant_id");
        assertThat(embeddedIssuer.getAuthoritiesClaim()).isNull();
        assertThat(embeddedIssuer.getAuthorities()).isEmpty();

        ResourceServer.Issuer externalIssuer = resourceServer.getIssuers()
            .get(1);

        assertThat(externalIssuer.getUri()).isEqualTo("https://idp.customer.com");
        assertThat(externalIssuer.getTenantClaim()).isEqualTo("org");
        assertThat(externalIssuer.getAuthoritiesClaim()).isEqualTo("groups");
        assertThat(externalIssuer.getAuthorities()).isEqualTo(List.of("ROLE_USER"));
    }
}
