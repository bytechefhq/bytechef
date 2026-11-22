/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.workflow.definition.LeafTaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowDsl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class CodeWorkflowContainerFacadeTest {

    private static final UUID COMPONENT_TASK_WORKFLOW_ID = UUID.randomUUID();

    @Mock
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Mock
    private CodeWorkflowFileStorage codeWorkflowFileStorage;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ObjectProvider<ComponentDefinitionService> componentDefinitionServiceProvider;

    @Mock
    private WorkflowService workflowService;

    private CodeWorkflowContainerFacadeImpl codeWorkflowContainerFacade;

    @BeforeEach
    void beforeEach() {
        codeWorkflowContainerFacade = new CodeWorkflowContainerFacadeImpl(
            codeWorkflowContainerService, codeWorkflowFileStorage, componentDefinitionServiceProvider,
            new ObjectMapper(),
            workflowService);
    }

    @Test
    void testUpdateReplacesBlobAndUpdatesMatchedWorkflowsInPlace() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(workflowDefinition("wf-a"));

        CodeWorkflowReconciliation reconciliation = codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        verify(workflowService).getWorkflow(workflowAId.toString());
        verify(workflowService).update(eq(workflowAId.toString()), anyString(), eq(3));
        verify(workflowService, never()).create(anyString(), any(), any());
        verify(codeWorkflowContainerService).update(codeWorkflowContainer);
        verify(codeWorkflowFileStorage).storeCodeWorkflowFile(eq(containerUuid + ".js"), any());

        assertThat(codeWorkflowContainer.getUuid()).isEqualTo(containerUuid.toString());
        assertThat(reconciliation.addedWorkflowNameIds()).isEmpty();
        assertThat(reconciliation.removedWorkflowNameIds()).isEmpty();
        assertThat(codeWorkflowContainer.getWorkflowNameIds()).containsEntry("wf-a", workflowAId.toString());
    }

    @Test
    void testUpdateDeletesOldBlobWhenUrlChanges() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        FileEntry oldFileEntry = new FileEntry(containerUuid + ".js", "file://old/" + containerUuid + ".js");

        codeWorkflowContainer.setWorkflows(oldFileEntry);

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);

        FileEntry newFileEntry = new FileEntry(containerUuid + ".js", "file://new/" + containerUuid + ".js");

        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any())).thenReturn(newFileEntry);
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(workflowDefinition("wf-a"));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        verify(codeWorkflowFileStorage).deleteCodeWorkflowFile(oldFileEntry);

        assertThat(codeWorkflowContainer.getWorkflows()).isEqualTo(newFileEntry);
    }

    @Test
    void testUpdateCreatesNewAndReportsRemoved() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();
        UUID workflowBId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow newWorkflow = mock(Workflow.class);

        when(newWorkflow.getId()).thenReturn(workflowBId.toString());
        when(workflowService.create(anyString(), any(), any())).thenReturn(newWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(workflowDefinition("wf-b"));

        CodeWorkflowReconciliation reconciliation = codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        verify(workflowService).create(anyString(), eq(Workflow.Format.JSON), eq(Workflow.SourceType.JDBC));
        verify(workflowService, never()).delete(anyString());

        assertThat(reconciliation.addedWorkflowNameIds()).containsExactly(Map.entry("wf-b", workflowBId.toString()));
        assertThat(reconciliation.removedWorkflowNameIds())
            .containsExactly(Map.entry("wf-a", workflowAId.toString()));
        assertThat(codeWorkflowContainer.getWorkflowNameIds()).doesNotContainKey("wf-a");
        assertThat(codeWorkflowContainer.getWorkflowNameIds()).containsEntry("wf-b", workflowBId.toString());
    }

    @Test
    void testCreateWithReuseAdoptsExistingWorkflowIds() {
        UUID workflowAId = UUID.randomUUID();
        UUID workflowBId = UUID.randomUUID();

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(5);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);

        Workflow newWorkflow = mock(Workflow.class);

        when(newWorkflow.getId()).thenReturn(workflowBId.toString());
        when(workflowService.create(anyString(), any(), any())).thenReturn(newWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> reusableWorkflowNameIds = Map.of("wf-a", workflowAId.toString());
        List<WorkflowDefinition> workflowDefinitions = List.of(
            workflowDefinition("wf-a"), workflowDefinition("wf-b"));

        CodeWorkflowReconciliation reconciliation = codeWorkflowContainerFacade.create(
            "container", "v1", workflowDefinitions, Language.JAVASCRIPT, "content".getBytes(),
            PlatformType.AUTOMATION, reusableWorkflowNameIds);

        verify(workflowService).update(eq(workflowAId.toString()), anyString(), eq(5));
        verify(workflowService).create(anyString(), eq(Workflow.Format.JSON), eq(Workflow.SourceType.JDBC));

        assertThat(reconciliation.addedWorkflowNameIds()).containsExactly(Map.entry("wf-b", workflowBId.toString()));
        assertThat(reconciliation.removedWorkflowNameIds()).isEmpty();
        assertThat(reconciliation.codeWorkflowContainer()
            .getWorkflowNameIds()).containsExactlyInAnyOrderEntriesOf(
                Map.of("wf-a", workflowAId.toString(), "wf-b", workflowBId.toString()));
    }

    @Test
    void testCreateWithReuseReportsDroppedReusableAsRemoved() {
        UUID workflowAId = UUID.randomUUID();
        UUID workflowCId = UUID.randomUUID();

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(2);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, String> reusableWorkflowNameIds = Map.of(
            "wf-a", workflowAId.toString(), "wf-c", workflowCId.toString());
        List<WorkflowDefinition> workflowDefinitions = List.of(workflowDefinition("wf-a"));

        CodeWorkflowReconciliation reconciliation = codeWorkflowContainerFacade.create(
            "container", "v1", workflowDefinitions, Language.JAVASCRIPT, "content".getBytes(),
            PlatformType.AUTOMATION, reusableWorkflowNameIds);

        assertThat(reconciliation.removedWorkflowNameIds())
            .containsExactly(Map.entry("wf-c", workflowCId.toString()));
    }

    @Test
    void testLegacyCreateDelegatesWithNoReuse() {
        UUID workflowAId = UUID.randomUUID();
        UUID workflowBId = UUID.randomUUID();

        Workflow workflowA = mock(Workflow.class);
        Workflow workflowB = mock(Workflow.class);

        when(workflowA.getId()).thenReturn(workflowAId.toString());
        when(workflowB.getId()).thenReturn(workflowBId.toString());
        when(workflowService.create(anyString(), any(), any())).thenReturn(workflowA, workflowB);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<WorkflowDefinition> workflowDefinitions = List.of(
            workflowDefinition("wf-a"), workflowDefinition("wf-b"));

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            "container", "v1", workflowDefinitions, Language.JAVASCRIPT, "content".getBytes(),
            PlatformType.AUTOMATION);

        verify(workflowService, never()).update(anyString(), anyString(), any(Integer.class));
        verify(workflowService, never()).getWorkflow(anyString());
        verify(codeWorkflowFileStorage, never()).deleteCodeWorkflowFile(any());

        assertThat(codeWorkflowContainer.getWorkflowNameIds()).containsExactlyInAnyOrderEntriesOf(
            Map.of("wf-a", workflowAId.toString(), "wf-b", workflowBId.toString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEmitsDeclaredTaskConnectionsIntoDefinition() throws Exception {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.task("my-task")
                        .connections(
                            WorkflowDsl.connection("slack", "slack-prod"),
                            WorkflowDsl.connection("httpClient", 2, "billing-api"))
                        .perform((input, context) -> "x")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        String definition = definitionCaptor.getValue();

        assertThat(definition)
            .doesNotContain("\"extensions\"")
            .contains("\"connections\"")
            .contains("\"slack-prod\"")
            .contains("\"componentName\":\"slack\"")
            .contains("\"componentVersion\":1")
            .contains("\"billing-api\"")
            .contains("\"componentVersion\":2");

        Workflow parsedWorkflow = new Workflow(definition, Workflow.Format.JSON);

        WorkflowTask workflowTask = parsedWorkflow.getTasks()
            .getFirst();

        Map<String, ?> extensions = workflowTask.getExtensions();

        assertThat(extensions).containsKey("connections");

        Map<String, Map<String, Object>> connections = (Map<String, Map<String, Object>>) extensions.get("connections");

        assertThat(connections).containsOnlyKeys("slack-prod", "billing-api");
        assertThat(connections.get("slack-prod")).containsEntry("componentName", "slack");
        assertThat(connections.get("billing-api")).containsEntry("componentVersion", 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEmitsTheJobContextFormulaAsTheTaskInputParameter() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .tasks(WorkflowDsl.task("my-task")
                    .perform((input, context) -> "x")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        Workflow parsedWorkflow = new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);

        WorkflowTask workflowTask = parsedWorkflow.getTasks()
            .getFirst();

        Map<String, Object> parameters = (Map<String, Object>) workflowTask.getParameters();

        assertThat(parameters).containsOnlyKeys("codeWorkflowContainerUuid", "workflowName", "taskName", "input",
            "type");
        assertThat(parameters).containsEntry("input", "=#root");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEmitsTheEngineDispatchersForTaskGroups() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.parallel("enrich")
                        .tasks(
                            WorkflowDsl.task("customer")
                                .perform((input, context) -> "c"),
                            WorkflowDsl.task("inventory")
                                .perform((input, context) -> "i")),
                    WorkflowDsl.forkJoin("notify")
                        .branches(
                            WorkflowDsl.branch(
                                WorkflowDsl.task("slack")
                                    .perform((input, context) -> "s")),
                            WorkflowDsl.branch(
                                WorkflowDsl.task("email")
                                    .perform((input, context) -> "e")))));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        Workflow parsedWorkflow = new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);

        List<WorkflowTask> workflowTasks = parsedWorkflow.getTasks();

        WorkflowTask parallelTask = workflowTasks.getFirst();

        assertThat(parallelTask.getType()).isEqualTo("parallel/v1");

        List<Map<String, Object>> parallelTasks =
            (List<Map<String, Object>>) parallelTask.getParameters()
                .get("tasks");

        assertThat(parallelTasks).hasSize(2);
        assertThat(parallelTasks.getFirst()).containsEntry("type", "codeWorkflow/v1/perform");

        assertThat((Map<String, Object>) parallelTasks.getFirst()
            .get("parameters")).containsEntry("taskName", "customer")
                .containsEntry("input", "=#root")
                .doesNotContainKey("concurrentTaskNames");

        WorkflowTask forkJoinTask = workflowTasks.get(1);

        assertThat(forkJoinTask.getType()).isEqualTo("fork-join/v1");

        List<List<Map<String, Object>>> branches =
            (List<List<Map<String, Object>>>) forkJoinTask.getParameters()
                .get("branches");

        assertThat(branches).hasSize(2);
        assertThat(branches.getFirst()).hasSize(1);

        assertThat((Map<String, Object>) branches.getFirst()
            .getFirst()
            .get("parameters")).containsEntry("taskName", "slack");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEmitsDeclaredInputsOutputsAndTriggers() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .inputs(
                    WorkflowDsl.input("orderId")
                        .label("Order ID")
                        .type("STRING")
                        .required(true))
                .outputs(
                    WorkflowDsl.output("customer")
                        .task("fetch-customer"))
                .triggers(
                    WorkflowDsl.trigger("daily", "schedule/v1/interval")
                        .parameters(Map.of("unit", "DAY")))
                .tasks(
                    WorkflowDsl.task("fetch-customer")
                        .perform((input, context) -> "c")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        Workflow parsedWorkflow = new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);

        Workflow.Input input = parsedWorkflow.getInputs()
            .getFirst();

        assertThat(input.name()).isEqualTo("orderId");
        assertThat(input.required()).isTrue();
        assertThat(input.type()).isEqualTo("string");

        assertThat(parsedWorkflow.getOutputs()
            .getFirst()
            .value()).isEqualTo("=#root['fetch-customer']");

        Map<String, Object> trigger = ((List<Map<String, Object>>) parsedWorkflow.getExtensions()
            .get("triggers")).getFirst();

        assertThat(trigger).containsEntry("type", "schedule/v1/interval");
    }

    @Test
    void testUpdateEmitsTheComponentInputAnInputReferences() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .inputs(
                    WorkflowDsl.input("channel")
                        .label("Slack Channel")
                        .component("slack", 1, "channel")
                        .required(true),
                    WorkflowDsl.input("note"))
                .tasks(
                    WorkflowDsl.task("post")
                        .perform((input, context) -> "c")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        List<WorkflowInput> workflowInputs = WorkflowInput.of(
            new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON));

        assertThat(workflowInputs.getFirst()
            .getComponentInputReference())
                .isEqualTo(new WorkflowInput.ComponentInputReference("slack", 1, "channel"));
        assertThat(workflowInputs.get(1)
            .getComponentInputReference()).isNull();
    }

    @Test
    void testUpdateEmitsAnInternalOnlyInput() {
        UUID containerUuid = UUID.randomUUID();
        UUID workflowAId = UUID.randomUUID();

        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(containerUuid);

        codeWorkflowContainer.addCodeWorkflow(workflowAId, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(workflowAId.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .inputs(
                    WorkflowDsl.input("contactPageUrl")
                        .internalOnly(true),
                    WorkflowDsl.input("note"))
                .tasks(
                    WorkflowDsl.task("post")
                        .perform((input, context) -> "c")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        List<WorkflowInput> workflowInputs = WorkflowInput.of(
            new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON));

        assertThat(workflowInputs.getFirst()
            .isInternalOnly()).isTrue();
        assertThat(workflowInputs.get(1)
            .isInternalOnly()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEmitsAComponentTaskAsAWorkflowTask() {
        stubComponent("apiPlatform", 1, "responseToAPIRequest");

        Workflow parsedWorkflow = updateAndCaptureWorkflow(
            newFacadeWithComponentDefinitionService(),
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.task("fetch")
                        .perform((input, context) -> "orders"),
                    WorkflowDsl.componentTask("respond", "apiPlatform/v1/responseToAPIRequest")
                        .label("Respond")
                        .parameters(Map.of("response", Map.of("body", "${fetch}", "statusCode", 200)))));

        List<WorkflowTask> workflowTasks = parsedWorkflow.getTasks();

        WorkflowTask respondTask = workflowTasks.get(1);

        assertThat(respondTask.getType()).isEqualTo("apiPlatform/v1/responseToAPIRequest");
        assertThat(respondTask.getLabel()).isEqualTo("Respond");

        Map<String, ?> respondParameters = respondTask.getParameters();

        Map<String, Object> response = (Map<String, Object>) respondParameters.get("response");

        assertThat(response).containsEntry("body", "${fetch}")
            .containsEntry("statusCode", 200);

        Map<String, Object> respondTaskMap = respondTask.toMap();

        assertThat(respondTaskMap).doesNotContainKey("connections");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateEmitsComponentTasksNestedInGroups() {
        stubComponent("logger", 1, "info");

        Workflow parsedWorkflow = updateAndCaptureWorkflow(
            newFacadeWithComponentDefinitionService(),
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.parallel("enrich")
                        .tasks(
                            WorkflowDsl.task("customer")
                                .perform((input, context) -> "c"),
                            WorkflowDsl.componentTask("log", "logger/v1/info")
                                .parameters(Map.of("text", "x")))));

        List<WorkflowTask> workflowTasks = parsedWorkflow.getTasks();

        WorkflowTask parallelTask = workflowTasks.getFirst();

        Map<String, ?> parallelParameters = parallelTask.getParameters();

        List<Map<String, Object>> parallelTasks = (List<Map<String, Object>>) parallelParameters.get("tasks");

        assertThat(parallelTasks.get(1)).containsEntry("type", "logger/v1/info")
            .containsEntry("name", "log");
    }

    @Test
    void testUpdateRejectsAComponentTaskWithAnUnknownComponent() {
        when(componentDefinitionService.fetchComponentDefinition("noSuchComponent", 1)).thenReturn(Optional.empty());

        assertComponentTaskRejected("noSuchComponent/v1/doIt", "respond", "does not have");
    }

    @Test
    void testUpdateRejectsAComponentTaskWithAnUnknownVersion() {
        when(componentDefinitionService.fetchComponentDefinition("apiPlatform", 9)).thenReturn(Optional.empty());

        assertComponentTaskRejected("apiPlatform/v9/responseToAPIRequest", "respond", "version 9");
    }

    @Test
    void testUpdateRejectsAComponentTaskWithAnUnknownAction() {
        stubComponent("apiPlatform", 1, "responseToAPIRequest");

        assertComponentTaskRejected("apiPlatform/v1/noSuchAction", "respond", "noSuchAction");
    }

    @Test
    void testUpdateRejectsAComponentTaskWithAMalformedType() {
        assertComponentTaskRejected("apiPlatform/1/responseToAPIRequest", "respond", "<component>/v<version>/<action>");
    }

    @Test
    void testUpdateRejectsAComponentTaskWithALeadingZeroVersion() {
        assertComponentTaskRejected("logger/v01/info", "respond", "<component>/v<version>/<action>");
    }

    @Test
    void testUpdateRejectsAnUnsupportedTaskInAGroup() {
        LeafTaskDefinition unsupportedTaskDefinition = new LeafTaskDefinition() {

            @Override
            public Optional<String> getDescription() {
                return Optional.empty();
            }

            @Override
            public Optional<String> getLabel() {
                return Optional.empty();
            }

            @Override
            public String getName() {
                return "odd";
            }
        };

        CodeWorkflowContainer codeWorkflowContainer = newComponentTaskContainer();

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.parallel("enrich")
                        .tasks(unsupportedTaskDefinition)));

        assertThatThrownBy(
            () -> codeWorkflowContainerFacade.update(
                codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION))
                    .cause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll("Task odd", "unsupported task type");
    }

    @Test
    void testUpdateSkipsComponentTaskValidationWithoutComponentDefinitionService() {
        Workflow parsedWorkflow = updateAndCaptureWorkflow(
            codeWorkflowContainerFacade,
            WorkflowDsl.workflow("wf-a")
                .tasks(WorkflowDsl.componentTask("respond", "apiPlatform/v1/responseToAPIRequest")));

        List<WorkflowTask> workflowTasks = parsedWorkflow.getTasks();

        WorkflowTask respondTask = workflowTasks.getFirst();

        assertThat(respondTask.getType()).isEqualTo("apiPlatform/v1/responseToAPIRequest");
    }

    @Test
    void testUpdateRejectsAnExpressionThatReferencesAHyphenatedTaskName() {
        assertWorkflowRejected(
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.task("fetch-orders")
                        .perform((input, context) -> "orders"),
                    WorkflowDsl.componentTask("respond", "apiPlatform/v1/responseToAPIRequest")
                        .parameters(Map.of("response", Map.of("success", "${fetch-orders}")))),
            "Task respond", "${fetch-orders}", "fetch-orders", "fetchOrders");
    }

    @Test
    void testUpdateRejectsAnExpressionThatReferencesAHyphenatedTriggerName() {
        assertWorkflowRejected(
            WorkflowDsl.workflow("wf-a")
                .triggers(WorkflowDsl.trigger("new-request", "apiPlatform/v1/newApiRequest"))
                .tasks(
                    WorkflowDsl.componentTask("transactions", "gaurus/v1/getAccountTransactions")
                        .parameters(Map.of("accounts", List.of(Map.of("iban", "${new-request.parameters.iban}"))))),
            "Task transactions", "new-request", "newRequest");
    }

    @Test
    void testUpdateRejectsAHyphenatedTaskNameReferencedInsideAGroup() {
        assertWorkflowRejected(
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.task("fetch-orders")
                        .perform((input, context) -> "orders"),
                    WorkflowDsl.parallel("notify")
                        .tasks(
                            WorkflowDsl.componentTask("log", "logger/v1/info")
                                .parameters(Map.of("text", "Orders: ${fetch-orders.count}")))),
            "Task log", "fetch-orders");
    }

    @Test
    void testUpdateAcceptsAHyphenatedTaskNameNoExpressionReferences() {
        Workflow parsedWorkflow = updateAndCaptureWorkflow(
            codeWorkflowContainerFacade,
            WorkflowDsl.workflow("wf-a")
                .tasks(
                    WorkflowDsl.task("fetch-orders")
                        .perform((input, context) -> "orders"),
                    WorkflowDsl.componentTask("respond", "apiPlatform/v1/responseToAPIRequest")
                        .parameters(Map.of("response", Map.of("success", "${fetch}")))));

        List<WorkflowTask> workflowTasks = parsedWorkflow.getTasks();

        assertThat(workflowTasks).extracting(WorkflowTask::getName)
            .containsExactly("fetch-orders", "respond");
    }

    private void assertWorkflowRejected(WorkflowDefinition workflowDefinition, String... expectedMessageParts) {
        CodeWorkflowContainer codeWorkflowContainer = newComponentTaskContainer();

        assertThatThrownBy(
            () -> codeWorkflowContainerFacade.update(
                codeWorkflowContainer, "v2", List.of(workflowDefinition), "content".getBytes(),
                PlatformType.AUTOMATION))
                    .cause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(expectedMessageParts);
    }

    private void assertComponentTaskRejected(String type, String... expectedMessageParts) {
        CodeWorkflowContainerFacadeImpl facade = newFacadeWithComponentDefinitionService();

        CodeWorkflowContainer codeWorkflowContainer = newComponentTaskContainer();

        List<WorkflowDefinition> workflowDefinitions = List.of(
            WorkflowDsl.workflow("wf-a")
                .tasks(WorkflowDsl.componentTask("respond", type)));

        assertThatThrownBy(
            () -> facade.update(
                codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION))
                    .cause()
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContainingAll(expectedMessageParts);
    }

    private CodeWorkflowContainer newComponentTaskContainer() {
        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(UUID.randomUUID());

        codeWorkflowContainer.addCodeWorkflow(COMPONENT_TASK_WORKFLOW_ID, "wf-a");
        codeWorkflowContainer.setLanguage(Language.JAVASCRIPT);
        codeWorkflowContainer.setName("container");
        codeWorkflowContainer.setExternalVersion("v1");

        return codeWorkflowContainer;
    }

    private CodeWorkflowContainerFacadeImpl newFacadeWithComponentDefinitionService() {
        when(componentDefinitionServiceProvider.getIfAvailable()).thenReturn(componentDefinitionService);

        return new CodeWorkflowContainerFacadeImpl(
            codeWorkflowContainerService, codeWorkflowFileStorage, componentDefinitionServiceProvider,
            new ObjectMapper(), workflowService);
    }

    private void stubComponent(String componentName, int componentVersion, String actionName) {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getName()).thenReturn(actionName);
        when(componentDefinition.getActions()).thenReturn(List.of(actionDefinition));
        when(componentDefinitionService.fetchComponentDefinition(componentName, componentVersion))
            .thenReturn(Optional.of(componentDefinition));
    }

    private Workflow updateAndCaptureWorkflow(
        CodeWorkflowContainerFacadeImpl facade, WorkflowDefinition workflowDefinition) {

        CodeWorkflowContainer codeWorkflowContainer = newComponentTaskContainer();

        Workflow existingWorkflow = mock(Workflow.class);

        when(existingWorkflow.getVersion()).thenReturn(3);
        when(workflowService.getWorkflow(COMPONENT_TASK_WORKFLOW_ID.toString())).thenReturn(existingWorkflow);
        when(codeWorkflowFileStorage.storeCodeWorkflowFile(anyString(), any()))
            .thenReturn(new FileEntry("container.js", "file://container.js"));
        when(codeWorkflowContainerService.update(codeWorkflowContainer)).thenReturn(codeWorkflowContainer);

        facade.update(
            codeWorkflowContainer, "v2", List.of(workflowDefinition), "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(
            eq(COMPONENT_TASK_WORKFLOW_ID.toString()), definitionCaptor.capture(), eq(3));

        return new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);
    }

    private static WorkflowDefinition workflowDefinition(String name) {
        return WorkflowDsl.workflow(name);
    }
}
