/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.ownership.OwnerContributorMissingException;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectOwnershipServiceImpl;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfiguration;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiCollectionRepository;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ApiCollectionIntTestConfiguration.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
@ApiCollectionIntTestConfigurationSharedMocks
class ApiCollectionFacadeIntTest {

    private static final long DEFAULT_WORKSPACE_ID = 1049;

    @Autowired
    private ApiCollectionFacade apiCollectionFacade;

    @Autowired
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Autowired
    private ApiCollectionRepository apiCollectionRepository;

    @Autowired
    private ApiCollectionService apiCollectionService;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkflowService workflowService;

    private Project project;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        project = Project.builder()
            .name("test-project")
            .workspaceId(DEFAULT_WORKSPACE_ID)
            .build();

        project.publish("v1");

        project = projectRepository.save(project);
    }

    @AfterEach
    void afterEach() {
        jdbcClient.sql("DELETE FROM api_collection_endpoint")
            .update();

        apiCollectionRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();

        if (workflowId != null) {
            workflowService.delete(workflowId);

            workflowId = null;
        }
    }

    @Test
    void testCreateApiCollectionStoresIt() {
        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("test-collection"));

        assertThat(apiCollectionService.getApiCollection(apiCollectionDTO.id()))
            .satisfies(apiCollection -> {
                assertThat(apiCollection.getName()).isEqualTo("test-collection");
                assertThat(apiCollection.getProjectDeploymentId()).isEqualTo(apiCollectionDTO.projectDeploymentId());
            });
    }

    @Test
    void testCreateApiCollectionNamesItsDeploymentAfterTheCollectionAndHidesIt() {
        ProjectDeployment plainProjectDeployment = new ProjectDeployment();

        plainProjectDeployment.setEnvironment(Environment.DEVELOPMENT);
        plainProjectDeployment.setName("plain-deployment");
        plainProjectDeployment.setProjectId(project.getId());
        plainProjectDeployment.setProjectVersion(1);

        plainProjectDeployment = projectDeploymentService.create(plainProjectDeployment);

        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("test-collection"));

        ProjectDeployment apiCollectionProjectDeployment = projectDeploymentService.getProjectDeployment(
            apiCollectionDTO.projectDeploymentId());

        assertThat(apiCollectionProjectDeployment.getName()).isEqualTo("test-collection");
        assertThat(projectDeploymentService.getProjectDeployments(null, null, null, null, null))
            .extracting(ProjectDeployment::getId)
            .containsExactly(plainProjectDeployment.getId());
    }

    @Test
    void testStartupCheckFindsTheApiCollectionRowsInTheOwnerTable() {
        apiCollectionFacade.createApiCollection(getApiCollectionDTO("test-collection"));

        ProjectOwnershipServiceImpl projectOwnershipService = new ProjectOwnershipServiceImpl(
            jdbcClient, List.of(), List.of());

        assertThatThrownBy(projectOwnershipService::afterSingletonsInstantiated)
            .isInstanceOfSatisfying(
                OwnerContributorMissingException.class,
                exception -> assertThat(exception.getProjectDeploymentOwners())
                    .contains(ProjectDeploymentOwner.API_COLLECTION));
    }

    @Test
    void testGetOpenApiSpecificationSkipsDisabledFirstTrigger() {
        Workflow workflow = workflowService.create(
            JsonUtils.write(
                Map.of(
                    "label", "api-workflow",
                    "triggers", List.of(
                        Map.of("name", "trigger_1", "type", "webhook/v1/autoRespondingWithHTTP200", "disabled", true),
                        Map.of("name", "trigger_2", "type", "apiPlatform/v1/newApiRequest", "parameters", Map.of())),
                    "tasks", List.of())),
            Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();

        ApiCollectionDTO apiCollectionDTO = apiCollectionFacade.createApiCollection(
            getApiCollectionDTO("test-collection"));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(apiCollectionDTO.projectDeploymentId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        projectDeploymentWorkflow = projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(apiCollectionDTO.id());
        apiCollectionEndpoint.setHttpMethod(ApiCollectionEndpoint.HttpMethod.GET);
        apiCollectionEndpoint.setName("test-endpoint");
        apiCollectionEndpoint.setPath("/test");
        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflow.getId());

        apiCollectionEndpointService.create(apiCollectionEndpoint);

        assertThat(apiCollectionFacade.getOpenApiSpecification(apiCollectionDTO.id())).contains("/test");
    }

    private ApiCollectionDTO getApiCollectionDTO(String name) {
        return new ApiCollectionDTO(
            1, "test-context-path", null, null, "test description", false, List.of(), Environment.DEVELOPMENT, null,
            null, null, name, null, project.getId(), null, 0, 1, List.of(), 0);
    }
}
