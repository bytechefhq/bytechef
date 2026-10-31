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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfiguration;
import com.bytechef.platform.configuration.config.PlatformConfigurationIntTestConfigurationSharedMocks;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.repository.WorkflowNodeTestOutputRepository;
import com.bytechef.platform.configuration.repository.WorkflowTestConfigurationRepository;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.file.storage.EditorTempFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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
class WorkflowNodeTestOutputFacadeIntTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final int MAX_SAMPLE_ITEMS = 5;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private CodeEditorScriptInputProvider codeEditorScriptInputProvider;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EditorTempFileStorage editorTempFileStorage;

    @Autowired
    private JdbcWorkflowRepository jdbcWorkflowRepository;

    @Autowired
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Autowired
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @Autowired
    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

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
    void testSaveWorkflowNodeTestOutputKeepsOutputFunctionSchemaWhenActionReturnsNoRows() {
        Workflow workflow = createWorkflow(
            """
                {
                    "triggers": [
                        {"name": "trigger_1", "type": "manual/v1/manual", "parameters": {}}
                    ],
                    "tasks": [
                        {"name": "postgresql_1", "type": "postgresql/v1/query", "parameters": {"query": "SELECT 1"}}
                    ]
                }
                """);

        when(triggerDefinitionService.getTriggerDefinition("manual", 1, "manual"))
            .thenReturn(mock(TriggerDefinition.class));
        when(actionDefinitionFacade.executePerform(
            eq("postgresql"), eq(1), eq("query"), isNull(), isNull(), isNull(), isNull(), isNull(), anyMap(), anyMap(),
            any(), eq(ENVIRONMENT_ID), isNull(), eq(true), anyMap(), isNull(), isNull()))
                .thenReturn(List.of());
        when(actionDefinitionFacade.executeOutput(eq("postgresql"), eq(1), eq("query"), anyMap(), anyMap()))
            .thenReturn(new OutputResponse(new ArrayProperty(ComponentDsl.array("rows")), List.of()));

        workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(workflow.getId(), "postgresql_1", ENVIRONMENT_ID);

        WorkflowNodeTestOutput workflowNodeTestOutput = getTestOutput(workflow.getId(), "postgresql_1");

        OutputResponse outputResponse = workflowNodeTestOutput.getOutput(Property.class);

        assertThat(outputResponse.outputSchema()).isInstanceOf(ArrayProperty.class);
        assertThat(outputResponse.outputSchema()
            .getName()).isEqualTo("rows");
        assertThat(outputResponse.sampleOutput()).isEqualTo(List.of());
    }

    @Test
    void testSaveWorkflowNodeSampleFileStoresFileAsFileEntrySampleOutput() {
        Workflow workflow = createWorkflow(
            """
                {
                    "triggers": [
                        {"name": "trigger_1", "type": "manual/v1/manual", "parameters": {}}
                    ],
                    "tasks": [
                        {"name": "awsS3_1", "type": "awsS3/v1/getObject", "parameters": {}}
                    ]
                }
                """);

        InputStream inputStream = new ByteArrayInputStream("a,b\n1,2".getBytes(StandardCharsets.UTF_8));

        when(editorTempFileStorage.storeFileContent("sample.csv", inputStream))
            .thenReturn(new FileEntry("sample.csv", "csv", "text/csv", "file:/editor/temp/sample.csv"));

        workflowNodeTestOutputFacade.saveWorkflowNodeSampleFile(
            workflow.getId(), "awsS3_1", "sample.csv", inputStream, ENVIRONMENT_ID);

        WorkflowNodeTestOutput workflowNodeTestOutput = getTestOutput(workflow.getId(), "awsS3_1");

        OutputResponse outputResponse = workflowNodeTestOutput.getOutput(Property.class);

        assertThat(outputResponse.outputSchema()).isInstanceOf(FileEntryProperty.class);
        assertThat(outputResponse.sampleOutput()).isEqualTo(
            Map.of(
                "extension", "csv", "mimeType", "text/csv", "name", "sample.csv", "url",
                "file:/editor/temp/sample.csv"));
        assertThat(workflowNodeTestOutput.isSampleOutputTruncated()).isFalse();
    }

    @Test
    void testSaveWorkflowNodeTestOutputExecutesTriggerNamedInWorkflowExecutionId() {
        Workflow workflow = createWorkflow(
            """
                {
                    "triggers": [
                        {"name": "trigger_1", "type": "manual/v1/manual", "parameters": {}},
                        {"name": "trigger_2", "type": "webhook/v1/autoRespondWithHTTP200", "parameters": {}}
                    ],
                    "tasks": []
                }
                """);

        String workflowUuid = "workflow-uuid-" + workflow.getId();
        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
        TriggerDefinition webhookTriggerDefinition = mock(TriggerDefinition.class);
        WebhookRequest webhookRequest = mock(WebhookRequest.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getLastWorkflowId(workflowUuid)).thenReturn(workflow.getId());
        when(jobPrincipalAccessor.getWorkflowUuid(workflow.getId())).thenReturn(workflowUuid);
        when(webhookTriggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);
        when(triggerDefinitionService.getTriggerDefinition("webhook", 1, "autoRespondWithHTTP200"))
            .thenReturn(webhookTriggerDefinition);
        when(triggerDefinitionService.getWebhookTriggerFlags("webhook", 1, "autoRespondWithHTTP200"))
            .thenReturn(new WebhookTriggerFlags(false, false, false, false));
        when(triggerDefinitionFacade.executeTrigger(
            eq("webhook"), eq(1), eq("autoRespondWithHTTP200"), isNull(), eq(workflowUuid), isNull(), anyMap(),
            any(), eq(webhookRequest), isNull(), eq(ENVIRONMENT_ID), eq(PlatformType.AUTOMATION), eq(true)))
                .thenReturn(new TriggerOutput(Map.of("body", "ok"), null, false));

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, -1, workflowUuid, "trigger_2");

        webhookTriggerTestFacade.enableTrigger(workflow.getId(), "trigger_2", ENVIRONMENT_ID, PlatformType.AUTOMATION);

        assertThat(webhookTriggerTestFacade.isWorkflowEnabled(workflowExecutionId)).isTrue();

        workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(workflowExecutionId, ENVIRONMENT_ID, webhookRequest);

        WorkflowNodeTestOutput workflowNodeTestOutput = getTestOutput(workflow.getId(), "trigger_2");

        assertThat(workflowNodeTestOutput.getSampleOutput()).isEqualTo(Map.of("body", "ok"));
        assertThat(workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(
            workflow.getId(), "trigger_1", ENVIRONMENT_ID)).isEmpty();
        assertThat(webhookTriggerTestFacade.isWorkflowEnabled(workflowExecutionId)).isFalse();
    }

    @Test
    void testSaveClusterElementTestOutputPassesTheSourceConnectionOfTheRootTaskToTheScriptInputProvider() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {
                            "name": "dataStream_1",
                            "type": "dataStream/v1",
                            "parameters": {},
                            "clusterElements": {
                                "source": {"name": "postgresql_1", "type": "postgresql/v1/read", "parameters": {}},
                                "processor": {"name": "script_1", "type": "script/v1/javascript", "parameters": {}}
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

        when(clusterElementDefinitionService.getClusterElementType("dataStream", 1, "PROCESSOR"))
            .thenReturn(new ClusterElementType("PROCESSOR", "processor", "Processor"));
        when(connectionService.getConnection(20L)).thenReturn(connection);
        when(connection.getId()).thenReturn(20L);
        when(codeEditorScriptInputProvider.getRootComponentName()).thenReturn("dataStream");
        when(codeEditorScriptInputProvider.getScriptInputParameters(
            anyInt(), anyString(), anyInt(), anyString(), any(), any())).thenReturn(Map.of("row", "value"));
        when(actionDefinitionFacade.executePerform(
            eq("script"), eq(1), eq("javascript"), any(), any(), any(), any(), any(), anyMap(), anyMap(), anyMap(),
            any(), any(), anyBoolean(), any(), any(), any()))
                .thenReturn(Map.of("result", "ok"));

        workflowNodeTestOutputFacade.saveClusterElementTestOutput(
            workflow.getId(), "dataStream_1", "PROCESSOR", "script_1", ENVIRONMENT_ID);

        ArgumentCaptor<ComponentConnection> componentConnectionArgumentCaptor =
            ArgumentCaptor.forClass(ComponentConnection.class);

        verify(codeEditorScriptInputProvider).getScriptInputParameters(
            eq(1), eq("postgresql"), eq(1), eq("read"), any(), componentConnectionArgumentCaptor.capture());

        ComponentConnection componentConnection = componentConnectionArgumentCaptor.getValue();

        assertThat(componentConnection).isNotNull();
        assertThat(componentConnection.connectionId()).isEqualTo(20L);

        WorkflowNodeTestOutput workflowNodeTestOutput = getTestOutput(workflow.getId(), "script_1");

        assertThat(workflowNodeTestOutput.getSampleOutput()).isEqualTo(Map.of("result", "ok"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSaveWorkflowNodeSampleOutputTruncatesLargeLists() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "oracle_1", "type": "oracle/v1/query", "parameters": {}}
                    ]
                }
                """);

        List<Map<String, Object>> rows = IntStream.range(0, 8)
            .mapToObj(index -> Map.<String, Object>of("id", index, "tags", List.of(IntStream.range(0, 8)
                .boxed()
                .toArray())))
            .toList();

        workflowNodeTestOutputFacade.saveWorkflowNodeSampleOutput(
            workflow.getId(), "oracle_1", Map.of("rows", rows), ENVIRONMENT_ID);

        WorkflowNodeTestOutput workflowNodeTestOutput = getTestOutput(workflow.getId(), "oracle_1");

        assertThat(workflowNodeTestOutput.isSampleOutputTruncated()).isTrue();

        Map<String, Object> sampleOutput = (Map<String, Object>) workflowNodeTestOutput.getSampleOutput();

        List<Map<String, Object>> sampleRows = (List<Map<String, Object>>) sampleOutput.get("rows");

        assertThat(sampleRows).hasSize(MAX_SAMPLE_ITEMS);
        assertThat((List<Object>) sampleRows.getFirst()
            .get("tags")).hasSize(MAX_SAMPLE_ITEMS);
        assertThat(sampleRows.getLast()).containsEntry("id", MAX_SAMPLE_ITEMS - 1);
    }

    @Test
    void testSaveWorkflowNodeSampleOutputKeepsSmallOutput() {
        Workflow workflow = createWorkflow(
            """
                {
                    "tasks": [
                        {"name": "oracle_1", "type": "oracle/v1/query", "parameters": {}}
                    ]
                }
                """);

        List<Map<String, Object>> rows = List.of(Map.of("id", 1), Map.of("id", 2));

        workflowNodeTestOutputFacade.saveWorkflowNodeSampleOutput(workflow.getId(), "oracle_1", rows, ENVIRONMENT_ID);

        WorkflowNodeTestOutput workflowNodeTestOutput = getTestOutput(workflow.getId(), "oracle_1");

        assertThat(workflowNodeTestOutput.isSampleOutputTruncated()).isFalse();
        assertThat(workflowNodeTestOutput.getSampleOutput()).isEqualTo(rows);
    }

    private Workflow createWorkflow(String definition) {
        return workflowService.create(definition, Format.JSON, Workflow.SourceType.JDBC);
    }

    private WorkflowNodeTestOutput getTestOutput(String workflowId, String workflowNodeName) {
        return workflowNodeTestOutputService.fetchWorkflowTestNodeOutput(workflowId, workflowNodeName, ENVIRONMENT_ID)
            .orElseThrow();
    }
}
