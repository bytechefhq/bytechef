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

package com.bytechef.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
class AuthorizationServerDefaultsTest {

    private static final String AUTHORIZATION_SERVER_ENABLED = "bytechef.oauth2.authorization-server.enabled";

    @Test
    void testAuthorizationServerIsOnInSharedDefaults() throws IOException {
        StandardEnvironment standardEnvironment = load("application-bytechef");

        assertThat(standardEnvironment.getProperty(AUTHORIZATION_SERVER_ENABLED)).isEqualTo("true");
        assertThat(standardEnvironment.getProperty("bytechef.edition")).isEqualTo("EE");
    }

    @Test
    void testDevProfileTurnsAuthorizationServerOn() throws IOException {
        StandardEnvironment standardEnvironment = load("application-dev");

        assertThat(standardEnvironment.getProperty(AUTHORIZATION_SERVER_ENABLED)).isEqualTo("true");
    }

    @Test
    void testProdProfileDoesNotSetAuthorizationServer() throws IOException {
        StandardEnvironment standardEnvironment = load("application-prod");

        assertThat(standardEnvironment.containsProperty(AUTHORIZATION_SERVER_ENABLED)).isFalse();
        assertThat(standardEnvironment.containsProperty("bytechef.oauth2.authorization-server")).isFalse();
    }

    private static StandardEnvironment load(String name) throws IOException {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
            name, new ClassPathResource("config/" + name + ".yml"));

        StandardEnvironment standardEnvironment = new StandardEnvironment();

        MutablePropertySources mutablePropertySources = standardEnvironment.getPropertySources();

        propertySources.forEach(mutablePropertySources::addLast);

        return standardEnvironment;
    }
}
