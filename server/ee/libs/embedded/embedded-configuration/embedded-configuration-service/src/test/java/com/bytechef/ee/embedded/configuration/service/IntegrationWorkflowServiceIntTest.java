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

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
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
@IntegrationIntTestConfigurationSharedMocks
public class IntegrationWorkflowServiceIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private IntegrationWorkflowRepository integrationWorkflowRepository;

    @AfterEach
    public void afterEach() {
        integrationWorkflowRepository.deleteAll();
        integrationRepository.deleteAll();
    }

    @Test
    public void testAddWorkflow() {
        Integration integration = getIntegration();

        integration = integrationRepository.save(integration);

        integrationWorkflowService.addWorkflow(
            Validate.notNull(integration.getId(), "id"),
            Validate.notNull(integration.getLastIntegrationVersion(), "latVersion"),
            "workflow2");

        assertThat(
            integrationWorkflowService.getWorkflowIds(integration.getId(), integration.getLastIntegrationVersion()))
                .contains("workflow2");
    }

    @Test
    public void testPublishWorkflowKeepsThePermissionExpressionOnThePublishedVersion() {
        Integration integration = integrationRepository.save(getIntegration());

        long integrationId = Validate.notNull(integration.getId(), "id");

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, 1, "publishedWorkflow");

        integrationWorkflow = integrationWorkflowService.updatePermissionExpression(
            Validate.notNull(integrationWorkflow.getId(), "id"), "metadata['plan'] == 'pro'");

        integrationWorkflow.setIntegrationVersion(2);
        integrationWorkflow.setWorkflowId("draftWorkflow");

        integrationWorkflowService.publishWorkflow(integrationId, 1, "publishedWorkflow", integrationWorkflow);

        assertThat(integrationWorkflowService.getIntegrationWorkflows(integrationId, 1))
            .singleElement()
            .satisfies(publishedIntegrationWorkflow -> {
                assertThat(publishedIntegrationWorkflow.getWorkflowId()).isEqualTo("publishedWorkflow");
                assertThat(publishedIntegrationWorkflow.getPermissionExpression())
                    .isEqualTo("metadata['plan'] == 'pro'");
            });
        assertThat(integrationWorkflowService.getIntegrationWorkflows(integrationId, 2))
            .singleElement()
            .satisfies(draftIntegrationWorkflow -> {
                assertThat(draftIntegrationWorkflow.getWorkflowId()).isEqualTo("draftWorkflow");
                assertThat(draftIntegrationWorkflow.getPermissionExpression())
                    .isEqualTo("metadata['plan'] == 'pro'");
            });
    }

    private Integration getIntegration() {
        return Integration.builder()
            .componentName("componentName")
            .name("Name")
            .build();
    }
}
