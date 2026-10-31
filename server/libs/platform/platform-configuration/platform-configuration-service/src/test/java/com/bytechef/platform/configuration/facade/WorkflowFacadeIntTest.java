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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import java.util.List;
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
class WorkflowFacadeIntTest {

    private static final String TEST_WORKFLOW_DEFINITION = """
        {
            "label": "Test Workflow",
            "description": "Test Description",
            "tasks": []
        }
        """;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private WorkflowFacade workflowFacade;

    @Autowired
    private WorkflowService workflowService;

    @AfterEach
    void afterEach() {
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testGetWorkflow() {
        Workflow workflow = createWorkflow(TEST_WORKFLOW_DEFINITION);

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(workflow.getId());

        assertThat(workflowDTO).isNotNull();
        assertThat(workflowDTO.getId()).isEqualTo(workflow.getId());
        assertThat(workflowDTO.getLabel()).isEqualTo("Test Workflow");
        assertThat(workflowDTO.getDescription()).isEqualTo("Test Description");
        assertThat(workflowDTO.getTasks()).isEmpty();
        assertThat(workflowDTO.getTriggers()).isEmpty();
    }

    @Test
    void testGetWorkflowWithClusterRoot() {
        Workflow workflow = createWorkflow(
            """
                {
                    "label": "Test Workflow",
                    "description": "Test Description",
                    "tasks": [
                        {
                            "name": "testTask",
                            "type": "testComponent/v1/testOperation"
                        }
                    ]
                }
                """);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.isClusterRoot()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition("testComponent", 1))
            .thenReturn(Optional.of(componentDefinition));

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(workflow.getId());

        verify(componentDefinitionService, atLeastOnce()).fetchComponentDefinition("testComponent", 1);

        assertThat(workflowDTO).isNotNull();

        List<WorkflowTaskDTO> workflowTaskDTOs = workflowDTO.getTasks();

        assertThat(workflowTaskDTOs).hasSize(1);

        WorkflowTaskDTO workflowTaskDTO = workflowTaskDTOs.getFirst();

        assertThat(workflowTaskDTO.getName()).isEqualTo("testTask");
        assertThat(workflowTaskDTO.isClusterRoot()).isTrue();
    }

    @Test
    void testUpdateRejectsDuplicateNodeNamesAndDoesNotPersist() {
        Workflow workflow = createWorkflow(TEST_WORKFLOW_DEFINITION);

        String duplicateNodeNamesDefinition = """
            {
                "label": "Duplicate Workflow",
                "tasks": [
                    {
                        "name": "task_1",
                        "type": "testComponent/v1/testOperation"
                    },
                    {
                        "name": "task_1",
                        "type": "testComponent/v1/testOperation"
                    }
                ]
            }
            """;

        assertThatThrownBy(
            () -> workflowFacade.update(workflow.getId(), duplicateNodeNamesDefinition, workflow.getVersion()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("task_1");

        Workflow persistedWorkflow = workflowService.getWorkflow(workflow.getId());

        assertThat(persistedWorkflow.getLabel()).isEqualTo("Test Workflow");
        assertThat(persistedWorkflow.getTasks(true)).isEmpty();
        assertThat(persistedWorkflow.getVersion()).isEqualTo(workflow.getVersion());
    }

    @Test
    void testUpdateRejectsInvalidInputNameAndDoesNotPersist() {
        Workflow workflow = createWorkflow(TEST_WORKFLOW_DEFINITION);

        String invalidInputNameDefinition = """
            {
                "label": "Invalid Input Workflow",
                "inputs": [
                    {
                        "name": "my input",
                        "type": "string"
                    }
                ],
                "tasks": []
            }
            """;

        assertThatThrownBy(
            () -> workflowFacade.update(workflow.getId(), invalidInputNameDefinition, workflow.getVersion()))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("my input");

        Workflow persistedWorkflow = workflowService.getWorkflow(workflow.getId());

        assertThat(persistedWorkflow.getLabel()).isEqualTo("Test Workflow");
        assertThat(persistedWorkflow.getInputs()).isEmpty();
        assertThat(persistedWorkflow.getVersion()).isEqualTo(workflow.getVersion());
    }

    @Test
    void testUpdatePersistsWhenValidationPasses() {
        Workflow workflow = createWorkflow(TEST_WORKFLOW_DEFINITION);

        int version = workflow.getVersion();

        workflowFacade.update(
            workflow.getId(),
            """
                {
                    "label": "Updated Workflow",
                    "inputs": [
                        {
                            "name": "my_input",
                            "type": "string"
                        }
                    ],
                    "tasks": []
                }
                """,
            version);

        Workflow persistedWorkflow = workflowService.getWorkflow(workflow.getId());

        assertThat(persistedWorkflow.getLabel()).isEqualTo("Updated Workflow");
        assertThat(persistedWorkflow.getInputs()).hasSize(1);
        assertThat(persistedWorkflow.getVersion()).isGreaterThan(version);
    }

    private Workflow createWorkflow(String definition) {
        return workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);
    }
}
