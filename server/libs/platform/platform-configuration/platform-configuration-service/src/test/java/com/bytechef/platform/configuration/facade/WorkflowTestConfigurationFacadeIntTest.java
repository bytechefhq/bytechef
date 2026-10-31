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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@PlatformConfigurationIntTestConfigurationSharedMocks
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
class WorkflowTestConfigurationFacadeIntTest {

    private static final long ENVIRONMENT_ID = 0;

    private static final String REQUIRED_INPUT_WORKFLOW_DEFINITION = """
        {
            "inputs": [
                {
                    "name": "data",
                    "label": "Data",
                    "type": "string",
                    "required": true
                }
            ]
        }
        """;

    private static final String OPTIONAL_INPUT_WORKFLOW_DEFINITION = """
        {
            "inputs": [
                {
                    "name": "data",
                    "label": "Data",
                    "type": "string",
                    "required": false
                }
            ]
        }
        """;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @AfterEach
    void afterEach() {
        workflowTestConfigurationRepository.deleteAll();
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testValidateInputsAcceptsNonEmptyString() {
        assertSaved(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of("data", "value"));
    }

    @Test
    void testValidateInputsAcceptsNonEmptyMap() {
        assertSaved(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of("data", Map.of("spreadsheetId", "123")));
    }

    @Test
    void testValidateInputsAcceptsNonEmptyCollection() {
        assertSaved(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of("data", List.of("a", "b")));
    }

    @Test
    void testValidateInputsRejectsEmptyString() {
        assertRejected(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of("data", ""));
    }

    @Test
    void testValidateInputsRejectsEmptyMap() {
        assertRejected(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of("data", Map.of()));
    }

    @Test
    void testValidateInputsRejectsEmptyCollection() {
        assertRejected(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of("data", List.of()));
    }

    @Test
    void testValidateInputsRejectsNullValue() {
        Map<String, Object> inputs = new HashMap<>();

        inputs.put("data", null);

        assertRejected(REQUIRED_INPUT_WORKFLOW_DEFINITION, inputs);
    }

    @Test
    void testValidateInputsRejectsMissingKey() {
        assertRejected(REQUIRED_INPUT_WORKFLOW_DEFINITION, Map.of());
    }

    @Test
    void testValidateInputsSkipsEmptyOptionalInput() {
        assertSaved(OPTIONAL_INPUT_WORKFLOW_DEFINITION, Map.of("data", ""));
    }

    private void assertRejected(String workflowDefinition, Map<String, Object> inputs) {
        Workflow workflow = workflowService.create(workflowDefinition, Format.JSON, Workflow.SourceType.JDBC);

        WorkflowTestConfiguration workflowTestConfiguration = createWorkflowTestConfiguration(
            workflow.getId(), inputs);

        assertThatThrownBy(() -> workflowTestConfigurationFacade.saveWorkflowTestConfiguration(
            workflowTestConfiguration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data");

        assertThat(workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflow.getId(), ENVIRONMENT_ID))
            .isEmpty();
    }

    private void assertSaved(String workflowDefinition, Map<String, Object> inputs) {
        Workflow workflow = workflowService.create(workflowDefinition, Format.JSON, Workflow.SourceType.JDBC);

        WorkflowTestConfiguration workflowTestConfiguration = createWorkflowTestConfiguration(
            workflow.getId(), inputs);

        workflowTestConfigurationFacade.saveWorkflowTestConfiguration(workflowTestConfiguration);

        Optional<WorkflowTestConfiguration> savedWorkflowTestConfiguration =
            workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflow.getId(), ENVIRONMENT_ID);

        assertThat(savedWorkflowTestConfiguration).hasValueSatisfying(
            curWorkflowTestConfiguration -> assertThat(curWorkflowTestConfiguration.getInputs())
                .isEqualTo(inputs));
    }

    private static WorkflowTestConfiguration createWorkflowTestConfiguration(
        String workflowId, Map<String, Object> inputs) {

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(List.of());
        workflowTestConfiguration.setEnvironmentId(ENVIRONMENT_ID);
        workflowTestConfiguration.setInputs(inputs);
        workflowTestConfiguration.setWorkflowId(workflowId);

        return workflowTestConfiguration;
    }
}
