/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectGraphQlControllerTest {

    @Test
    void testAutomationWorkflowProjectsReturnsListFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        ConnectedUserWorkflowTemplateDTO workflowTemplateOne = new ConnectedUserWorkflowTemplateDTO(
            "wf-uuid-1", "Workflow One", "First workflow", "2026-05-22T10:00:00Z",
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("manual", "Manual Trigger", "manual-icon")),
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("gmail", "Gmail", "gmail-icon")));

        AutomationWorkflowProjectDTO projectOne =
            new AutomationWorkflowProjectDTO(1L, "Project One", "First project", null, List.of(), true, 5, 2,
                List.of(workflowTemplateOne));
        AutomationWorkflowProjectDTO projectTwo =
            new AutomationWorkflowProjectDTO(2L, "Project Two", null, 10L, List.of(20L), false, 1, null, List.of());

        when(automationWorkflowProjectFacade.getProjects()).thenReturn(List.of(projectOne, projectTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectDTO> result = controller.automationWorkflowProjects();

        assertThat(result).hasSize(2);
        AutomationWorkflowProjectDTO workflowProjectDTO = result.getFirst();

        assertThat(workflowProjectDTO.id()).isEqualTo(1L);
        assertThat(workflowProjectDTO.name()).isEqualTo("Project One");
        assertThat(workflowProjectDTO.description()).isEqualTo("First project");
        assertThat(workflowProjectDTO.categoryId()).isNull();

        List<ConnectedUserWorkflowTemplateDTO> connectedUserWorkflowTemplateDTOs1 =
            workflowProjectDTO.workflowTemplates();

        assertThat(connectedUserWorkflowTemplateDTOs1).hasSize(1);
        assertThat(workflowProjectDTO.version()).isEqualTo(5);

        ConnectedUserWorkflowTemplateDTO connectedUserWorkflowTemplateDTO = connectedUserWorkflowTemplateDTOs1
            .getFirst();

        assertThat(connectedUserWorkflowTemplateDTO.lastModifiedDate()).isEqualTo("2026-05-22T10:00:00Z");

        AutomationWorkflowProjectDTO automationWorkflowProjectDTO = result.getFirst();

        List<ConnectedUserWorkflowTemplateDTO> connectedUserWorkflowTemplateDTOs2 = automationWorkflowProjectDTO
            .workflowTemplates();

        ConnectedUserWorkflowTemplateDTO connectedUserWorkflowTemplateDTO2 = connectedUserWorkflowTemplateDTOs2
            .getFirst();

        assertThat(connectedUserWorkflowTemplateDTO2.triggers()).hasSize(1);
        assertThat(connectedUserWorkflowTemplateDTO2.components()).hasSize(1);

        AutomationWorkflowProjectDTO automationWorkflowProjectDTO2 = result.get(1);

        assertThat(automationWorkflowProjectDTO2.id()).isEqualTo(2L);
        assertThat(automationWorkflowProjectDTO2.description()).isNull();
        assertThat(automationWorkflowProjectDTO2.categoryId()).isEqualTo(10L);
    }

    @Test
    void testCreateAutomationWorkflowProjectDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.createProject("My Project", "desc", null, List.of())).thenReturn(42L);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.createAutomationWorkflowProject("My Project", "desc", null, null);

        assertThat(result).isEqualTo("42");
        verify(automationWorkflowProjectFacade).createProject("My Project", "desc", null, List.of());
    }

    @Test
    void testCreateAutomationWorkflowProjectWithCategoryAndTags() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.createProject("P", null, "Electronics", List.of("java", "spring")))
            .thenReturn(99L);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.createAutomationWorkflowProject("P", null, "Electronics", List.of("java", "spring"));

        assertThat(result).isEqualTo("99");
        verify(automationWorkflowProjectFacade).createProject("P", null, "Electronics", List.of("java", "spring"));
    }

    @Test
    void testUpdateAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        boolean result =
            controller.updateAutomationWorkflowProject("7", "Updated", "new desc", "Finance", List.of("api"));

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).updateProject(7L, "Updated", "new desc", "Finance", List.of("api"));
    }

    @Test
    void testDeleteAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        boolean result = controller.deleteAutomationWorkflowProject("5");

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).deleteProject(5L);
    }

    @Test
    void testCreateAutomationWorkflowProjectWorkflowDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.createProjectWorkflow(3L, "{\"tasks\":[]}")).thenReturn("new-wf-uuid");

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.createAutomationWorkflowProjectWorkflow("3", "{\"tasks\":[]}");

        assertThat(result).isEqualTo("new-wf-uuid");
        verify(automationWorkflowProjectFacade).createProjectWorkflow(3L, "{\"tasks\":[]}");
    }

    @Test
    void testDeleteAutomationWorkflowProjectWorkflowDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        boolean result = controller.deleteAutomationWorkflowProjectWorkflow("wf-uuid-to-delete");

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).deleteProjectWorkflow("wf-uuid-to-delete");
    }

    @Test
    void testPublishAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        boolean result = controller.publishAutomationWorkflowProject("8");

        assertThat(result).isTrue();
        verify(automationWorkflowProjectFacade).publishProject(8L);
    }

    @Test
    void testAutomationWorkflowProjectCategoriesReturnsCategoriesFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectCategoryDTO categoryOne = new AutomationWorkflowProjectCategoryDTO(1L, "Finance");
        AutomationWorkflowProjectCategoryDTO categoryTwo = new AutomationWorkflowProjectCategoryDTO(2L, "Marketing");

        when(automationWorkflowProjectFacade.getCategories()).thenReturn(List.of(categoryOne, categoryTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectCategoryDTO> result = controller.automationWorkflowProjectCategories();

        assertThat(result).hasSize(2);

        AutomationWorkflowProjectCategoryDTO automationWorkflowProjectCategoryDTO1 = result.getFirst();

        assertThat(automationWorkflowProjectCategoryDTO1.id()).isEqualTo(1L);
        assertThat(automationWorkflowProjectCategoryDTO1.name()).isEqualTo("Finance");

        AutomationWorkflowProjectCategoryDTO automationWorkflowProjectCategoryDTO2 = result.get(1);

        assertThat(automationWorkflowProjectCategoryDTO2.id()).isEqualTo(2L);
        assertThat(automationWorkflowProjectCategoryDTO2.name()).isEqualTo("Marketing");
    }

    @Test
    void testAutomationWorkflowProjectTagsReturnsTagsFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectTagDTO tagOne = new AutomationWorkflowProjectTagDTO(10L, "java");
        AutomationWorkflowProjectTagDTO tagTwo = new AutomationWorkflowProjectTagDTO(20L, "spring");

        when(automationWorkflowProjectFacade.getTags()).thenReturn(List.of(tagOne, tagTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectTagDTO> result = controller.automationWorkflowProjectTags();

        assertThat(result).hasSize(2);
        AutomationWorkflowProjectTagDTO automationWorkflowProjectTagDTO1 = result.getFirst();

        assertThat(automationWorkflowProjectTagDTO1.id()).isEqualTo(10L);
        assertThat(automationWorkflowProjectTagDTO1.name()).isEqualTo("java");

        AutomationWorkflowProjectTagDTO automationWorkflowProjectTagDTO2 = result.get(1);

        assertThat(automationWorkflowProjectTagDTO2.id()).isEqualTo(20L);
        assertThat(automationWorkflowProjectTagDTO2.name()).isEqualTo("spring");
    }

    @Test
    void testAutomationWorkflowProjectVersionsReturnsVersionsFromFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        AutomationWorkflowProjectVersionDTO versionOne = new AutomationWorkflowProjectVersionDTO(
            1, "PUBLISHED", "2026-01-01T10:00:00Z");
        AutomationWorkflowProjectVersionDTO versionTwo = new AutomationWorkflowProjectVersionDTO(2, "DRAFT", null);

        when(automationWorkflowProjectFacade.getProjectVersions(5L)).thenReturn(List.of(versionOne, versionTwo));

        AutomationWorkflowProjectGraphQlController controller =
            new AutomationWorkflowProjectGraphQlController(automationWorkflowProjectFacade);

        List<AutomationWorkflowProjectVersionDTO> result = controller.automationWorkflowProjectVersions("5");

        assertThat(result).hasSize(2);
        AutomationWorkflowProjectVersionDTO automationWorkflowProjectVersionDTO1 = result.getFirst();

        assertThat(automationWorkflowProjectVersionDTO1.version()).isEqualTo(1);
        assertThat(automationWorkflowProjectVersionDTO1.status()).isEqualTo("PUBLISHED");
        assertThat(automationWorkflowProjectVersionDTO1.publishedDate()).isEqualTo("2026-01-01T10:00:00Z");

        AutomationWorkflowProjectVersionDTO automationWorkflowProjectVersionDTO2 = result.get(1);

        assertThat(automationWorkflowProjectVersionDTO2.version()).isEqualTo(2);
        assertThat(automationWorkflowProjectVersionDTO2.status()).isEqualTo("DRAFT");
        assertThat(automationWorkflowProjectVersionDTO2.publishedDate()).isNull();

        verify(automationWorkflowProjectFacade).getProjectVersions(5L);
    }

    @Test
    void testDuplicateAutomationWorkflowProjectWorkflowDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.duplicateProjectWorkflow("source-wf-id")).thenReturn("new-wf-id");

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.duplicateAutomationWorkflowProjectWorkflow("source-wf-id");

        assertThat(result).isEqualTo("new-wf-id");
        verify(automationWorkflowProjectFacade).duplicateProjectWorkflow("source-wf-id");
    }

    @Test
    void testDuplicateAutomationWorkflowProjectDelegatesToFacade() {
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade = mock(AutomationWorkflowProjectFacade.class);

        when(automationWorkflowProjectFacade.duplicateProject(3L)).thenReturn(99L);

        AutomationWorkflowProjectGraphQlController controller = new AutomationWorkflowProjectGraphQlController(
            automationWorkflowProjectFacade);

        String result = controller.duplicateAutomationWorkflowProject("3");

        assertThat(result).isEqualTo("99");
        verify(automationWorkflowProjectFacade).duplicateProject(3L);
    }
}
