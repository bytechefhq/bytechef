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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@PlatformConfigurationIntTestConfigurationSharedMocks
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
class WorkflowNodeScriptFacadeIntTest {

    private static final long ENVIRONMENT_ID = 1L;

    private static final String CLUSTER_ELEMENT_SCRIPT_WORKFLOW_DEFINITION = """
        {
            "tasks": [
                {
                    "name": "node_1",
                    "type": "dataStream/v1/sync",
                    "parameters": {},
                    "clusterElements": {
                        "processor": {
                            "name": "script_1",
                            "type": "script/v1/java",
                            "parameters": {}
                        }
                    }
                }
            ]
        }
        """;

    private static final String WORKFLOW_NODE_SCRIPT_WORKFLOW_DEFINITION = """
        {
            "tasks": [
                {
                    "name": "script_1",
                    "type": "script/v1/java",
                    "parameters": {}
                }
            ]
        }
        """;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private CodeEditorScriptInputProvider codeEditorScriptInputProvider;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private WorkflowNodeScriptFacade workflowNodeScriptFacade;

    @Autowired
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @Autowired
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationRepository workflowTestConfigurationRepository;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @AfterEach
    void afterEach() {
        workflowNodeTestOutputRepository.deleteAll();
        workflowTestConfigurationRepository.deleteAll();
        jdbcWorkflowRepository.deleteAll();
    }

    @Test
    void testTestClusterElementScriptWithNullInputParametersCallsOverloadWithoutInputParameters() {
        Workflow workflow = createWorkflow(CLUSTER_ELEMENT_SCRIPT_WORKFLOW_DEFINITION);

        stubProcessorClusterElementType();
        stubScriptPerform(Map.of("result", "ok"));

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testClusterElementScript(
            workflow.getId(), "node_1", "processor", "script_1", ENVIRONMENT_ID, null);

        assertThat(scriptTestExecutionDTO.error()).isNull();
        assertThat(scriptTestExecutionDTO.output()).isEqualTo(Map.of("result", "ok"));

        assertThat(captureScriptInputParameters()).containsEntry("input", Map.of());
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isPresent();
    }

    @Test
    void testTestClusterElementScriptWithInputParametersCallsOverloadWithInputParameters() {
        Workflow workflow = createWorkflow(CLUSTER_ELEMENT_SCRIPT_WORKFLOW_DEFINITION);

        Map<String, Object> inputParameters = Map.of("key", "value");

        stubProcessorClusterElementType();
        stubScriptPerform(Map.of("result", "ok"));

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testClusterElementScript(
            workflow.getId(), "node_1", "processor", "script_1", ENVIRONMENT_ID, inputParameters);

        assertThat(scriptTestExecutionDTO.error()).isNull();
        assertThat(scriptTestExecutionDTO.output()).isEqualTo(Map.of("result", "ok"));

        assertThat(captureScriptInputParameters()).containsEntry("input", inputParameters);
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isPresent();
    }

    @Test
    void testTestWorkflowNodeScriptWithNullInputParametersCallsOverloadWithoutInputParameters() {
        Workflow workflow = createWorkflow(WORKFLOW_NODE_SCRIPT_WORKFLOW_DEFINITION);

        stubScriptPerform(Map.of("result", "ok"));

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testWorkflowNodeScript(
            workflow.getId(), "script_1", ENVIRONMENT_ID, null);

        assertThat(scriptTestExecutionDTO.error()).isNull();
        assertThat(scriptTestExecutionDTO.output()).isEqualTo(Map.of("result", "ok"));

        assertThat(captureScriptInputParameters()).doesNotContainKey("input");
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isPresent();
    }

    @Test
    void testTestWorkflowNodeScriptWithInputParametersCallsOverloadWithInputParameters() {
        Workflow workflow = createWorkflow(WORKFLOW_NODE_SCRIPT_WORKFLOW_DEFINITION);

        Map<String, Object> inputParameters = Map.of("data", "test");

        stubScriptPerform(Map.of("result", "ok"));

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testWorkflowNodeScript(
            workflow.getId(), "script_1", ENVIRONMENT_ID, inputParameters);

        assertThat(scriptTestExecutionDTO.error()).isNull();
        assertThat(scriptTestExecutionDTO.output()).isEqualTo(Map.of("result", "ok"));

        assertThat(captureScriptInputParameters()).containsEntry("input", inputParameters);
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isPresent();
    }

    @Test
    void testTestWorkflowNodeScriptReturnsErrorOnException() {
        Workflow workflow = createWorkflow(WORKFLOW_NODE_SCRIPT_WORKFLOW_DEFINITION);

        stubScriptPerformThrows(new RuntimeException("Script execution failed"));

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testWorkflowNodeScript(
            workflow.getId(), "script_1", ENVIRONMENT_ID, null);

        assertThat(scriptTestExecutionDTO.error()).isNotNull();
        assertThat(scriptTestExecutionDTO.error()
            .getMessage()).isEqualTo("Script execution failed");
        assertThat(scriptTestExecutionDTO.output()).isNull();
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isEmpty();
    }

