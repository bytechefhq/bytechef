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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
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
