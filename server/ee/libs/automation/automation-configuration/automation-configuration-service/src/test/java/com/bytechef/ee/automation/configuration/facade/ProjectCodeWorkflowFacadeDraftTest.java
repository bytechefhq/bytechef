/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftServiceImpl;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeDraftTest {

    @Test
    void testEditorSaveUpdatesDraftContainerInPlace() {
        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");
        project.setProjectVersions(List.of(new ProjectVersion(3)));

        ProjectCodeWorkflow latestProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(latestProjectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(latestProjectCodeWorkflow.getProjectVersion()).thenReturn(3);

        CodeWorkflowContainer draftCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(draftCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(latestProjectCodeWorkflow);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L))
            .thenReturn(Optional.of(latestProjectCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(draftCodeWorkflowContainer);

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        CodeWorkflowContainer updatedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            updatedCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.AUTOMATION)))
                .thenReturn(reconciliation);

        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService,
            codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class), workflowService);

        projectCodeWorkflowFacade.updateCodeWorkflowSource(1L, jsScript("my-code-project"));

        verify(codeWorkflowContainerFacade).update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.AUTOMATION));
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any());
        verify(codeWorkflowContainerFacade, never()).create(any(), any(), any(), any(), any(), any(), any());
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
        verify(projectCodeWorkflowService, never()).create(any(), any());
    }

    @Test
    void testEditorSaveAfterPublishMintsNewContainerReusingDraftWorkflowsByUuid() {
        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");
        project.setProjectVersions(List.of(new ProjectVersion(2)));

        ProjectCodeWorkflow publishedProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(publishedProjectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(publishedProjectCodeWorkflow.getProjectVersion()).thenReturn(1);

        CodeWorkflowContainer publishedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(publishedCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);
        when(publishedCodeWorkflowContainer.getWorkflowNameIds()).thenReturn(Map.of("wf-a", "pub-id"));

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(publishedProjectCodeWorkflow);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L))
            .thenReturn(Optional.of(publishedProjectCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(publishedCodeWorkflowContainer);

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        UUID workflowUuid = UUID.randomUUID();

        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "pub-id", workflowUuid)));
        when(projectWorkflowService.getProjectWorkflows(1L, 2))
            .thenReturn(List.of(new ProjectWorkflow(1L, 2, "draft-id", workflowUuid)));

        CodeWorkflowContainer newCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            newCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            any(), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION), any()))
                .thenReturn(reconciliation);

        WorkflowService workflowService = mock(WorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService,
            codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class), workflowService);

        projectCodeWorkflowFacade.updateCodeWorkflowSource(1L, jsScript("my-code-project"));

        ArgumentCaptor<Map<String, String>> reusableWorkflowNameIdsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(codeWorkflowContainerFacade).create(
            eq("my-code-project"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION),
            reusableWorkflowNameIdsCaptor.capture());

        assertThat(reusableWorkflowNameIdsCaptor.getValue())
            .isEqualTo(Map.of("wf-a", "draft-id"));

        verify(projectCodeWorkflowService).create(newCodeWorkflowContainer, project);
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    @Test
    void testEditorSaveSyncsAddedAndRemovedProjectWorkflows() {
        Project project = new Project();

        project.setId(1L);
        project.setName("my-code-project");
        project.setProjectVersions(List.of(new ProjectVersion(3)));

        ProjectCodeWorkflow latestProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(latestProjectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(5L);
        when(latestProjectCodeWorkflow.getProjectVersion()).thenReturn(3);

        CodeWorkflowContainer draftCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(draftCodeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getProjectCodeWorkflow(1L)).thenReturn(latestProjectCodeWorkflow);
        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(1L))
            .thenReturn(Optional.of(latestProjectCodeWorkflow));

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(5L)).thenReturn(draftCodeWorkflowContainer);

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProject(1L)).thenReturn(project);

        CodeWorkflowContainer updatedCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            updatedCodeWorkflowContainer, Map.of("wf-new", "id-n"), Map.of("wf-old", "id-o"));

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.update(
            eq(draftCodeWorkflowContainer), any(), any(), any(), eq(PlatformType.AUTOMATION)))
                .thenReturn(reconciliation);

        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService,
            codeWorkflowContainerService, mock(CodeWorkflowFileStorage.class), workflowService);

        projectCodeWorkflowFacade.updateCodeWorkflowSource(1L, jsScript("my-code-project"));

        verify(projectWorkflowService).addWorkflow(1L, 3, "id-n");
        verify(projectWorkflowService).delete(1L, 3, "id-o");
        verify(workflowService).delete("id-o");
    }

    @Test
    void testCreateEmptyCodeWorkflowCreatesDraftWithoutPublishing() {
        ProjectService projectService = mock(ProjectService.class);

        when(projectService.fetchProject("my-empty-project")).thenReturn(Optional.empty());

        Project createdProject = new Project();

        createdProject.setId(11L);
        createdProject.setName("my-empty-project");

        when(projectService.create(any())).thenReturn(createdProject);

        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.fetchProjectCodeWorkflow(11L)).thenReturn(Optional.empty());

        CodeWorkflowContainer newCodeWorkflowContainer = mock(CodeWorkflowContainer.class);

        CodeWorkflowReconciliation reconciliation = new CodeWorkflowReconciliation(
            newCodeWorkflowContainer, Map.of(), Map.of());

        CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);

        when(codeWorkflowContainerFacade.create(
            eq("my-empty-project"), any(), any(), eq(Language.JAVASCRIPT), any(), eq(PlatformType.AUTOMATION),
            eq(Map.of()))).thenReturn(reconciliation);

        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(
            projectService, projectWorkflowService, codeWorkflowContainerFacade, projectCodeWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class), workflowService);

        Project result = projectCodeWorkflowFacade.createEmptyCodeWorkflow(2L, "my-empty-project", Language.JAVASCRIPT);

        assertThat(result).isEqualTo(createdProject);

        verify(projectCodeWorkflowService).create(newCodeWorkflowContainer, createdProject);
        verify(projectService, never()).publishProject(anyLong(), any(), anyBoolean());
    }

    private static String jsScript(String name) {
        return "({name: \"" + name + "\", version: \"1\", workflows: [{name: \"my-workflow\", "
            + "label: \"My Workflow\", tasks: [{name: \"my-task\", label: \"My Task\", "
            + "perform: function() { return \"hello\"; }}]}]})";
    }

    private static ProjectCodeWorkflowFacadeImpl newFacade(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        WorkflowService workflowService) {

        return new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, projectWorkflowService,
            mock(ProjectFacade.class), projectCodeWorkflowService, codeWorkflowContainerService,
            codeWorkflowFileStorage,
            new ProjectCodeWorkflowDraftServiceImpl(
                codeWorkflowContainerFacade, codeWorkflowContainerService, projectCodeWorkflowService,
                projectWorkflowService, workflowService, List.of()),
            mock(TagService.class), mock(ProjectDeploymentFacade.class), mock(ProjectDeploymentService.class),
            mock(ProjectDeploymentWorkflowService.class));
    }

    private static ApplicationProperties applicationProperties(boolean javaEnabled) {
        ApplicationProperties.Workflow workflow = new ApplicationProperties.Workflow();

        workflow.getCodeWorkflow()
            .setJavaEnabled(javaEnabled);

        ApplicationProperties applicationProperties = mock(ApplicationProperties.class);

        when(applicationProperties.getWorkflow()).thenReturn(workflow);

        return applicationProperties;
    }
}
