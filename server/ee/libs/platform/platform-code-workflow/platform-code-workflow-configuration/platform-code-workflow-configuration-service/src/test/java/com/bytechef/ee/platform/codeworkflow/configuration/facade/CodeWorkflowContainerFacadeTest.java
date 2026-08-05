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
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowDsl;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class CodeWorkflowContainerFacadeTest {

    @Mock
    private CodeWorkflowContainerService codeWorkflowContainerService;

    @Mock
    private CodeWorkflowFileStorage codeWorkflowFileStorage;

    @Mock
    private WorkflowService workflowService;

    private CodeWorkflowContainerFacadeImpl codeWorkflowContainerFacade;

    @BeforeEach
    void beforeEach() {
        codeWorkflowContainerFacade = new CodeWorkflowContainerFacadeImpl(
            codeWorkflowContainerService, codeWorkflowFileStorage, new ObjectMapper(), workflowService);
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

    private static WorkflowDefinition workflowDefinition(String name) {
        return WorkflowDsl.workflow(name);
    }
}
