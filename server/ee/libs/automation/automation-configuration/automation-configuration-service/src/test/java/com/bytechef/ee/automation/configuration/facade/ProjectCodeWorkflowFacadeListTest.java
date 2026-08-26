/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

/**
 * Verifies {@link ProjectCodeWorkflowFacadeImpl#getCodeWorkflowProjects}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeListTest {

    @Test
    void testGetCodeWorkflowProjectsReturnsProjectsForDistinctIds() {
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getCodeWorkflowProjectIds()).thenReturn(List.of(1L, 2L));

        Project firstProject = new Project();

        firstProject.setId(1L);
        firstProject.setName("first-code-project");

        Project secondProject = new Project();

        secondProject.setId(2L);
        secondProject.setName("second-code-project");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProjects(List.of(1L, 2L))).thenReturn(List.of(firstProject, secondProject));

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(projectService, projectCodeWorkflowService);

        List<Project> projects = projectCodeWorkflowFacade.getCodeWorkflowProjects();

        assertThat(projects).containsExactly(firstProject, secondProject);
    }

    @Test
    void testGetCodeWorkflowProjectsExcludesEmbeddedBridgeCatalogProjects() {
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getCodeWorkflowProjectIds()).thenReturn(List.of(1L, 2L));

        Project editorProject = new Project();

        editorProject.setId(1L);
        editorProject.setName("first-code-project");

        Project bridgeCatalogProject = new Project();

        bridgeCatalogProject.setId(2L);
        bridgeCatalogProject.setName("__EMBEDDED_AUTOMATION__catalog-project");

        ProjectService projectService = mock(ProjectService.class);

        when(projectService.getProjects(List.of(1L, 2L))).thenReturn(List.of(editorProject, bridgeCatalogProject));

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(projectService, projectCodeWorkflowService);

        List<Project> projects = projectCodeWorkflowFacade.getCodeWorkflowProjects();

        assertThat(projects).containsExactly(editorProject);
    }

    @Test
    void testGetCodeWorkflowProjectsReturnsEmptyListWhenNoCodeWorkflowsExist() {
        ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);

        when(projectCodeWorkflowService.getCodeWorkflowProjectIds()).thenReturn(List.of());

        ProjectService projectService = mock(ProjectService.class);

        ProjectCodeWorkflowFacadeImpl projectCodeWorkflowFacade = newFacade(projectService, projectCodeWorkflowService);

        List<Project> projects = projectCodeWorkflowFacade.getCodeWorkflowProjects();

        assertThat(projects).isEmpty();

        verify(projectService, never()).getProjects(anyList());
    }

    private static ProjectCodeWorkflowFacadeImpl newFacade(
        ProjectService projectService, ProjectCodeWorkflowService projectCodeWorkflowService) {

        return new ProjectCodeWorkflowFacadeImpl(
            applicationProperties(true), mock(CacheManager.class), projectService, mock(ProjectWorkflowService.class),
            mock(CodeWorkflowContainerFacade.class), projectCodeWorkflowService,
            mock(CodeWorkflowContainerService.class), mock(CodeWorkflowFileStorage.class),
            mock(TagService.class), mock(WorkflowService.class), List.of());
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
