/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfiguration;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ApiCollectionIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ApiCollectionIntTestConfigurationSharedMocks
class ApiCollectionDeleteCascadeIntTest {

    private static final long DEFAULT_WORKSPACE_ID = 1049;

    private static final String WORKFLOW_DEFINITION = """
        {
            "label": "API endpoint",
            "tasks": []
        }
        """;

    @Autowired
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Autowired
    private ApiCollectionRepository apiCollectionRepository;

    @Autowired
    private ApiCollectionService apiCollectionService;

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkflowService workflowService;

    private final List<String> workflowIds = new ArrayList<>();

    private Project project;

    @BeforeEach
    void beforeEach() {
        Project newProject = Project.builder()
            .name("test-project")
            .workspaceId(DEFAULT_WORKSPACE_ID)
            .build();

        newProject.publish("v1");

        project = projectRepository.save(newProject);
    }

    @AfterEach
    void afterEach() {
        apiEndpointRepository.deleteAll();
        apiCollectionRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();

        for (String workflowId : workflowIds) {
            if (workflowService.fetchWorkflow(workflowId)
                .isPresent()) {

                workflowService.delete(workflowId);
            }
        }
    }

    @Test
    void testDeleteWorkflowDeletesItsApiCollectionEndpointsAndTheirDeploymentWorkflows() {
        String workflowId = createProjectWorkflow();
        String otherWorkflowId = createProjectWorkflow();

        ProjectDeployment projectDeployment = createProjectDeployment("test-collection");

        ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
            projectDeployment, workflowId);
        ProjectDeploymentWorkflow otherProjectDeploymentWorkflow = createProjectDeploymentWorkflow(
            projectDeployment, otherWorkflowId);

        ApiCollection apiCollection = createApiCollection(projectDeployment, "test-collection");

        createApiCollectionEndpoint(apiCollection, projectDeploymentWorkflow, "first");

        ApiCollectionEndpoint otherApiCollectionEndpoint = createApiCollectionEndpoint(
            apiCollection, otherProjectDeploymentWorkflow, "second");

        projectWorkflowFacade.deleteWorkflow(workflowId);

        assertThat(apiEndpointRepository.findByApiCollectionIdOrderByHttpMethod(apiCollection.getId()))
            .extracting(ApiCollectionEndpoint::getId)
            .containsExactly(otherApiCollectionEndpoint.getId());
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(workflowId)).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(otherWorkflowId))
            .extracting(ProjectDeploymentWorkflow::getId)
            .containsExactly(otherProjectDeploymentWorkflow.getId());
        assertThat(apiCollectionRepository.findById(apiCollection.getId())).isPresent();
    }

    @Test
    void testDeleteProjectDeploymentDeletesItsApiCollections() {
        String workflowId = createProjectWorkflow();

        ProjectDeployment projectDeployment = createProjectDeployment("test-collection");
        ProjectDeployment otherProjectDeployment = createProjectDeployment("other-test-collection");

        ApiCollection apiCollection = createApiCollection(projectDeployment, "test-collection");
        ApiCollection otherApiCollection = createApiCollection(otherProjectDeployment, "other-test-collection");

        createApiCollectionEndpoint(
            apiCollection, createProjectDeploymentWorkflow(projectDeployment, workflowId), "first");

        ApiCollectionEndpoint otherApiCollectionEndpoint = createApiCollectionEndpoint(
            otherApiCollection, createProjectDeploymentWorkflow(otherProjectDeployment, workflowId), "second");

        projectDeploymentFacade.deleteProjectDeployment(projectDeployment.getId());

        assertThat(apiCollectionRepository.findById(apiCollection.getId())).isEmpty();
        assertThat(apiEndpointRepository.findByApiCollectionIdOrderByHttpMethod(apiCollection.getId())).isEmpty();
        assertThat(projectDeploymentRepository.findById(projectDeployment.getId())).isEmpty();
        assertThat(apiCollectionRepository.findById(otherApiCollection.getId())).isPresent();
        assertThat(apiEndpointRepository.findById(otherApiCollectionEndpoint.getId())).isPresent();
    }

    @Test
    void testDeleteProjectDeletesItsApiCollections() {
        String workflowId = createProjectWorkflow();

        ProjectDeployment projectDeployment = createProjectDeployment("test-collection");

        ApiCollection apiCollection = createApiCollection(projectDeployment, "test-collection");

        createApiCollectionEndpoint(
            apiCollection, createProjectDeploymentWorkflow(projectDeployment, workflowId), "first");

        projectFacade.deleteProject(project.getId());

        assertThat(apiCollectionRepository.findAll()).isEmpty();
        assertThat(apiEndpointRepository.findAll()).isEmpty();
        assertThat(projectDeploymentWorkflowRepository.findAll()).isEmpty();
        assertThat(projectDeploymentRepository.findAll()).isEmpty();
        assertThat(projectRepository.findById(project.getId())).isEmpty();
    }

    private ApiCollection createApiCollection(ProjectDeployment projectDeployment, String name) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setCollectionVersion(1);
        apiCollection.setContextPath(name);
        apiCollection.setName(name);
        apiCollection.setProjectDeploymentId(projectDeployment.getId());

        return apiCollectionService.create(apiCollection);
    }

    private ApiCollectionEndpoint createApiCollectionEndpoint(
        ApiCollection apiCollection, ProjectDeploymentWorkflow projectDeploymentWorkflow, String path) {

        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(apiCollection.getId());
        apiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
        apiCollectionEndpoint.setName(path);
        apiCollectionEndpoint.setPath(path);
        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflow.getId());

        return apiCollectionEndpointService.create(apiCollectionEndpoint);
    }

    private ProjectDeployment createProjectDeployment(String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);

        return projectDeploymentRepository.save(projectDeployment);
    }

    private ProjectDeploymentWorkflow createProjectDeploymentWorkflow(
        ProjectDeployment projectDeployment, String workflowId) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
    }

    private String createProjectWorkflow() {
        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String workflowId = workflow.getId();

        workflowIds.add(workflowId);

        projectWorkflowRepository.save(new ProjectWorkflow(project.getId(), 1, workflowId, UUID.randomUUID()));

        return workflowId;
    }
}
