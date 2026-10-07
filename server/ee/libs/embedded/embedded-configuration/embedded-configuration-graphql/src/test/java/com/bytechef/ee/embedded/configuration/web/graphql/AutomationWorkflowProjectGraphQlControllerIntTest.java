/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectAdminFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    AutomationWorkflowProjectGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationWorkflowProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
class AutomationWorkflowProjectGraphQlControllerIntTest {

    private static final String AUTOMATION_WORKFLOW_PROJECTS_QUERY = """
        query {
            automationWorkflowProjects {
                id
                name
                description
                categoryId
                tagIds
                published
                version
                lastPublishedVersion
                permissionExpression
                automationHubVisible
                workflowTemplates {
                    workflowUuid
                    workflowId
                    label
                    description
                    permissionExpression
                    lastModifiedDate
                    triggers {
                        name
                        title
                        icon
                    }
                    components {
                        name
                        title
                        icon
                    }
                }
            }
        }
        """;
    private static final String DRAFT_WORKFLOW_ID = "0b6e4f8c-3f1d-4a52-9d6e-5c2b7a1e9f30";
    private static final String NEW_WORKFLOW_UUID = "7d3c2b1a-9e8f-4d6c-b5a4-3f2e1d0c9b8a";
    private static final String SOURCE_WORKFLOW_UUID = "c4b3a291-8f7e-4d5c-a6b7-e8f9a0b1c2d3";
    private static final String WORKFLOW_UUID = "1f2e3d4c-5b6a-4789-8a9b-0c1d2e3f4a5b";

