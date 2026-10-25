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

package com.bytechef.config.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author Ivica Cardic
 */
class CommunityEditionSessionStoreEnvironmentPostProcessorTest {

    private final Log log = mock(Log.class);

    private final CommunityEditionSessionStoreEnvironmentPostProcessor communityEditionSessionStoreEnvironmentPostProcessor =
        new CommunityEditionSessionStoreEnvironmentPostProcessor(destination -> log);

    @Test
    void testLogsInfoForRedisInCe() {
        communityEditionSessionStoreEnvironmentPostProcessor.postProcessEnvironment(
            createEnvironment("ce", "redis"), new SpringApplication());

        verify(log).info("bytechef.session.store=redis requires ByteChef Enterprise Edition; using memory sessions");
    }

    @Test
    void testLogsInfoForJdbcWithUppercaseEdition() {
        communityEditionSessionStoreEnvironmentPostProcessor.postProcessEnvironment(
            createEnvironment("CE", "jdbc"), new SpringApplication());

        verify(log).info("bytechef.session.store=jdbc requires ByteChef Enterprise Edition; using memory sessions");
    }

    @Test
    void testLogsInfoWhenEditionIsMissing() {
        communityEditionSessionStoreEnvironmentPostProcessor.postProcessEnvironment(
            createEnvironment(null, "redis"), new SpringApplication());

        verify(log).info("bytechef.session.store=redis requires ByteChef Enterprise Edition; using memory sessions");
    }

    @Test
    void testSilentForMemoryStore() {
        communityEditionSessionStoreEnvironmentPostProcessor.postProcessEnvironment(
            createEnvironment("ce", "memory"), new SpringApplication());

        verifyNoInteractions(log);
    }

    @Test
    void testSilentInEe() {
        communityEditionSessionStoreEnvironmentPostProcessor.postProcessEnvironment(
            createEnvironment("EE", "redis"), new SpringApplication());

        verifyNoInteractions(log);
    }

    @Test
    void testEnvironmentIsNotChanged() {
        StandardEnvironment environment = createEnvironment("ce", "redis");

        int propertySourceCount = environment.getPropertySources()
            .size();

        communityEditionSessionStoreEnvironmentPostProcessor.postProcessEnvironment(
            environment, new SpringApplication());

        assertThat(environment.getPropertySources()
            .size()).isEqualTo(propertySourceCount);
    }

    private static StandardEnvironment createEnvironment(String edition, String store) {
        StandardEnvironment environment = new StandardEnvironment();

        Map<String, Object> properties = new HashMap<>();

        if (edition != null) {
            properties.put("bytechef.edition", edition);
        }

        properties.put("bytechef.session.store", store);

        environment.getPropertySources()
            .addLast(new MapPropertySource("application", properties));

        return environment;
    }
}
