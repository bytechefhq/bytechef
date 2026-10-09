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

import com.bytechef.config.ApplicationProperties.Ai.Memory.Redis;
import com.bytechef.config.ApplicationProperties.Oauth2;
import com.bytechef.config.ApplicationProperties.Oauth2.ResourceServer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * @author Ivica Cardic
 */
class ApplicationPropertiesTest {

    @Test
    void testBindsTheAiMemoryRedisSettingsWithoutUnknownProperties() {
        MapConfigurationPropertySource mapConfigurationPropertySource = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.ai.memory.provider", "redis",
                "bytechef.ai.memory.redis.host", "redis.internal",
                "bytechef.ai.memory.redis.port", "6380",
                "bytechef.ai.memory.redis.username", "bytechef",
                "bytechef.ai.memory.redis.password", "secret",
                "bytechef.ai.memory.redis.session-key-prefix", "session:",
                "bytechef.ai.memory.session-max-archived-events", "250"));

        Binder binder = new Binder(mapConfigurationPropertySource);

        ApplicationProperties applicationProperties = binder.bind(
            "bytechef", Bindable.of(ApplicationProperties.class), new NoUnboundElementsBindHandler(BindHandler.DEFAULT))
            .get();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Memory memory = ai.getMemory();

        Redis redis = memory.getRedis();

        assertThat(memory.getProvider()).isEqualTo(ApplicationProperties.Ai.Memory.Provider.REDIS);
        assertThat(redis.getHost()).isEqualTo("redis.internal");
        assertThat(redis.getPort()).isEqualTo(6380);
        assertThat(redis.getUsername()).isEqualTo("bytechef");
        assertThat(redis.getPassword()).isEqualTo("secret");
        assertThat(redis.getSessionKeyPrefix()).isEqualTo("session:");
        assertThat(memory.getSessionMaxArchivedEvents()).isEqualTo(250);
    }

    @Test
    void testLeavesTheAiMemoryRedisPortNullUntilBound() {
        MapConfigurationPropertySource mapConfigurationPropertySource = new MapConfigurationPropertySource(
            Map.of("bytechef.ai.memory.provider", "redis"));

        Binder binder = new Binder(mapConfigurationPropertySource);

        ApplicationProperties applicationProperties = binder.bind(
            "bytechef", Bindable.of(ApplicationProperties.class))
            .get();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Memory memory = ai.getMemory();

        Redis redis = memory.getRedis();

        assertThat(redis.getPort()).isNull();
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

    @Test
    void testAllowedParentOriginsDefaultsToEmpty() {
        ApplicationProperties applicationProperties = bind(new MapPropertySource("test", Map.of()));

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        assertThat(embedded.getAllowedParentOrigins()).isEmpty();
    }

    @Test
    void testAllowedParentOriginsBindsFromProperty() {
        ApplicationProperties applicationProperties = bind(
            new MapPropertySource(
                "test",
                Map.of("bytechef.embedded.allowed-parent-origins", "https://a.example,https://b.example")));

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        assertThat(embedded.getAllowedParentOrigins()).containsExactly("https://a.example", "https://b.example");
    }

    @Test
    void testAllowedParentOriginsBindsFromEnvironmentVariable() {
        ApplicationProperties applicationProperties = bind(
            new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of("BYTECHEF_EMBEDDED_ALLOWED_PARENT_ORIGINS", "https://a.example,https://b.example")));

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        assertThat(embedded.getAllowedParentOrigins()).containsExactly("https://a.example", "https://b.example");
    }

    private static ApplicationProperties bind(PropertySource<?> propertySource) {
        MutablePropertySources propertySources = new MutablePropertySources();

        propertySources.addFirst(propertySource);

        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));

        return binder
            .bindOrCreate(
                "bytechef", Bindable.of(ApplicationProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT, new UnboundElementsSourceFilter()));
    }
}
