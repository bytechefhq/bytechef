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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.evaluator.ExpressionEvaluationException;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.dto.ClusterElementOutputDTO;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
@ActiveProfiles("testint")
@PlatformConfigurationIntTestConfigurationSharedMocks
@SpringBootTest(classes = PlatformConfigurationIntTestConfiguration.class)
class WorkflowNodeOutputFacadeIntTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String UNPARSEABLE_FORMULA = "=range(1, config.linesPerRow";

    private static final String CLUSTER_ELEMENT_DEFINITION = """
        {
            "tasks": [
                {
                    "name": "aiAgent_1",
                    "type": "aiAgent/v1/chat",
                    "parameters": {},
                    "clusterElements": {
                        "tools": [
                            {"name": "hubspot_1", "type": "hubspot/v1/createContact", "parameters": {}}
                        ]
                    }
                }
            ]
        }
        """;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ActionDefinitionService actionDefinitionService;

    @Autowired
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Autowired
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

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
    void testGetPreviousWorkflowNodeOutputsReturnsOutputsForPreviousNodes() {
        String workflowId = createWorkflow(tasksDefinition("action1", "action2"));

        stubStaticActionOutput("action1", new OutputResponse(null, Map.of("key", "value"), null));

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action2", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).extracting(WorkflowNodeOutputDTO::workflowNodeName)
            .containsExactly("action1");
    }

    @Test
    void testGetPreviousWorkflowNodeSampleOutputsReturnsSampleOutputs() {
        String workflowId = createWorkflow(tasksDefinition("action1", "action2"));

        stubStaticActionOutput("action1", new OutputResponse(null, Map.of("key", "value"), null));

        Map<String, ?> sampleOutputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, "action2", ENVIRONMENT_ID);

        assertThat(sampleOutputs).containsOnlyKeys("action1");
        assertThat(sampleOutputs.get("action1")).isEqualTo(Map.of("key", "value"));
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsIncludesLoopTaskDispatcherOutputForSiblings() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "loop1", "type": "loop/v1", "parameters": {"iteratee": []}},
                        {"name": "action2", "type": "component/v1/action2", "parameters": {}}
                    ]
                }
                """);

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(taskDispatcherDefinition);
        when(taskDispatcherDefinitionService.isDynamicOutputDefined("loop", 1)).thenReturn(false);
        when(taskDispatcherDefinition.getOutputResponse()).thenReturn(mock(OutputResponse.class));

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action2", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).hasSize(1);

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputDTOs.getFirst();

        assertThat(workflowNodeOutputDTO.workflowNodeName()).isEqualTo("loop1");
        assertThat(workflowNodeOutputDTO.taskDispatcherDefinition()).isSameAs(taskDispatcherDefinition);
    }

    @Test
    void testNodeWhoseDynamicOutputFailsDoesNotBreakTheOutputsOfTheNodesAfterIt() {
        String workflowId = createWorkflow(tasksDefinition("action1", "action2", "action3"));

        stubStaticActionOutput("action1", new OutputResponse(null, Map.of("field1", "value1"), null));
        stubDynamicAction("action2");

        when(actionDefinitionFacade.executeOutput(eq("component"), eq(1), eq("action2"), anyMap(), anyMap()))
            .thenThrow(new IllegalStateException("Table does not have primary key column 'id': dt_0_conversations"));

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action3", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).extracting(WorkflowNodeOutputDTO::workflowNodeName)
            .containsExactly("action1", "action2");
        assertThat(workflowNodeOutputDTOs.getFirst()
            .getSampleOutput()).isNotNull();

        Map<String, ?> sampleOutputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, "action3", ENVIRONMENT_ID);

        assertThat(sampleOutputs.get("action1")).isEqualTo(Map.of("field1", "value1"));
    }

    @Test
    void testLoopWhoseItemsCannotBeEvaluatedDoesNotBreakTheOutputsOfTheNodesAfterIt() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "loop1",
                            "type": "loop/v1",
                            "parameters": {"items": "%s", "iteratee": []}
                        },
                        {"name": "action2", "type": "component/v1/action2", "parameters": {}}
                    ]
                }
                """.formatted(UNPARSEABLE_FORMULA));

        TaskDispatcherDefinition taskDispatcherDefinition = mock(TaskDispatcherDefinition.class);
        OutputResponse loopOutputResponse = mock(OutputResponse.class);

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(taskDispatcherDefinition);
        when(taskDispatcherDefinitionService.isDynamicOutputDefined("loop", 1)).thenReturn(true);
        when(taskDispatcherDefinition.getOutputResponse()).thenReturn(loopOutputResponse);

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action2", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).hasSize(1);

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputDTOs.getFirst();

        assertThat(workflowNodeOutputDTO.workflowNodeName()).isEqualTo("loop1");
        assertThat(workflowNodeOutputDTO.taskDispatcherDefinition()).isSameAs(taskDispatcherDefinition);
        assertThat(workflowNodeOutputDTO.outputResponse()).isSameAs(loopOutputResponse);
        assertThat(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(workflowId, "action2", ENVIRONMENT_ID))
            .isEmpty();

        verify(taskDispatcherDefinitionService, never()).executeOutput(any(), anyInt(), anyMap());
    }

    @Test
    void testGetWorkflowNodeOutputOfLoopThrowsExpressionEvaluationExceptionOnItemsEvaluationFailure() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "loop1",
                            "type": "loop/v1",
                            "parameters": {"items": "%s", "iteratee": []}
                        }
                    ]
                }
                """.formatted(UNPARSEABLE_FORMULA));

        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(mock(TaskDispatcherDefinition.class));
        when(taskDispatcherDefinitionService.isDynamicOutputDefined("loop", 1)).thenReturn(true);

        assertThatThrownBy(() -> workflowNodeOutputFacade.getWorkflowNodeOutput(workflowId, "loop1", ENVIRONMENT_ID))
            .isInstanceOf(ExpressionEvaluationException.class);
    }

    @Test
    void testTriggerWhoseDynamicOutputFailsDoesNotBreakTheOutputsOfTheNodesAfterIt() {
        String workflowId = createWorkflow(
            """
                {
                    "triggers": [
                        {"name": "trigger_1", "type": "dataTable/v1/recordUpdated", "parameters": {}}
                    ],
                    "tasks": [
                        {"name": "action1", "type": "component/v1/action1", "parameters": {}}
                    ]
                }
                """);

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinitionService.getTriggerDefinition("dataTable", 1, "recordUpdated"))
            .thenReturn(triggerDefinition);
        when(triggerDefinition.getOutputResponse())
            .thenReturn(new OutputResponse(null, Map.of("row", "value"), null));
        when(triggerDefinitionFacade.executeOutput(eq("dataTable"), eq(1), eq("recordUpdated"), anyMap(), any()))
            .thenThrow(new IllegalStateException("Table does not have primary key column 'id': dt_0_conversations"));

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).extracting(WorkflowNodeOutputDTO::workflowNodeName)
            .containsExactly("trigger_1");
        assertThat(workflowNodeOutputDTOs.getFirst()
            .getSampleOutput()).isEqualTo(Map.of("row", "value"));
    }

    @Test
    void testSampleOutputsCachePreventsDuplicateComputation() {
        String workflowId = createWorkflow(tasksDefinition("action1", "action2", "action3"));

        stubStaticActionOutput("action1", new OutputResponse(null, Map.of("field1", "value1"), null));
        stubDynamicAction("action2");

        when(actionDefinitionFacade.executeOutput(eq("component"), eq(1), eq("action2"), anyMap(), anyMap()))
            .thenReturn(new OutputResponse(null, Map.of("dynamic", "result"), null));

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action3", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).extracting(WorkflowNodeOutputDTO::workflowNodeName)
            .containsExactly("action1", "action2");
        assertThat(workflowNodeOutputDTOs.get(1)
            .getSampleOutput()).isEqualTo(Map.of("dynamic", "result"));

        verify(actionDefinitionService, times(2)).getActionDefinition("component", 1, "action1");
    }

    @Test
    void testSampleOutputsCacheDeduplicatesAcrossMultipleDynamicNodes() {
        String workflowId = createWorkflow(tasksDefinition("action1", "action2", "action3", "action4"));

        stubStaticActionOutput("action1", new OutputResponse(null, Map.of("f1", "v1"), null));
        stubDynamicAction("action2");
        stubDynamicAction("action3");

        when(actionDefinitionFacade.executeOutput(eq("component"), eq(1), eq("action2"), anyMap(), anyMap()))
            .thenReturn(new OutputResponse(null, Map.of("d2", "r2"), null));
        when(actionDefinitionFacade.executeOutput(eq("component"), eq(1), eq("action3"), anyMap(), anyMap()))
            .thenReturn(new OutputResponse(null, Map.of("d3", "r3"), null));

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(
            workflowId, "action4", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTOs).hasSize(3);

        // Once for the top-level walk, once while computing action2's predecessors and once while computing
        // action3's predecessors. Without the per-call sample output cache, action3's walk would recompute action2's
        // predecessors and fetch action1 a fourth time.
        verify(actionDefinitionService, times(3)).getActionDefinition("component", 1, "action1");
    }

    @Test
    void testGetPreviousWorkflowNodeOutputsReturnsEmptyListForFirstNode() {
        String workflowId = createWorkflow(tasksDefinition("action1"));

        assertThat(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(workflowId, "action1", ENVIRONMENT_ID))
            .isEmpty();
    }

    @Test
    void testGetWorkflowNodeOutputThrowsExpressionEvaluationExceptionOnParameterEvaluationFailure() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "action1", "type": "component/v1/action1", "parameters": {"value": "%s"}}
                    ]
                }
                """.formatted(UNPARSEABLE_FORMULA));

        stubDynamicAction("action1");

        assertThatThrownBy(
            () -> workflowNodeOutputFacade.getWorkflowNodeOutput(workflowId, "action1", ENVIRONMENT_ID))
                .isInstanceOf(ExpressionEvaluationException.class);

        verify(actionDefinitionFacade, never()).executeOutput(any(), anyInt(), any(), any(), any());
    }

    @Test
    void testGetClusterElementOutputMarksSavedTestOutputAsTestOutputResponse() {
        String workflowId = createWorkflow(CLUSTER_ELEMENT_DEFINITION);
        ClusterElementDefinition clusterElementDefinition = stubClusterElementDefinitions();

        saveTestOutput(workflowId, "hubspot_1", "hubspot/v1/createContact", Map.of("id", "contact-42"), false);

        ClusterElementOutputDTO clusterElementOutputDTO = workflowNodeOutputFacade.getClusterElementOutput(
            workflowId, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID);

        assertThat(clusterElementOutputDTO).isNotNull();
        assertThat(clusterElementOutputDTO.clusterElementDefinition()).isSameAs(clusterElementDefinition);
        assertThat(clusterElementOutputDTO.sampleOutput()).isEqualTo(Map.of("id", "contact-42"));
        assertThat(clusterElementOutputDTO.testOutputResponse()).isTrue();

        verify(clusterElementDefinitionFacade, never()).executeOutput(any(), anyInt(), any(), any(), any());
    }

    @Test
    void testGetClusterElementOutputDoesNotMarkDefinitionOutputAsTestOutputResponse() {
        String workflowId = createWorkflow(CLUSTER_ELEMENT_DEFINITION);
        ClusterElementDefinition clusterElementDefinition = stubClusterElementDefinitions();

        when(clusterElementDefinition.getOutputResponse())
            .thenReturn(new OutputResponse(null, Map.of("id", "sample id")));

        ClusterElementOutputDTO clusterElementOutputDTO = workflowNodeOutputFacade.getClusterElementOutput(
            workflowId, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID);

        assertThat(clusterElementOutputDTO).isNotNull();
        assertThat(clusterElementOutputDTO.sampleOutput()).isEqualTo(Map.of("id", "sample id"));
        assertThat(clusterElementOutputDTO.testOutputResponse()).isFalse();
    }

    @Test
    void testGetClusterElementOutputMarksDynamicOutputAsTestOutputResponse() {
        String workflowId = createWorkflow(CLUSTER_ELEMENT_DEFINITION);

        stubClusterElementDefinitions();

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflowId, "hubspot_1", "hubspot_1", 5L, false, ENVIRONMENT_ID);

        when(clusterElementDefinitionFacade.executeOutput(eq("hubspot"), eq(1), eq("createContact"), anyMap(), eq(5L)))
            .thenReturn(new OutputResponse(null, Map.of("id", "contact-42")));

        ClusterElementOutputDTO clusterElementOutputDTO = workflowNodeOutputFacade.getClusterElementOutput(
            workflowId, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID);

        assertThat(clusterElementOutputDTO).isNotNull();
        assertThat(clusterElementOutputDTO.sampleOutput()).isEqualTo(Map.of("id", "contact-42"));
        assertThat(clusterElementOutputDTO.testOutputResponse()).isTrue();
    }

    @Test
    void testGetWorkflowNodeOutputMarksDynamicOutputAsTestOutputResponse() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "var_1", "type": "var/v1/set", "parameters": {"value": "pero"}}
                    ]
                }
                """);

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("var", 1, "set")).thenReturn(actionDefinition);
        when(actionDefinitionService.isDynamicOutputDefined("var", 1, "set")).thenReturn(true);

        workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
            workflowId, "var_1", "connection", 7L, false, ENVIRONMENT_ID);

        when(actionDefinitionFacade.executeOutput(
            "var", 1, "set", Map.of("value", "pero"), Map.of("connection", 7L)))
                .thenReturn(new OutputResponse(null, "pero"));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "var_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.getSampleOutput()).isEqualTo("pero");
        assertThat(workflowNodeOutputDTO.testOutputResponse()).isTrue();
    }

    @Test
    void testGetWorkflowNodeOutputDoesNotMarkDefinitionOutputAsTestOutputResponse() {
        String workflowId = createWorkflow(tasksDefinition("action_1"));

        stubStaticActionOutput("action_1", new OutputResponse(null, Map.of("id", "sample id")));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "action_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.getSampleOutput()).isEqualTo(Map.of("id", "sample id"));
        assertThat(workflowNodeOutputDTO.testOutputResponse()).isFalse();
    }

    @Test
    void testGetWorkflowNodeOutputSkipsDynamicOutputWhenRequiredPropertyIsMissing() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "dataStorage_1", "type": "dataStorage/v1/getValue", "parameters": {}}
                    ]
                }
                """);

        stubDataStorageGetValue(requiredStringProperty("key", null));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "dataStorage_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.outputResponse()).isNull();
        assertThat(workflowNodeOutputDTO.testOutputResponse()).isFalse();

        verify(actionDefinitionFacade, never()).executeOutput(any(), anyInt(), any(), any(), any());
    }

    @Test
    void testGetWorkflowNodeOutputRunsDynamicOutputWhenRequiredPropertiesAreSet() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "dataStorage_1", "type": "dataStorage/v1/getValue", "parameters": {"key": "counter"}}
                    ]
                }
                """);

        stubDataStorageGetValue(requiredStringProperty("key", null));

        when(actionDefinitionFacade.executeOutput(
            eq("dataStorage"), eq(1), eq("getValue"), eq(Map.of("key", "counter")), anyMap()))
                .thenReturn(new OutputResponse(null, 42));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "dataStorage_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.getSampleOutput()).isEqualTo(42);
        assertThat(workflowNodeOutputDTO.testOutputResponse()).isTrue();
    }

    @Test
    void testGetWorkflowNodeOutputIgnoresMissingRequiredPropertyHiddenByDisplayCondition() {
        String workflowId = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "dataStorage_1", "type": "dataStorage/v1/getValue", "parameters": {"type": "NUMBER"}}
                    ]
                }
                """);

        stubDataStorageGetValue(requiredStringProperty("defaultValue", "type == 'STRING'"));

        when(actionDefinitionFacade.executeOutput(eq("dataStorage"), eq(1), eq("getValue"), anyMap(), anyMap()))
            .thenReturn(new OutputResponse(null, 42));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "dataStorage_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.getSampleOutput()).isEqualTo(42);
    }

    @Test
    void testGetWorkflowNodeOutputOfTriggerSkipsDynamicOutputWhenRequiredPropertyIsMissing() {
        String workflowId = createWorkflow(
            """
                {
                    "triggers": [
                        {"name": "trigger_1", "type": "dataTable/v1/recordUpdated", "parameters": {}}
                    ],
                    "tasks": []
                }
                """);

        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinitionService.getTriggerDefinition("dataTable", 1, "recordUpdated"))
            .thenReturn(triggerDefinition);
        doReturn(List.of(requiredStringProperty("table", null))).when(triggerDefinition)
            .getProperties();
        when(triggerDefinition.getOutputResponse())
            .thenReturn(new OutputResponse(null, Map.of("row", "value"), null));

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "trigger_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.getSampleOutput()).isEqualTo(Map.of("row", "value"));
        assertThat(workflowNodeOutputDTO.testOutputResponse()).isFalse();

        verify(triggerDefinitionFacade, never()).executeOutput(any(), anyInt(), any(), any(), any());
    }

    @Test
    void testGetClusterElementOutputReportsTruncatedSavedTestOutput() {
        String workflowId = createWorkflow(CLUSTER_ELEMENT_DEFINITION);

        stubClusterElementDefinitions();

        saveTestOutput(workflowId, "hubspot_1", "hubspot/v1/createContact", List.of(1, 2), true);

        ClusterElementOutputDTO clusterElementOutputDTO = workflowNodeOutputFacade.getClusterElementOutput(
            workflowId, "aiAgent_1", "tools", "hubspot_1", ENVIRONMENT_ID);

        assertThat(clusterElementOutputDTO).isNotNull();
        assertThat(clusterElementOutputDTO.sampleOutputTruncated()).isTrue();
        assertThat(clusterElementOutputDTO.sampleOutput()).isEqualTo(List.of(1, 2));
    }

    @Test
    void testGetWorkflowNodeOutputReportsTruncatedSavedTestOutput() {
        String workflowId = createWorkflow(tasksDefinition("action_1"));

        when(actionDefinitionService.getActionDefinition("component", 1, "action_1"))
            .thenReturn(mock(ActionDefinition.class));

        saveTestOutput(workflowId, "action_1", "component/v1/action_1", List.of(1, 2), true);

        WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
            workflowId, "action_1", ENVIRONMENT_ID);

        assertThat(workflowNodeOutputDTO).isNotNull();
        assertThat(workflowNodeOutputDTO.sampleOutputTruncated()).isTrue();
        assertThat(workflowNodeOutputDTO.testOutputResponse()).isTrue();
        assertThat(workflowNodeOutputDTO.getSampleOutput()).isEqualTo(List.of(1, 2));
    }

    private String createWorkflow(String definition) {
        Workflow workflow = workflowService.create(definition, Format.JSON, Workflow.SourceType.JDBC);

        return workflow.getId();
    }

    private static StringProperty requiredStringProperty(String name, String displayCondition) {
        ModifiableStringProperty modifiableStringProperty = ComponentDsl.string(name)
            .required(true);

        if (displayCondition != null) {
            modifiableStringProperty.displayCondition(displayCondition);
        }

        return new StringProperty(modifiableStringProperty);
    }

    private void saveTestOutput(
        String workflowId, String workflowNodeName, String type, Object sampleOutput, boolean sampleOutputTruncated) {

        BaseProperty outputSchema = sampleOutput instanceof List
            ? new ArrayProperty(ComponentDsl.array()) : new ObjectProperty(ComponentDsl.object());

        workflowNodeTestOutputService.save(
            workflowId, workflowNodeName, WorkflowNodeType.ofType(type),
            new OutputResponse(outputSchema, sampleOutput, null), sampleOutputTruncated, ENVIRONMENT_ID);
    }

    private ClusterElementDefinition stubClusterElementDefinitions() {
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinitionService.getClusterElementType("aiAgent", 1, "tools"))
            .thenReturn(new ClusterElementType("TOOLS", "tools", "Tools", true, false));
        when(clusterElementDefinitionService.getClusterElementDefinition("hubspot", 1, "createContact"))
            .thenReturn(clusterElementDefinition);

        return clusterElementDefinition;
    }

    private void stubDataStorageGetValue(StringProperty stringProperty) {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("dataStorage", 1, "getValue")).thenReturn(actionDefinition);
        doReturn(List.of(stringProperty)).when(actionDefinition)
            .getProperties();
        when(actionDefinitionService.isDynamicOutputDefined("dataStorage", 1, "getValue")).thenReturn(true);
    }

    private void stubDynamicAction(String actionName) {
        when(actionDefinitionService.getActionDefinition("component", 1, actionName))
            .thenReturn(mock(ActionDefinition.class));
        when(actionDefinitionService.isDynamicOutputDefined("component", 1, actionName)).thenReturn(true);
    }

    private void stubStaticActionOutput(String actionName, OutputResponse outputResponse) {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinitionService.getActionDefinition("component", 1, actionName)).thenReturn(actionDefinition);
        when(actionDefinition.getOutputResponse()).thenReturn(outputResponse);
    }

    private static String tasksDefinition(String... taskNames) {
        List<String> tasks = Arrays.stream(taskNames)
            .map(taskName -> """
                {"name": "%s", "type": "component/v1/%s", "parameters": {}}""".formatted(taskName, taskName))
            .toList();

        return """
            {
                "tasks": [%s]
            }
            """.formatted(String.join(", ", tasks));
    }
}