    @Test
    void testTestClusterElementScriptReturnsErrorOnException() {
        Workflow workflow = createWorkflow(CLUSTER_ELEMENT_SCRIPT_WORKFLOW_DEFINITION);

        stubProcessorClusterElementType();
        stubScriptPerformThrows(new RuntimeException("Element execution failed"));

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testClusterElementScript(
            workflow.getId(), "node_1", "processor", "script_1", ENVIRONMENT_ID, null);

        assertThat(scriptTestExecutionDTO.error()).isNotNull();
        assertThat(scriptTestExecutionDTO.error()
            .getMessage()).isEqualTo("Element execution failed");
        assertThat(scriptTestExecutionDTO.output()).isNull();
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isEmpty();
    }

    @Test
    void testTestWorkflowNodeScriptReturnsNullOutputWhenTestOutputIsNull() {
        Workflow workflow = createWorkflow(WORKFLOW_NODE_SCRIPT_WORKFLOW_DEFINITION);

        stubScriptPerform(null);

        ScriptTestExecutionDTO scriptTestExecutionDTO = workflowNodeScriptFacade.testWorkflowNodeScript(
            workflow.getId(), "script_1", ENVIRONMENT_ID, null);

        assertThat(scriptTestExecutionDTO.error()).isNull();
        assertThat(scriptTestExecutionDTO.output()).isNull();
        assertThat(fetchTestOutput(workflow.getId(), "script_1")).isEmpty();
    }

    @Test
    void testGetClusterElementScriptInputUsesTheSourceConnectionOfTheRootTask() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "dataStream_1",
                            "type": "dataStream/v1",
                            "parameters": {},
                            "clusterElements": {
                                "source": {"name": "postgresql_1", "type": "postgresql/v1/read", "parameters": {}}
                            }
                        }
                    ]
                }
                """);

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "dataStream_1", "script_1", 10L, false, ENVIRONMENT_ID);
        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflow.getId(), "dataStream_1", "postgresql_1", 20L, false, ENVIRONMENT_ID);

        Connection connection = mock(Connection.class);

        when(connectionService.getConnection(20L)).thenReturn(connection);
        when(connection.getId()).thenReturn(20L);
        when(codeEditorScriptInputProvider.getRootComponentName()).thenReturn("dataStream");
        when(codeEditorScriptInputProvider.getScriptInputParameters(
            anyInt(), anyString(), anyInt(), anyString(), any(), any())).thenReturn(Map.of("key", "value"));

        Map<String, Object> scriptInput = workflowNodeScriptFacade.getClusterElementScriptInput(
            workflow.getId(), "dataStream_1", "processor", "script_1", ENVIRONMENT_ID);

        ArgumentCaptor<ComponentConnection> componentConnectionArgumentCaptor =
            ArgumentCaptor.forClass(ComponentConnection.class);

        verify(codeEditorScriptInputProvider).getScriptInputParameters(
            eq(1), eq("postgresql"), eq(1), eq("read"), any(), componentConnectionArgumentCaptor.capture());

        ComponentConnection componentConnection = componentConnectionArgumentCaptor.getValue();

        assertThat(componentConnection).isNotNull();
        assertThat(componentConnection.connectionId()).isEqualTo(20L);
        assertThat(scriptInput).isEqualTo(Map.of("key", "value"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureScriptInputParameters() {
        ArgumentCaptor<Map<String, Object>> inputParametersArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(actionDefinitionFacade).executePerform(
            eq("script"), eq(1), eq("java"), any(), any(), any(), any(), any(),
            inputParametersArgumentCaptor.capture(), anyMap(), anyMap(), any(), any(), anyBoolean(), any(), any(),
            any());

        return inputParametersArgumentCaptor.getValue();
    }

    private Workflow createWorkflow(String definition) {
        return workflowService.create(definition, Format.JSON, Workflow.SourceType.JDBC);
    }

    private Optional<WorkflowNodeTestOutput> fetchTestOutput(String workflowId, String workflowNodeName) {
        return workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(workflowId, workflowNodeName, ENVIRONMENT_ID);
    }

    private void stubProcessorClusterElementType() {
        when(clusterElementDefinitionService.getClusterElementType("dataStream", 1, "PROCESSOR"))
            .thenReturn(new ClusterElementType("processor", "processor", "Processor"));
    }

    private void stubScriptPerform(Object value) {
        when(actionDefinitionFacade.executePerform(
            eq("script"), eq(1), eq("java"), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(), any(),
            any(), anyBoolean(), any(), any(), any()))
                .thenReturn(value);
    }

    private void stubScriptPerformThrows(RuntimeException exception) {
        when(actionDefinitionFacade.executePerform(
            eq("script"), eq(1), eq("java"), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(), any(),
            any(), anyBoolean(), any(), any(), any()))
                .thenThrow(exception);
    }
}
