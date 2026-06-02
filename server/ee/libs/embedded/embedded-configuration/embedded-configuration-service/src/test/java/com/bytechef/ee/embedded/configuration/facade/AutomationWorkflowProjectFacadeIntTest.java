/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationWorkflowProjectFacadeIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, TriggerDefinitionFacade.class,
    ComponentConnectionFacade.class,
    ComponentDefinitionService.class, ConnectedUserService.class, ConnectionDefinitionService.class,
    ConnectionFacade.class, ConnectionLifecycleFacade.class, ConnectionService.class,
    EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceConfigurationWorkflowService.class,
    McpIntegrationInstanceToolService.class, McpServerService.class, McpToolService.class,
    OAuth2ParametersFacade.class,
    OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectDeploymentFacade.class,
    ProjectDeploymentService.class, ProjectDeploymentWorkflowService.class, ProjectFacade.class,
    TaskExecutionService.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class,
    WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
public class AutomationWorkflowProjectFacadeIntTest {

    private static final String TEST_EXTERNAL_USER_ID = "test-external-user-42";

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private TagService tagService;

    @BeforeEach
    void setUp() {
        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), null, true, TEST_EXTERNAL_USER_ID, 1L, null, 0);

        when(connectedUserService.getConnectedUser(TEST_EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUser);
    }

    @Test
    void testCopyWorkflowTemplate() {
        long projectId = automationWorkflowProjectFacade.createProject("Onboarding", "", null, List.of());
        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null);
        automationWorkflowProjectFacade.publishProject(projectId);

        String publishedWorkflowUuid = automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .filter(project -> project.id() == projectId)
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .findFirst()
            .map(ConnectedUserWorkflowTemplateDTO::workflowUuid)
            .orElseThrow();

        String newWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            TEST_EXTERNAL_USER_ID, publishedWorkflowUuid, Environment.PRODUCTION);

        assertThat(newWorkflowUuid).isNotBlank()
            .isNotEqualTo(publishedWorkflowUuid);
    }

    @Test
    void testCopyWorkflowTemplateUnknownIdThrows() {
        assertThatThrownBy(() -> connectedUserProjectFacade
            .copyWorkflowTemplate(TEST_EXTERNAL_USER_ID, "nope", Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCopyWorkflowTemplateUnpublishedThrows() {
        long projectId = automationWorkflowProjectFacade.createProject("Unpublished", "", null, List.of());
        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null);

        String draftWorkflowUuid = automationWorkflowProjectFacade.getProject(projectId)
            .workflowTemplates()
            .getFirst()
            .workflowUuid();

        assertThatThrownBy(
            () -> connectedUserProjectFacade
                .copyWorkflowTemplate(TEST_EXTERNAL_USER_ID, draftWorkflowUuid, Environment.PRODUCTION))
                    .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAndGetProject() {
        long projectId = automationWorkflowProjectFacade.createProject(
            "Onboarding", "Onboarding flows", null, List.of());

        AutomationWorkflowProjectDTO project = automationWorkflowProjectFacade.getProject(projectId);

        assertThat(project.name()).isEqualTo("Onboarding");
        assertThat(project.description()).isEqualTo("Onboarding flows");
        assertThat(automationWorkflowProjectFacade.getProjects()).extracting(AutomationWorkflowProjectDTO::id)
            .contains(projectId);
    }

    @Test
    void testCreateProjectWorkflow() {
        long projectId = automationWorkflowProjectFacade.createProject("Onboarding", "", null, List.of());

        String workflowUuid = automationWorkflowProjectFacade.createProjectWorkflow(projectId, null);

        assertThat(workflowUuid).isNotBlank();
        assertThat(automationWorkflowProjectFacade.getProject(projectId)
            .workflowTemplates())
                .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .contains(workflowUuid);
    }

    @Test
    void testDeleteProject() {
        long projectId = automationWorkflowProjectFacade.createProject("Temp", "", null, List.of());

        automationWorkflowProjectFacade.deleteProject(projectId);

        assertThat(automationWorkflowProjectFacade.getProjects()).extracting(AutomationWorkflowProjectDTO::id)
            .doesNotContain(projectId);
    }

    @Test
    void testPublishProject() {
        long publishedProjectId = automationWorkflowProjectFacade.createProject(
            "PublishedCatalog", "", null, List.of());

        automationWorkflowProjectFacade.createProjectWorkflow(publishedProjectId, null);
        automationWorkflowProjectFacade.publishProject(publishedProjectId);

        long unpublishedProjectId = automationWorkflowProjectFacade.createProject(
            "UnpublishedCatalog", "", null, List.of());

        automationWorkflowProjectFacade.createProjectWorkflow(unpublishedProjectId, null);

        List<AutomationWorkflowProjectDTO> publishedProjects = automationWorkflowProjectFacade.getPublishedProjects();

        AutomationWorkflowProjectDTO publishedProject = publishedProjects.stream()
            .filter(project -> project.id() == publishedProjectId)
            .findFirst()
            .orElseThrow();

        assertThat(publishedProject.workflowTemplates()).hasSize(1);

        AutomationWorkflowProjectDTO unpublishedProject = publishedProjects.stream()
            .filter(project -> project.id() == unpublishedProjectId)
            .findFirst()
            .orElseThrow();

        assertThat(unpublishedProject.workflowTemplates()).isEmpty();
    }

    @Test
    void testPublishProjectDoesNotAccumulateWorkflowTemplatesInAdminList() {
        long projectId = automationWorkflowProjectFacade.createProject("StableCatalog", "", null, List.of());

        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null);

        int workflowTemplateCountBeforePublish = automationWorkflowProjectFacade.getProject(projectId)
            .workflowTemplates()
            .size();

        automationWorkflowProjectFacade.publishProject(projectId);
        automationWorkflowProjectFacade.publishProject(projectId);

        assertThat(automationWorkflowProjectFacade.getProject(projectId)
            .workflowTemplates()).hasSize(workflowTemplateCountBeforePublish);
    }

    @Test
    void testWorkflowComponentsResolvedFromTaskDefinition() {
        String workflowDefinitionWithTask = """
            {
                "label": "Email Workflow",
                "description": "Sends emails",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Send Email",
                        "name": "sendEmail",
                        "type": "gmail/v1/send",
                        "parameters": {}
                    }
                ]
            }
            """;

        ComponentDefinition gmailDefinition = new ComponentDefinition("gmail");

        when(componentDefinitionService.fetchComponentDefinition(anyString(), any()))
            .thenReturn(Optional.of(gmailDefinition));

        long projectId = automationWorkflowProjectFacade.createProject("EmailCatalog", "", null, List.of());
        automationWorkflowProjectFacade.createProjectWorkflow(projectId, workflowDefinitionWithTask);

        AutomationWorkflowProjectDTO project = automationWorkflowProjectFacade.getProject(projectId);

        assertThat(project.workflowTemplates()).hasSize(1);

        ConnectedUserWorkflowTemplateDTO workflowTemplate = project.workflowTemplates()
            .getFirst();

        List<ConnectedUserWorkflowTemplateDTO.Component> components = workflowTemplate.components();

        assertThat(components).isNotNull();
        assertThat(components).hasSize(1);

        ConnectedUserWorkflowTemplateDTO.Component component = components.getFirst();

        assertThat(component.name()).isEqualTo("gmail");
    }

    @Test
    void testWorkflowComponentsNonNullForEmptyWorkflow() {
        long projectId = automationWorkflowProjectFacade.createProject("EmptyWorkflowCatalog", "", null, List.of());
        automationWorkflowProjectFacade.createProjectWorkflow(projectId, null);

        AutomationWorkflowProjectDTO project = automationWorkflowProjectFacade.getProject(projectId);

        List<ConnectedUserWorkflowTemplateDTO> connectedUserWorkflowTemplateDTOS = project.workflowTemplates();
        assertThat(connectedUserWorkflowTemplateDTOS).hasSize(1);

        ConnectedUserWorkflowTemplateDTO connectedUserWorkflowTemplateDTO = connectedUserWorkflowTemplateDTOS
            .getFirst();

        assertThat(connectedUserWorkflowTemplateDTO.components()).isNotNull();
    }

    @Test
    void testCreateProjectCreatesNewCategoryAndTags() {
        long projectId = automationWorkflowProjectFacade.createProject(
            "CatalogWithNewCategoryAndTags", "", "Automation", List.of("crm", "erp"));

        AutomationWorkflowProjectDTO project = automationWorkflowProjectFacade.getProject(projectId);

        assertThat(project.categoryId()).isNotNull();
        assertThat(project.tagIds()).hasSize(2);

        assertThat(categoryService.getCategories())
            .extracting(Category::getName)
            .contains("Automation");

        assertThat(tagService.getTags())
            .extracting(Tag::getName)
            .contains("crm", "erp");
    }

    @Test
    void testCreateProjectReusesExistingCategoryByName() {
        long firstProjectId = automationWorkflowProjectFacade.createProject(
            "FirstCatalogProject", "", "Reusable", List.of());
        long secondProjectId = automationWorkflowProjectFacade.createProject(
            "SecondCatalogProject", "", "Reusable", List.of());

        AutomationWorkflowProjectDTO firstProject = automationWorkflowProjectFacade.getProject(firstProjectId);
        AutomationWorkflowProjectDTO secondProject = automationWorkflowProjectFacade.getProject(secondProjectId);

        assertThat(firstProject.categoryId()).isNotNull();
        assertThat(secondProject.categoryId()).isNotNull();
        assertThat(firstProject.categoryId()).isEqualTo(secondProject.categoryId());

        long reusableCategoryCount = categoryService.getCategories()
            .stream()
            .filter(category -> "Reusable".equals(category.getName()))
            .count();

        assertThat(reusableCategoryCount).isEqualTo(1);
    }
}
