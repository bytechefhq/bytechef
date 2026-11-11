/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCodeWorkflowStateDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeSummary;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.dto.WorkflowChangeType;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionChange;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionValueChange;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeKind;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeTarget;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    AutomationProjectGraphQlController.class
})
@GraphQlTest(
    controllers = AutomationProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
class AutomationProjectGraphQlControllerIntTest {

    private static final String AUTOMATION_PROJECTS_QUERY = """
        query {
            automationProjects {
                id
                name
                description
                categoryId
                tagIds
                published
                version
                lastPublishedVersion
                permissionExpression
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
    private AutomationProjectFacade automationProjectFacade;

    @MockitoBean
    private AutomationProjectCodeWorkflowFacade automationProjectCodeWorkflowFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAutomationProjectsOfAnEnvironmentCountsItsConfigurations() {
        AutomationProjectDTO project = new AutomationProjectDTO(
            1L, "Project One", null, null, List.of(), true, 2, 1, List.of(), null, false, true, 3, null);

        when(automationProjectFacade.getProjects(1L)).thenReturn(List.of(project));

        this.graphQlTester
            .document("""
                query {
                    automationProjects(environmentId: "1") {
                        id
                        configurationCount
                    }
                }
                """)
            .execute()
            .path("automationProjects[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("automationProjects[0].configurationCount")
            .entity(Integer.class)
            .isEqualTo(3);

        verify(automationProjectFacade, never()).getProjects();
    }

    @Test
    void testAutomationProjectsReturnsListFromFacade() {
        ConnectedUserWorkflowTemplateDTO workflowTemplateOne = new ConnectedUserWorkflowTemplateDTO(
            WORKFLOW_UUID, "Workflow One", "First workflow", "2026-05-22T10:00:00Z",
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("manual", "Manual Trigger", "manual-icon")),
            List.of(new ConnectedUserWorkflowTemplateDTO.Component("gmail", "Gmail", "gmail-icon")), List.of(), null,
            DRAFT_WORKFLOW_ID);

        AutomationProjectDTO projectOne = new AutomationProjectDTO(
            1L, "Project One", "First project", null, List.of(), true, 5, 2, List.of(workflowTemplateOne), null,
            false, true,
            0, null);
        AutomationProjectDTO projectTwo = new AutomationProjectDTO(
            2L, "Project Two", null, 10L, List.of(20L), false, 1, null, List.of(), null, false, true, 0, null);

        when(automationProjectFacade.getProjects()).thenReturn(List.of(projectOne, projectTwo));

        this.graphQlTester
            .document(AUTOMATION_PROJECTS_QUERY)
            .execute()
            .path("automationProjects")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationProjects[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("automationProjects[0].name")
            .entity(String.class)
            .isEqualTo("Project One")
            .path("automationProjects[0].description")
            .entity(String.class)
            .isEqualTo("First project")
            .path("automationProjects[0].categoryId")
            .valueIsNull()
            .path("automationProjects[0].tagIds")
            .entityList(String.class)
            .hasSize(0)
            .path("automationProjects[0].published")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("automationProjects[0].version")
            .entity(Integer.class)
            .isEqualTo(5)
            .path("automationProjects[0].lastPublishedVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationProjects[0].permissionExpression")
            .valueIsNull()
            .path("automationProjects[0].workflowTemplates")
            .entityList(Object.class)
            .hasSize(1)
            .path("automationProjects[0].workflowTemplates[0].workflowUuid")
            .entity(String.class)
            .isEqualTo(WORKFLOW_UUID)
            .path("automationProjects[0].workflowTemplates[0].workflowId")
            .entity(String.class)
            .isEqualTo(DRAFT_WORKFLOW_ID)
            .path("automationProjects[0].workflowTemplates[0].label")
            .entity(String.class)
            .isEqualTo("Workflow One")
            .path("automationProjects[0].workflowTemplates[0].description")
            .entity(String.class)
            .isEqualTo("First workflow")
            .path("automationProjects[0].workflowTemplates[0].permissionExpression")
            .valueIsNull()
            .path("automationProjects[0].workflowTemplates[0].lastModifiedDate")
            .entity(String.class)
            .isEqualTo("2026-05-22T10:00:00Z")
            .path("automationProjects[0].workflowTemplates[0].triggers")
            .entityList(Object.class)
            .hasSize(1)
            .path("automationProjects[0].workflowTemplates[0].triggers[0].name")
            .entity(String.class)
            .isEqualTo("manual")
            .path("automationProjects[0].workflowTemplates[0].triggers[0].title")
            .entity(String.class)
            .isEqualTo("Manual Trigger")
            .path("automationProjects[0].workflowTemplates[0].triggers[0].icon")
            .entity(String.class)
            .isEqualTo("manual-icon")
            .path("automationProjects[0].workflowTemplates[0].components")
            .entityList(Object.class)
            .hasSize(1)
            .path("automationProjects[0].workflowTemplates[0].components[0].name")
            .entity(String.class)
            .isEqualTo("gmail")
            .path("automationProjects[0].workflowTemplates[0].components[0].title")
            .entity(String.class)
            .isEqualTo("Gmail")
            .path("automationProjects[0].workflowTemplates[0].components[0].icon")
            .entity(String.class)
            .isEqualTo("gmail-icon")
            .path("automationProjects[1].id")
            .entity(String.class)
            .isEqualTo("2")
            .path("automationProjects[1].name")
            .entity(String.class)
            .isEqualTo("Project Two")
            .path("automationProjects[1].description")
            .valueIsNull()
            .path("automationProjects[1].categoryId")
            .entity(String.class)
            .isEqualTo("10")
            .path("automationProjects[1].tagIds")
            .entityList(String.class)
            .containsExactly("20")
            .path("automationProjects[1].published")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("automationProjects[1].version")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjects[1].lastPublishedVersion")
            .valueIsNull()
            .path("automationProjects[1].workflowTemplates")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testCodeProjectExposesItsLanguageAndWhetherTheDraftIsPublishable() {
        AutomationProjectDTO codeProject = new AutomationProjectDTO(
            3L, "Code Project", null, null, List.of(), true, 2, 1, List.of(), null, true, true, 0, null);

        when(automationProjectFacade.getProjects()).thenReturn(List.of(codeProject));
        when(automationProjectCodeWorkflowFacade.getCodeWorkflowState(3L))
            .thenReturn(Optional.of(new AutomationProjectCodeWorkflowStateDTO(Language.JAVA, false)));

        this.graphQlTester
            .document("query { automationProjects { id codeWorkflowLanguage codeWorkflowDraftPublishable } }")
            .execute()
            .path("automationProjects[0].codeWorkflowLanguage")
            .entity(String.class)
            .isEqualTo("JAVA")
            .path("automationProjects[0].codeWorkflowDraftPublishable")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testVisualProjectHasNoCodeWorkflowStateAndNeverHitsTheCodeFacade() {
        AutomationProjectDTO visualProject = new AutomationProjectDTO(
            4L, "Visual Project", null, null, List.of(), true, 2, 1, List.of(), null, false, true, 0, null);

        when(automationProjectFacade.getProjects()).thenReturn(List.of(visualProject));

        this.graphQlTester
            .document("query { automationProjects { id codeWorkflowLanguage codeWorkflowDraftPublishable } }")
            .execute()
            .path("automationProjects[0].codeWorkflowLanguage")
            .valueIsNull()
            .path("automationProjects[0].codeWorkflowDraftPublishable")
            .valueIsNull();

        verifyNoInteractions(automationProjectCodeWorkflowFacade);
    }

    @Test
    void testCreateAutomationProjectDelegatesToFacade() {
        when(automationProjectFacade.createProject("My Project", "desc", null, List.of(), null))
            .thenReturn(42L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationProject(name: "My Project", description: "desc")
                }
                """)
            .execute()
            .path("createAutomationProject")
            .entity(String.class)
            .isEqualTo("42");

        verify(automationProjectFacade).createProject("My Project", "desc", null, List.of(), null);
    }

    @Test
    void testCreateAutomationProjectWithCategoryAndTags() {
        when(automationProjectFacade.createProject(
            "P", null, "Electronics", List.of("java", "spring"), null))
                .thenReturn(99L);

        this.graphQlTester
            .document("""
                mutation {
                    createAutomationProject(name: "P", category: "Electronics", tags: ["java", "spring"])
                }
                """)
            .execute()
            .path("createAutomationProject")
            .entity(String.class)
            .isEqualTo("99");

        verify(automationProjectFacade).createProject(
            "P", null, "Electronics", List.of("java", "spring"), null);
    }

    @Test
    void testUpdateAutomationProjectDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAutomationProject(
                        id: "7", name: "Updated", description: "new desc", category: "Finance", tags: ["api"])
                }
                """)
            .execute()
            .path("updateAutomationProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).updateProject(
            7L, "Updated", "new desc", "Finance", List.of("api"), null);
    }

    @Test
    void testDeleteAutomationProjectDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationProject(id: "5")
                }
                """)
            .execute()
            .path("deleteAutomationProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).deleteProject(5L);
    }

    @Test
    void testDeleteAutomationProjectRejectsNonNumericId() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteAutomationProject(id: "not-a-number")
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        verifyNoInteractions(automationProjectFacade);
    }

    @Test
    void testCreateAutomationProjectWorkflowDelegatesToFacade() {
        when(automationProjectFacade.createProjectWorkflow(3L, "{\"tasks\":[]}", null))
            .thenReturn(NEW_WORKFLOW_UUID);

        this.graphQlTester
            .document("""
                mutation($definition: String) {
                    createAutomationProjectWorkflow(projectId: "3", definition: $definition)
                }
                """)
            .variable("definition", "{\"tasks\":[]}")
            .execute()
            .path("createAutomationProjectWorkflow")
            .entity(String.class)
            .isEqualTo(NEW_WORKFLOW_UUID);

        verify(automationProjectFacade).createProjectWorkflow(3L, "{\"tasks\":[]}", null);
    }

    @Test
    void testUpdateAutomationProjectWorkflowDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!) {
                    updateAutomationProjectWorkflow(
                        workflowUuid: $workflowUuid, label: "Renamed", description: "Renamed workflow")
                }
                """)
            .variable("workflowUuid", WORKFLOW_UUID)
            .execute()
            .path("updateAutomationProjectWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).updateProjectWorkflow(WORKFLOW_UUID, "Renamed", "Renamed workflow");
    }

    @Test
    void testUpdateAutomationProjectWorkflowPermissionExpressionDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!, $permissionExpression: String) {
                    updateAutomationProjectWorkflowPermissionExpression(
                        workflowUuid: $workflowUuid, permissionExpression: $permissionExpression)
                }
                """)
            .variable("workflowUuid", WORKFLOW_UUID)
            .variable("permissionExpression", "metadata['tier'] == 'pro'")
            .execute()
            .path("updateAutomationProjectWorkflowPermissionExpression")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).updateProjectWorkflowPermissionExpression(
            WORKFLOW_UUID, "metadata['tier'] == 'pro'");
    }

    @Test
    void testDeleteAutomationProjectWorkflowDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!) {
                    deleteAutomationProjectWorkflow(workflowUuid: $workflowUuid)
                }
                """)
            .variable("workflowUuid", WORKFLOW_UUID)
            .execute()
            .path("deleteAutomationProjectWorkflow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).deleteProjectWorkflow(WORKFLOW_UUID);
    }

    @Test
    void testPublishAutomationProjectDelegatesToFacadeAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    publishAutomationProject(id: "8")
                }
                """)
            .execute()
            .path("publishAutomationProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).publishProject(8L, null);
    }

    @Test
    void testPublishAutomationProjectPassesTheDescription() {
        this.graphQlTester
            .document("""
                mutation {
                    publishAutomationProject(id: "8", description: "Adds greeting")
                }
                """)
            .execute()
            .path("publishAutomationProject")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(automationProjectFacade).publishProject(8L, "Adds greeting");
    }

    @Test
    void testAutomationProjectCategoriesReturnsCategoriesFromFacade() {
        AutomationProjectCategoryDTO categoryOne = new AutomationProjectCategoryDTO(1L, "Finance");
        AutomationProjectCategoryDTO categoryTwo = new AutomationProjectCategoryDTO(2L, "Marketing");

        when(automationProjectFacade.getCategories()).thenReturn(List.of(categoryOne, categoryTwo));

        this.graphQlTester
            .document("""
                query {
                    automationProjectCategories {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("automationProjectCategories")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationProjectCategories[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("automationProjectCategories[0].name")
            .entity(String.class)
            .isEqualTo("Finance")
            .path("automationProjectCategories[1].id")
            .entity(String.class)
            .isEqualTo("2")
            .path("automationProjectCategories[1].name")
            .entity(String.class)
            .isEqualTo("Marketing");
    }

    @Test
    void testAutomationProjectTagsReturnsTagsFromFacade() {
        AutomationProjectTagDTO tagOne = new AutomationProjectTagDTO(10L, "java");
        AutomationProjectTagDTO tagTwo = new AutomationProjectTagDTO(20L, "spring");

        when(automationProjectFacade.getTags()).thenReturn(List.of(tagOne, tagTwo));

        this.graphQlTester
            .document("""
                query {
                    automationProjectTags {
                        id
                        name
                    }
                }
                """)
            .execute()
            .path("automationProjectTags")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationProjectTags[0].id")
            .entity(String.class)
            .isEqualTo("10")
            .path("automationProjectTags[0].name")
            .entity(String.class)
            .isEqualTo("java")
            .path("automationProjectTags[1].id")
            .entity(String.class)
            .isEqualTo("20")
            .path("automationProjectTags[1].name")
            .entity(String.class)
            .isEqualTo("spring");
    }

    @Test
    void testAutomationProjectVersionsReturnsVersionsFromFacade() {
        AutomationProjectVersionDTO versionOne = new AutomationProjectVersionDTO(
            1, "PUBLISHED", "2026-01-01T10:00:00Z", "First release");
        AutomationProjectVersionDTO versionTwo = new AutomationProjectVersionDTO(
            2, "DRAFT", null, null);

        when(automationProjectFacade.getProjectVersions(5L)).thenReturn(List.of(versionOne, versionTwo));

        this.graphQlTester
            .document("""
                query {
                    automationProjectVersions(id: "5") {
                        version
                        status
                        publishedDate
                        description
                    }
                }
                """)
            .execute()
            .path("automationProjectVersions")
            .entityList(Object.class)
            .hasSize(2)
            .path("automationProjectVersions[0].description")
            .entity(String.class)
            .isEqualTo("First release")
            .path("automationProjectVersions[0].version")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjectVersions[0].status")
            .entity(String.class)
            .isEqualTo("PUBLISHED")
            .path("automationProjectVersions[0].publishedDate")
            .entity(String.class)
            .isEqualTo("2026-01-01T10:00:00Z")
            .path("automationProjectVersions[1].version")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationProjectVersions[1].status")
            .entity(String.class)
            .isEqualTo("DRAFT")
            .path("automationProjectVersions[1].publishedDate")
            .valueIsNull();

        verify(automationProjectFacade).getProjectVersions(5L);
    }

    @Test
    void testAutomationProjectVersionWorkflowChanges() {
        when(automationProjectFacade.getProjectVersionWorkflowChanges(5L, 2))
            .thenReturn(new VersionWorkflowChanges(
                1, 2, List.of(new VersionWorkflowChangeSummary(
                    SOURCE_WORKFLOW_UUID, "Workflow One", WorkflowChangeType.MODIFIED, 3))));

        this.graphQlTester
            .document("""
                query {
                    automationProjectVersionWorkflowChanges(id: "5", projectVersion: 2) {
                        baseVersion
                        version
                        workflows {
                            workflowUuid
                            label
                            changeType
                            changeCount
                        }
                    }
                }
                """)
            .execute()
            .path("automationProjectVersionWorkflowChanges.baseVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjectVersionWorkflowChanges.version")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("automationProjectVersionWorkflowChanges.workflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo(SOURCE_WORKFLOW_UUID)
            .path("automationProjectVersionWorkflowChanges.workflows[0].changeType")
            .entity(String.class)
            .isEqualTo("MODIFIED")
            .path("automationProjectVersionWorkflowChanges.workflows[0].changeCount")
            .entity(Integer.class)
            .isEqualTo(3);

        verify(automationProjectFacade).getProjectVersionWorkflowChanges(5L, 2);
    }

    @Test
    void testAutomationProjectVersionWorkflowChange() {
        when(automationProjectFacade.getProjectVersionWorkflowChange(5L, 2, SOURCE_WORKFLOW_UUID))
            .thenReturn(new VersionWorkflowChange(
                SOURCE_WORKFLOW_UUID, "Workflow One", WorkflowChangeType.MODIFIED, 1, 2,
                List.of(new WorkflowDefinitionChange(
                    WorkflowNodeChangeKind.MODIFIED, WorkflowNodeChangeTarget.TASK, "gmail_1", "gmail/v1/sendEmail",
                    "Send", null, List.of(new WorkflowDefinitionValueChange("parameters.to", "a", "b")))),
                "{}", "{\"label\":\"x\"}"));

        this.graphQlTester
            .document("""
                query($workflowUuid: String!) {
                    automationProjectVersionWorkflowChange(
                        id: "5", projectVersion: 2, workflowUuid: $workflowUuid) {
                        workflowUuid
                        changeType
                        baseVersion
                        version
                        newDefinition
                        changes {
                            kind
                            target
                            valueChanges {
                                path
                            }
                        }
                    }
                }
                """)
            .variable("workflowUuid", SOURCE_WORKFLOW_UUID)
            .execute()
            .path("automationProjectVersionWorkflowChange.changeType")
            .entity(String.class)
            .isEqualTo("MODIFIED")
            .path("automationProjectVersionWorkflowChange.baseVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("automationProjectVersionWorkflowChange.newDefinition")
            .entity(String.class)
            .isEqualTo("{\"label\":\"x\"}")
            .path("automationProjectVersionWorkflowChange.changes[0].kind")
            .entity(String.class)
            .isEqualTo("MODIFIED")
            .path("automationProjectVersionWorkflowChange.changes[0].target")
            .entity(String.class)
            .isEqualTo("TASK")
            .path("automationProjectVersionWorkflowChange.changes[0].valueChanges[0].path")
            .entity(String.class)
            .isEqualTo("parameters.to");

        verify(automationProjectFacade).getProjectVersionWorkflowChange(5L, 2, SOURCE_WORKFLOW_UUID);
    }

    @Test
    void testDuplicateAutomationProjectWorkflowDelegatesToFacade() {
        when(automationProjectFacade.duplicateProjectWorkflow(SOURCE_WORKFLOW_UUID))
            .thenReturn(NEW_WORKFLOW_UUID);

        this.graphQlTester
            .document("""
                mutation($workflowUuid: ID!) {
                    duplicateAutomationProjectWorkflow(workflowUuid: $workflowUuid)
                }
                """)
            .variable("workflowUuid", SOURCE_WORKFLOW_UUID)
            .execute()
            .path("duplicateAutomationProjectWorkflow")
            .entity(String.class)
            .isEqualTo(NEW_WORKFLOW_UUID);

        verify(automationProjectFacade).duplicateProjectWorkflow(SOURCE_WORKFLOW_UUID);
    }

    @Test
    void testDuplicateAutomationProjectDelegatesToFacade() {
        when(automationProjectFacade.duplicateProject(3L)).thenReturn(99L);

        this.graphQlTester
            .document("""
                mutation {
                    duplicateAutomationProject(id: "3")
                }
                """)
            .execute()
            .path("duplicateAutomationProject")
            .entity(String.class)
            .isEqualTo("99");

        verify(automationProjectFacade).duplicateProject(3L);
    }
}