    @Autowired
    private AutomationWorkflowProjectAdminFacade automationWorkflowProjectFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAutomationWorkflowProjectsReturnsListFromFacade() {
        ConnectedUserWorkflowTemplateDTO workflowTemplateOne = new ConnectedUserWorkflowTemplateDTO(
            WORKFLOW_UUID, "Workflow One", "First workflow", "2026-05-22T10:00:00Z",
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("manual", "Manual Trigger", "manual-icon")),
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("gmail", "Gmail", "gmail-icon")), List.of(), null,
            DRAFT_WORKFLOW_ID);

        AutomationWorkflowProjectDTO projectOne = new AutomationWorkflowProjectDTO(
            1L, "Project One", "First project", null, List.of(), true, 5, 2, List.of(workflowTemplateOne), null, true);
        AutomationWorkflowProjectDTO projectTwo = new AutomationWorkflowProjectDTO(
            2L, "Project Two", null, 10L, List.of(20L), false, 1, null, List.of(), null, true);

        when(automationWorkflowProjectFacade.getProjects()).thenReturn(List.of(projectOne, projectTwo));

        this.graphQlTester
            .document(AUTOMATION_WORKFLOW_PROJECTS_QUERY)
            .execute()
            .path("automationWorkflowProjects")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationWorkflowProjects[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("automationWorkflowProjects[0].name")
            .entity(String.class)
            .isEqualTo("Project One")
            .path("automationWorkflowProjects[0].description")
            .entity(String.class)
            .isEqualTo("First project")
            .path("automationWorkflowProjects[0].categoryId")
            .valueIsNull()
            .path("automationWorkflowProjects[0].tagIds")
            .entityList(String.class)
            .hasSize(0)
            .path("automationWorkflowProjects[0].published")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationWorkflowProjects[0].version")
            .entity(Integer.class)
            .isEqualTo(5)
            .path("automationWorkflowProjects[0].lastPublishedVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationWorkflowProjects[0].permissionExpression")
            .valueIsNull()
            .path("automationWorkflowProjects[0].automationHubVisible")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationWorkflowProjects[0].workflowTemplates")
            .entityList(Object.class)
            .hasSize(1)
            .path("automationWorkflowProjects[0].workflowTemplates[0].workflowUuid")
            .entity(String.class)
            .isEqualTo(WORKFLOW_UUID)
            .path("automationWorkflowProjects[0].workflowTemplates[0].workflowId")
            .entity(String.class)
            .isEqualTo(DRAFT_WORKFLOW_ID)
            .path("automationWorkflowProjects[0].workflowTemplates[0].label")
            .entity(String.class)
            .isEqualTo("Workflow One")
            .path("automationWorkflowProjects[0].workflowTemplates[0].description")
            .entity(String.class)
            .isEqualTo("First workflow")
            .path("automationWorkflowProjects[0].workflowTemplates[0].permissionExpression")
            .valueIsNull()
            .path("automationWorkflowProjects[0].workflowTemplates[0].lastModifiedDate")
            .entity(String.class)
            .isEqualTo("2026-05-22T10:00:00Z")
            .path("automationWorkflowProjects[0].workflowTemplates[0].triggers")
            .entityList(Object.class)
            .hasSize(1)
            .path("automationWorkflowProjects[0].workflowTemplates[0].triggers[0].name")
            .entity(String.class)
            .isEqualTo("manual")
            .path("automationWorkflowProjects[0].workflowTemplates[0].triggers[0].title")
            .entity(String.class)
            .isEqualTo("Manual Trigger")
            .path("automationWorkflowProjects[0].workflowTemplates[0].triggers[0].icon")
            .entity(String.class)
            .isEqualTo("manual-icon")
            .path("automationWorkflowProjects[0].workflowTemplates[0].components")
            .entityList(Object.class)
            .hasSize(1)
            .path("automationWorkflowProjects[0].workflowTemplates[0].components[0].name")
            .entity(String.class)
            .isEqualTo("gmail")
            .path("automationWorkflowProjects[0].workflowTemplates[0].components[0].title")
            .entity(String.class)
            .isEqualTo("Gmail")
            .path("automationWorkflowProjects[0].workflowTemplates[0].components[0].icon")
            .entity(String.class)
            .isEqualTo("gmail-icon")
            .path("automationWorkflowProjects[1].id")
            .entity(String.class)
            .isEqualTo("2")
            .path("automationWorkflowProjects[1].name")
            .entity(String.class)
            .isEqualTo("Project Two")
            .path("automationWorkflowProjects[1].description")
            .valueIsNull()
            .path("automationWorkflowProjects[1].categoryId")
            .entity(String.class)
            .isEqualTo("10")
            .path("automationWorkflowProjects[1].tagIds")
            .entityList(String.class)
            .containsExactly("20")
            .path("automationWorkflowProjects[1].published")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationWorkflowProjects[1].version")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationWorkflowProjects[1].lastPublishedVersion")
            .valueIsNull()
            .path("automationWorkflowProjects[1].workflowTemplates")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testCreateAutomationWorkflowProjectDelegatesToFacade() {
        when(automationWorkflowProjectFacade.createProject("My Project", "desc", null, List.of(), null, null))
            .thenReturn(42L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationWorkflowProject(name: "My Project", description: "desc")
                }
                """)
            .execute()
            .path("createAutomationWorkflowProject")
            .entity(String.class)
            .isEqualTo("42");

        verify(automationWorkflowProjectFacade).createProject("My Project", "desc", null, List.of(), null, null);
    }

    @Test
    void testCreateAutomationWorkflowProjectWithCategoryAndTags() {
        when(automationWorkflowProjectFacade.createProject(
            "P", null, "Electronics", List.of("java", "spring"), null, null))
                .thenReturn(99L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationWorkflowProject(name: "P", category: "Electronics", tags: ["java", "spring"])
                }
                """)
            .execute()
            .path("createAutomationWorkflowProject")
            .entity(String.class)
            .isEqualTo("99");

        verify(automationWorkflowProjectFacade).createProject(
            "P", null, "Electronics", List.of("java", "spring"), null, null);
    }

    @Test
    void testCreateAutomationWorkflowProjectPassesAutomationHubVisibleToFacade() {
        when(automationWorkflowProjectFacade.createProject("P", null, null, List.of(), null, false))
            .thenReturn(7L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationWorkflowProject(name: "P", automationHubVisible: false)
                }
                """)
            .execute()
            .path("createAutomationWorkflowProject")
            .entity(String.class)
            .isEqualTo("7");

        verify(automationWorkflowProjectFacade).createProject("P", null, null, List.of(), null, false);
    }

    @Test
    void testUpdateAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationWorkflowProject(
                        id: "7", name: "Updated", description: "new desc", category: "Finance", tags: ["api"])
                }
                """)
            .execute()
            .path("updateAutomationWorkflowProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).updateProject(
            7L, "Updated", "new desc", "Finance", List.of("api"), null, null);
    }

    @Test
    void testUpdateAutomationWorkflowProjectPassesAutomationHubVisibleToFacade() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationWorkflowProject(
                        id: "7", name: "Updated", description: "new desc", category: "Finance", tags: ["api"],
                        automationHubVisible: false)
                }
                """)
            .execute()
            .path("updateAutomationWorkflowProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).updateProject(
            7L, "Updated", "new desc", "Finance", List.of("api"), null, false);
    }

    @Test
    void testDeleteAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationWorkflowProject(id: "5")
                }
                """)
            .execute()
            .path("deleteAutomationWorkflowProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).deleteProject(5L);
    }

    @Test
    void testDeleteAutomationWorkflowProjectRejectsNonNumericId() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationWorkflowProject(id: "not-a-number")
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        verifyNoInteractions(automationWorkflowProjectFacade);
    }

    @Test
    void testCreateAutomationWorkflowProjectWorkflowDelegatesToFacade() {
        when(automationWorkflowProjectFacade.createProjectWorkflow(3L, "{\"tasks\":[]}", null))
            .thenReturn(NEW_WORKFLOW_UUID);

        this.graphQlTester
            .document("""
                mutation($definition: String) {
                    createAutomationWorkflowProjectWorkflow(projectId: "3", definition: $definition)
                }
                """)
            .variable("definition", "{\"tasks\":[]}")
            .execute()
            .path("createAutomationWorkflowProjectWorkflow")
            .entity(String.class)
            .isEqualTo(NEW_WORKFLOW_UUID);

        verify(automationWorkflowProjectFacade).createProjectWorkflow(3L, "{\"tasks\":[]}", null);
    }

    @Test
    void testUpdateAutomationWorkflowProjectWorkflowDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!) {
                    updateAutomationWorkflowProjectWorkflow(
                        workflowUuid: $workflowUuid, label: "Renamed", description: "Renamed workflow")
                }
                """)
            .variable("workflowUuid", WORKFLOW_UUID)
            .execute()
            .path("updateAutomationWorkflowProjectWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).updateProjectWorkflow(WORKFLOW_UUID, "Renamed", "Renamed workflow");
    }

    @Test
    void testUpdateAutomationWorkflowProjectWorkflowPermissionExpressionDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!, $permissionExpression: String) {
                    updateAutomationWorkflowProjectWorkflowPermissionExpression(
                        workflowUuid: $workflowUuid, permissionExpression: $permissionExpression)
                }
                """)
            .variable("workflowUuid", WORKFLOW_UUID)
            .variable("permissionExpression", "metadata['tier'] == 'pro'")
            .execute()
            .path("updateAutomationWorkflowProjectWorkflowPermissionExpression")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).updateProjectWorkflowPermissionExpression(
            WORKFLOW_UUID, "metadata['tier'] == 'pro'");
    }

    @Test
    void testDeleteAutomationWorkflowProjectWorkflowDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!) {
                    deleteAutomationWorkflowProjectWorkflow(workflowUuid: $workflowUuid)
                }
                """)
            .variable("workflowUuid", WORKFLOW_UUID)
            .execute()
            .path("deleteAutomationWorkflowProjectWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).deleteProjectWorkflow(WORKFLOW_UUID);
    }

    @Test
    void testPublishAutomationWorkflowProjectDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    publishAutomationWorkflowProject(id: "8")
                }
                """)
            .execute()
            .path("publishAutomationWorkflowProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationWorkflowProjectFacade).publishProject(8L);
    }

    @Test
    void testAutomationWorkflowProjectCategoriesReturnsCategoriesFromFacade() {
        AutomationWorkflowProjectCategoryDTO categoryOne = new AutomationWorkflowProjectCategoryDTO(1L, "Finance");
        AutomationWorkflowProjectCategoryDTO categoryTwo = new AutomationWorkflowProjectCategoryDTO(2L, "Marketing");

        when(automationWorkflowProjectFacade.getCategories()).thenReturn(List.of(categoryOne, categoryTwo));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectCategories {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectCategories")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationWorkflowProjectCategories[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("automationWorkflowProjectCategories[0].name")
            .entity(String.class)
            .isEqualTo("Finance")
            .path("automationWorkflowProjectCategories[1].id")
            .entity(String.class)
            .isEqualTo("2")
            .path("automationWorkflowProjectCategories[1].name")
            .entity(String.class)
            .isEqualTo("Marketing");
    }

    @Test
    void testAutomationWorkflowProjectTagsReturnsTagsFromFacade() {
        AutomationWorkflowProjectTagDTO tagOne = new AutomationWorkflowProjectTagDTO(10L, "java");
        AutomationWorkflowProjectTagDTO tagTwo = new AutomationWorkflowProjectTagDTO(20L, "spring");

        when(automationWorkflowProjectFacade.getTags()).thenReturn(List.of(tagOne, tagTwo));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectTags {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectTags")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationWorkflowProjectTags[0].id")
            .entity(String.class)
            .isEqualTo("10")
            .path("automationWorkflowProjectTags[0].name")
            .entity(String.class)
            .isEqualTo("java")
            .path("automationWorkflowProjectTags[1].id")
            .entity(String.class)
            .isEqualTo("20")
            .path("automationWorkflowProjectTags[1].name")
            .entity(String.class)
            .isEqualTo("spring");
    }

    @Test
    void testAutomationWorkflowProjectVersionsReturnsVersionsFromFacade() {
        AutomationWorkflowProjectVersionDTO versionOne = new AutomationWorkflowProjectVersionDTO(
            1, "PUBLISHED", "2026-01-01T10:00:00Z");
        AutomationWorkflowProjectVersionDTO versionTwo = new AutomationWorkflowProjectVersionDTO(2, "DRAFT", null);

        when(automationWorkflowProjectFacade.getProjectVersions(5L)).thenReturn(List.of(versionOne, versionTwo));

        this.graphQlTester
            .document("""
                query {
                    automationWorkflowProjectVersions(id: "5") {
                        version
                        status
                        publishedDate
                    }
                }
                """)
            .execute()
            .path("automationWorkflowProjectVersions")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationWorkflowProjectVersions[0].version")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationWorkflowProjectVersions[0].status")
            .entity(String.class)
            .isEqualTo("PUBLISHED")
            .path("automationWorkflowProjectVersions[0].publishedDate")
            .entity(String.class)
            .isEqualTo("2026-01-01T10:00:00Z")
            .path("automationWorkflowProjectVersions[1].version")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationWorkflowProjectVersions[1].status")
            .entity(String.class)
            .isEqualTo("DRAFT")
            .path("automationWorkflowProjectVersions[1].publishedDate")
            .valueIsNull();

        verify(automationWorkflowProjectFacade).getProjectVersions(5L);
    }

    @Test
    void testDuplicateAutomationWorkflowProjectWorkflowDelegatesToFacade() {
        when(automationWorkflowProjectFacade.duplicateProjectWorkflow(SOURCE_WORKFLOW_UUID))
            .thenReturn(NEW_WORKFLOW_UUID);

        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!) {
                    duplicateAutomationWorkflowProjectWorkflow(workflowUuid: $workflowUuid)
                }
                """)
            .variable("workflowUuid", SOURCE_WORKFLOW_UUID)
            .execute()
            .path("duplicateAutomationWorkflowProjectWorkflow")
            .entity(String.class)
            .isEqualTo(NEW_WORKFLOW_UUID);

        verify(automationWorkflowProjectFacade).duplicateProjectWorkflow(SOURCE_WORKFLOW_UUID);
    }

    @Test
    void testDuplicateAutomationWorkflowProjectDelegatesToFacade() {
        when(automationWorkflowProjectFacade.duplicateProject(3L)).thenReturn(99L);

        this.graphQlTester
            .document("""
                mutation {
                    duplicateAutomationWorkflowProject(id: "3")
                }
                """)
            .execute()
            .path("duplicateAutomationWorkflowProject")
            .entity(String.class)
            .isEqualTo("99");

        verify(automationWorkflowProjectFacade).duplicateProject(3L);
    }
}
