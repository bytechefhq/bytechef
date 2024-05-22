/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class IntegrationRepositoryIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @AfterEach
    public void afterEach() {
        integrationRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integration).isEqualTo(
            OptionalUtils.get(integrationRepository.findById(Validate.notNull(integration.getId(), "id"))));
    }

    @Test
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        Integration resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration).isEqualTo(integration);

        integrationRepository.deleteById(Validate.notNull(resultIntegration.getId(), "id"));

        assertThat(integrationRepository.findById(integration.getId()))
            .isEmpty();
    }

    @Test
    public void testFindById() {
        Integration integration = integrationRepository.save(getIntegration());

        Integration resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration).isEqualTo(integration);
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationRepository.save(integration);

        assertThat(integrationRepository.findById(Validate.notNull(integration.getId(), "id")))
            .hasValue(integration);
    }

    private static Integration getIntegration() {
        return Integration.builder()
            .componentName("componentName")
            .build();
    }
}
