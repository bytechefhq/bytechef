/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowDsl;
import java.util.List;
import java.util.Map;
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

        // File storage providers use generateFilename=true, so a same-named store still writes a new blob
        // with a distinct URL.
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
                        .perform(() -> "x")));

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

        // The emitted definition must survive the real workflow mapper: connections is a task-level property
        // the mapper collects into WorkflowTask.extensions, which is where ComponentConnection.of reads it.
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
                    .parameters(Map.of("retries", 3))
                    .perform(() -> "x")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        Workflow parsedWorkflow = new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);

        WorkflowTask workflowTask = parsedWorkflow.getTasks()
            .getFirst();

        Map<String, Object> parameters = (Map<String, Object>) workflowTask.getParameters();

        // The engine evaluates this formula against the job context before dispatch, so the task's input parameter
        // arrives as the workflow's inputs plus every prior task's output — see TaskContext.input().
        assertThat(parameters).containsEntry("input", "=#root");

        // A task's own declared parameters ride alongside the platform's, which win on a clash.
        assertThat(parameters).containsEntry("retries", 3);
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
                                .perform(() -> "c"),
                            WorkflowDsl.task("inventory")
                                .perform(() -> "i")),
                    WorkflowDsl.forkJoin("notify")
                        .branches(
                            WorkflowDsl.branch(
                                WorkflowDsl.task("slack")
                                    .perform(() -> "s")),
                            WorkflowDsl.branch(
                                WorkflowDsl.task("email")
                                    .perform(() -> "e")))));

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

        // Each nested leaf stays an ordinary perform node, carrying its own task name and its own job-context
        // formula, so nesting changes nothing about how a task resolves or what it sees.
        assertThat((Map<String, Object>) parallelTasks.getFirst()
            .get("parameters")).containsEntry("taskName", "customer")
                .containsEntry("input", "=#root")
                // Recorded so a read of a sibling fails with "runs at the same time as this task" rather than
                // "no task output named customer", which reads like a typo.
                .containsEntry("concurrentTaskNames", List.of("inventory"));

        WorkflowTask forkJoinTask = workflowTasks.get(1);

        assertThat(forkJoinTask.getType()).isEqualTo("fork-join/v1");

        List<List<Map<String, Object>>> branches =
            (List<List<Map<String, Object>>>) forkJoinTask.getParameters()
                .get("branches");

        assertThat(branches).hasSize(2);
        assertThat(branches.getFirst()).hasSize(1);

        // Only the other branches are concurrent — a branch runs its own tasks in sequence.
        assertThat((Map<String, Object>) branches.getFirst()
            .getFirst()
            .get("parameters")).containsEntry("concurrentTaskNames", List.of("email"));
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
                        .required(true))
                .outputs(
                    WorkflowDsl.output("customer")
                        .task("fetch-customer"))
                .triggers(
                    WorkflowDsl.trigger("daily", "schedule/v1/interval")
                        .parameters(Map.of("unit", "DAY")))
                .tasks(
                    WorkflowDsl.task("fetch-customer")
                        .perform(() -> "c")));

        codeWorkflowContainerFacade.update(
            codeWorkflowContainer, "v2", workflowDefinitions, "content".getBytes(), PlatformType.AUTOMATION);

        ArgumentCaptor<String> definitionCaptor = ArgumentCaptor.forClass(String.class);

        verify(workflowService).update(eq(workflowAId.toString()), definitionCaptor.capture(), eq(3));

        Workflow parsedWorkflow = new Workflow(definitionCaptor.getValue(), Workflow.Format.JSON);

        Workflow.Input input = parsedWorkflow.getInputs()
            .getFirst();

        assertThat(input.name()).isEqualTo("orderId");
        assertThat(input.required()).isTrue();

        // Naming a task emits the formula, since a task name here can be one no ${...} reference could reach.
        assertThat(parsedWorkflow.getOutputs()
            .getFirst()
            .value()).isEqualTo("=#root['fetch-customer']");

        Map<String, Object> trigger = ((List<Map<String, Object>>) parsedWorkflow.getExtensions()
            .get("triggers")).getFirst();

        assertThat(trigger).containsEntry("type", "schedule/v1/interval");
    }

    private static WorkflowDefinition workflowDefinition(String name) {
        return WorkflowDsl.workflow(name);
    }
}
