/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.constant.PlatformType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowDraftServiceTest {

    private static final byte[] BYTES = "source".getBytes(StandardCharsets.UTF_8);

    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
    private final CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);
    private final ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final ProjectCodeWorkflowDraftServiceImpl projectCodeWorkflowDraftService =
        new ProjectCodeWorkflowDraftServiceImpl(
            codeWorkflowContainerFacade, codeWorkflowContainerService, projectCodeWorkflowService,
            projectWorkflowService, workflowService, List.of());

    @Test
    void testSaveDraftUpdatesTheDraftContainerInPlace() {
        Project project = project(2);
        CodeWorkflowContainer draftContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "draft-a"));

        stubLatest(pcw(10L, 2));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(draftContainer);
        when(codeWorkflowContainerFacade.update(eq(draftContainer), any(), any(), any(), eq(PlatformType.AUTOMATION)))
            .thenReturn(new CodeWorkflowReconciliation(draftContainer, Map.of(), Map.of()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).update(eq(draftContainer), any(), any(), eq(BYTES), any());
        verify(projectCodeWorkflowService, never()).create(any(), any());
    }

    @Test
    void testSaveDraftAfterPublishReusesDraftWorkflowIdsByName() {
        Project project = project(2);
        UUID uuid = UUID.randomUUID();
        CodeWorkflowContainer publishedContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a"));
        CodeWorkflowContainer newContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "draft-a"));

        stubLatest(pcw(10L, 1));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(publishedContainer);
        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "pub-a", uuid)));
        when(projectWorkflowService.getProjectWorkflows(1L, 2))
            .thenReturn(List.of(new ProjectWorkflow(1L, 2, "draft-a", uuid)));
        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(newContainer, Map.of(), Map.of()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(codeWorkflowContainerFacade).create(
            any(), any(), any(), eq(Language.JAVASCRIPT), eq(BYTES), eq(PlatformType.AUTOMATION),
            eq(Map.of("wf-a", "draft-a")));
        verify(projectCodeWorkflowService).create(newContainer, project);
    }

    @Test
    void testSaveDraftAddsAndRemovesDraftWorkflows() {
        Project project = project(2);
        CodeWorkflowContainer draftContainer = container(Language.JAVASCRIPT, Map.of());

        stubLatest(pcw(10L, 2));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(draftContainer);
        when(codeWorkflowContainerFacade.update(any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(
                draftContainer, Map.of("wf-new", "id-new"), Map.of("wf-old", "id-old")));
        when(projectWorkflowService.addWorkflow(1L, 2, "id-new"))
            .thenReturn(new ProjectWorkflow(1L, 2, "id-new", UUID.randomUUID()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(projectWorkflowService).addWorkflow(1L, 2, "id-new");
        verify(projectWorkflowService).delete(1L, 2, "id-old");
        verify(workflowService).delete("id-old");
    }

    @Test
    void testSaveDraftOnAnEmptyDraftRestoresThePublishedUuidByName() {
        Project project = project(2);
        UUID publishedUuid = UUID.randomUUID();
        ProjectCodeWorkflow publishedPcw = pcw(10L, 1);
        CodeWorkflowContainer publishedContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a"));
        CodeWorkflowContainer newContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "new-a"));
        ProjectWorkflow addedRow = new ProjectWorkflow(1L, 2, "new-a", UUID.randomUUID());

        stubLatest(publishedPcw);

        when(projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(1L, 2)).thenReturn(Optional.of(publishedPcw));
        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(publishedContainer);
        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "pub-a", publishedUuid)));
        when(projectWorkflowService.getProjectWorkflows(1L, 2)).thenReturn(List.of());
        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(newContainer, Map.of("wf-a", "new-a"), Map.of()));
        when(projectWorkflowService.addWorkflow(1L, 2, "new-a")).thenReturn(addedRow);

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        assertThat(addedRow.getUuid()).isEqualTo(publishedUuid);

        verify(projectWorkflowService).update(addedRow);
    }

    @Test
    void testSaveDraftInPlaceRestoresThePublishedUuidForAnAddedWorkflowByName() {
        Project project = project(2);
        UUID publishedUuid = UUID.randomUUID();
        ProjectCodeWorkflow draftPcw = pcw(11L, 2);
        ProjectCodeWorkflow publishedPcw = pcw(10L, 1);
        CodeWorkflowContainer draftContainer = container(Language.JAVASCRIPT, Map.of());
        CodeWorkflowContainer publishedContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a"));
        ProjectWorkflow addedRow = new ProjectWorkflow(1L, 2, "new-a", UUID.randomUUID());

        stubLatest(draftPcw);

        when(projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(1L, 2)).thenReturn(Optional.of(publishedPcw));
        when(codeWorkflowContainerService.getCodeWorkflowContainer(11L)).thenReturn(draftContainer);
        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(publishedContainer);
        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "pub-a", publishedUuid)));
        when(codeWorkflowContainerFacade.update(eq(draftContainer), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(draftContainer, Map.of("wf-a", "new-a"), Map.of()));
        when(projectWorkflowService.addWorkflow(1L, 2, "new-a")).thenReturn(addedRow);

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        assertThat(addedRow.getUuid()).isEqualTo(publishedUuid);

        verify(projectWorkflowService).update(addedRow);
        verify(projectCodeWorkflowService, never()).create(any(), any());
    }

    @Test
    void testSaveDraftKeepsAFreshUuidForANameThePublishedVersionLacked() {
        Project project = project(2);
        ProjectCodeWorkflow publishedPcw = pcw(10L, 1);
        CodeWorkflowContainer publishedContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "pub-a"));
        CodeWorkflowContainer newContainer = container(Language.JAVASCRIPT, Map.of("wf-b", "new-b"));

        stubLatest(publishedPcw);

        when(projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(1L, 2)).thenReturn(Optional.of(publishedPcw));
        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(publishedContainer);
        when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt())).thenReturn(List.of());
        when(codeWorkflowContainerFacade.create(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new CodeWorkflowReconciliation(newContainer, Map.of("wf-b", "new-b"), Map.of()));
        when(projectWorkflowService.addWorkflow(1L, 2, "new-b"))
            .thenReturn(new ProjectWorkflow(1L, 2, "new-b", UUID.randomUUID()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.JAVASCRIPT);

        verify(projectWorkflowService, never()).update(any());
    }

    @Test
    void testSaveDraftInAnotherLanguageUpdatesTheSameDraftContainerWithTheNewLanguage() {
        Project project = project(2);
        CodeWorkflowContainer draftContainer = container(Language.JAVASCRIPT, Map.of("wf-a", "draft-a"));

        stubLatest(pcw(10L, 2));

        when(codeWorkflowContainerService.getCodeWorkflowContainer(10L)).thenReturn(draftContainer);
        when(codeWorkflowContainerFacade.update(eq(draftContainer), any(), any(), any(), eq(PlatformType.AUTOMATION)))
            .thenReturn(new CodeWorkflowReconciliation(draftContainer, Map.of(), Map.of()));

        projectCodeWorkflowDraftService.saveDraft(project, definition(), BYTES, Language.PYTHON);

        verify(draftContainer).setLanguage(Language.PYTHON);
        verify(codeWorkflowContainerFacade).update(
            eq(draftContainer), any(), any(), eq(BYTES), eq(PlatformType.AUTOMATION));
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any(), any());
        verify(projectCodeWorkflowService, never()).create(any(), any());
    }

    private void stubLatest(ProjectCodeWorkflow projectCodeWorkflow) {
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L)).thenReturn(Optional.of(projectCodeWorkflow));
    }

    private static Project project(int draftVersion) {
        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");
        project.setProjectVersions(List.of(new ProjectVersion(draftVersion)));

        return project;
    }

    private static ProjectCodeWorkflow pcw(long containerId, int projectVersion) {
        ProjectCodeWorkflow projectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(projectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(containerId);
        when(projectCodeWorkflow.getProjectVersion()).thenReturn(projectVersion);

        return projectCodeWorkflow;
    }

    private static CodeWorkflowContainer container(Language language, Map<String, String> workflowNameIds) {
        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(language);
        when(codeWorkflowContainer.getWorkflowNameIds()).thenReturn(workflowNameIds);

        return codeWorkflowContainer;
    }

    private static ProjectDefinition definition() {
        ProjectDefinition projectDefinition = mock(ProjectDefinition.class);

        when(projectDefinition.getName()).thenReturn("my-code-project");
        when(projectDefinition.getVersion()).thenReturn("1");
        when(projectDefinition.getWorkflows()).thenReturn(List.of());

        return projectDefinition;
    }
}
