/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.apiplatform.configuration.audit.ApiCollectionAuditPublisher;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ApiCollectionFacadeTest {

    @Mock
    private ApiCollectionAuditPublisher apiCollectionAuditPublisher;

    @Mock
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Mock
    private ApiCollectionService apiCollectionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private TagService tagService;

    @InjectMocks
    private ApiCollectionFacadeImpl apiCollectionFacade;

    @Test
    void testCreateApiCollectionThrowsWhenNameAlreadyExists() {
        Project project = new Project();

        project.setWorkspaceId(5L);

        when(projectService.getProject(42L)).thenReturn(project);
        when(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.STAGING, null))
            .thenReturn(true);

        ApiCollectionDTO apiCollectionDTO = createApiCollectionDTO();

        assertThatThrownBy(() -> apiCollectionFacade.createApiCollection(apiCollectionDTO))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(ApiCollectionErrorType.NAME_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void testCreateApiCollectionDoesNotThrowWhenNameAvailable() {
        Project project = new Project();

        project.setWorkspaceId(5L);

        when(projectService.getProject(42L)).thenReturn(project);
        when(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.STAGING, null))
            .thenReturn(false);
        when(projectDeploymentService.create(any())).thenAnswer(invocation -> {
            ProjectDeployment projectDeployment = invocation.getArgument(0);

            projectDeployment.setId(200L);

            return projectDeployment;
        });
        when(apiCollectionService.create(any())).thenAnswer(invocation -> {
            ApiCollection apiCollection = invocation.getArgument(0);

            apiCollection.setId(100L);

            return apiCollection;
        });
        when(projectService.getProjectDeploymentProject(200L)).thenReturn(project);

        ProjectDeployment savedProjectDeployment = new ProjectDeployment();

        savedProjectDeployment.setId(200L);
        savedProjectDeployment.setProjectId(42L);
        savedProjectDeployment.setProjectVersion(3);
        savedProjectDeployment.setEnvironment(Environment.STAGING);

        when(projectDeploymentService.getProjectDeployment(200L)).thenReturn(savedProjectDeployment);

        ApiCollectionDTO apiCollectionDTO = createApiCollectionDTO();

        ApiCollectionDTO created = apiCollectionFacade.createApiCollection(apiCollectionDTO);

        assertThat(created.name()).isEqualTo("billing");
        assertThat(created.id()).isEqualTo(100L);
    }

    @Test
    void testUpdateApiCollectionThrowsWhenAnotherCollectionOwnsName() {
        stubPersistedApiCollectionAndDeployment();

        when(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.STAGING, 100L))
            .thenReturn(true);

        ApiCollectionDTO apiCollectionDTO = updateApiCollectionDTO("billing");

        assertThatThrownBy(() -> apiCollectionFacade.updateApiCollection(apiCollectionDTO))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(ApiCollectionErrorType.NAME_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void testUpdateApiCollectionDoesNotThrowWhenRenamingToOwnName() {
        stubPersistedApiCollectionAndDeployment();

        // excludeId (the collection's own persisted id) genuinely excludes the row being updated, so a name only
        // this collection owns must not be reported as a collision.
        when(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.STAGING, 100L))
            .thenReturn(false);
        when(apiCollectionService.update(any())).thenAnswer(invocation -> {
            ApiCollection updatedApiCollection = invocation.getArgument(0);

            // Mirrors ApiCollectionServiceImpl.update(), which loads the persisted row and never copies
            // projectDeploymentId off the incoming argument, so the returned entity keeps its own deployment id.
            updatedApiCollection.setProjectDeploymentId(200L);

            return updatedApiCollection;
        });

        ApiCollectionDTO apiCollectionDTO = updateApiCollectionDTO("billing");

        ApiCollectionDTO updated = apiCollectionFacade.updateApiCollection(apiCollectionDTO);

        assertThat(updated.name()).isEqualTo("billing");
    }

    @Test
    void testUpdateApiCollectionResolvesDeploymentFromPersistedEntityNotFromDto() {
        stubPersistedApiCollectionAndDeployment();

        when(apiCollectionService.existsByNameAndEnvironment("billing", 5L, Environment.STAGING, 100L))
            .thenReturn(false);
        when(apiCollectionService.update(any())).thenAnswer(invocation -> {
            ApiCollection updatedApiCollection = invocation.getArgument(0);

            updatedApiCollection.setProjectDeploymentId(200L);

            return updatedApiCollection;
        });

        ApiCollectionDTO apiCollectionDTO = updateApiCollectionDTO("billing");

        assertThat(apiCollectionDTO.projectDeploymentId()).isZero();

        ApiCollectionDTO updated = apiCollectionFacade.updateApiCollection(apiCollectionDTO);

        assertThat(updated.name()).isEqualTo("billing");

        // The regression: the guard must resolve the deployment (200L) from the persisted entity, never from the
        // client-supplied DTO's readOnly projectDeploymentId (0L, per toApiCollection()'s primitive default).
        verify(projectDeploymentService, never()).getProjectDeployment(0L);
        verify(projectService, never()).getProjectDeploymentProject(0L);
    }

    /**
     * The pointer is what ties an endpoint to the workflow it serves, and every {@code api_collection_endpoint} row FKs
     * to it. Because {@link ApiCollectionEndpointDTO#toApiCollectionEndpoint()} does not map it, an update whose DTO
     * carries no pointer must leave the persisted one alone rather than blanking it.
     */
    @Test
    void testUpdateApiCollectionEndpointKeepsProjectDeploymentWorkflowWhenTheWorkflowIsUnchanged() {
        stubEndpointUpdate(901L);

        apiCollectionFacade.updateApiCollectionEndpoint(endpointDTO("wf-uuid"));

        assertThat(capturedUpdatedEndpoint().getProjectDeploymentWorkflowId()).isEqualTo(901L);
    }

    @Test
    void testUpdateApiCollectionEndpointRepointsProjectDeploymentWorkflowWhenTheWorkflowChanged() {
        stubEndpointUpdate(902L);

        apiCollectionFacade.updateApiCollectionEndpoint(endpointDTO("other-wf-uuid"));

        assertThat(capturedUpdatedEndpoint().getProjectDeploymentWorkflowId()).isEqualTo(902L);
    }

    /**
     * A uuid naming no workflow of this collection is bad caller input, not a server fault: before the endpoint update
     * learned to re-resolve the pointer it was silently ignored, and the naive re-resolution reported it as an untyped
     * {@code IllegalArgumentException} — a 500.
     */
    @Test
    void testUpdateApiCollectionEndpointRejectsAWorkflowUuidOutsideTheCollection() {
        ApiCollectionEndpoint persistedApiCollectionEndpoint = new ApiCollectionEndpoint();

        persistedApiCollectionEndpoint.setId(501L);
        persistedApiCollectionEndpoint.setApiCollectionId(100L);
        persistedApiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

        when(apiCollectionEndpointService.getOpenApiEndpoint(501L)).thenReturn(persistedApiCollectionEndpoint);

        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(100L);
        apiCollection.setProjectDeploymentId(200L);

        when(apiCollectionService.getApiCollection(100L)).thenReturn(apiCollection);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, "foreign-wf-uuid"))
            .thenReturn(Optional.empty());

        ApiCollectionEndpointDTO apiCollectionEndpointDTO = endpointDTO("foreign-wf-uuid");

        assertThatThrownBy(() -> apiCollectionFacade.updateApiCollectionEndpoint(apiCollectionEndpointDTO))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(ApiCollectionErrorType.WORKFLOW_NOT_FOUND.getErrorKey());

        verify(apiCollectionEndpointService, never()).update(any());
    }

    @Test
    void testUpdateApiCollectionEndpointRejectsAMalformedWorkflowUuidTheSameWay() {
        ApiCollectionEndpoint persistedApiCollectionEndpoint = new ApiCollectionEndpoint();

        persistedApiCollectionEndpoint.setId(501L);
        persistedApiCollectionEndpoint.setApiCollectionId(100L);
        persistedApiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

        when(apiCollectionEndpointService.getOpenApiEndpoint(501L)).thenReturn(persistedApiCollectionEndpoint);

        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(100L);
        apiCollection.setProjectDeploymentId(200L);

        when(apiCollectionService.getApiCollection(100L)).thenReturn(apiCollection);

        // UUID.fromString throws before the repository is reached; the caller must not be able to tell that apart
        // from a well-formed uuid naming nothing.
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, "not-a-uuid"))
            .thenThrow(new IllegalArgumentException("Invalid UUID string: not-a-uuid"));

        ApiCollectionEndpointDTO apiCollectionEndpointDTO = endpointDTO("not-a-uuid");

        assertThatThrownBy(() -> apiCollectionFacade.updateApiCollectionEndpoint(apiCollectionEndpointDTO))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(ApiCollectionErrorType.WORKFLOW_NOT_FOUND.getErrorKey());
    }

    private ApiCollectionEndpoint capturedUpdatedEndpoint() {
        ArgumentCaptor<ApiCollectionEndpoint> apiCollectionEndpointCaptor =
            ArgumentCaptor.forClass(ApiCollectionEndpoint.class);

        verify(apiCollectionEndpointService).update(apiCollectionEndpointCaptor.capture());

        return apiCollectionEndpointCaptor.getValue();
    }

    /**
     * Stubs the whole update path so the endpoint resolves onto the project deployment workflow
     * {@code resolvedProjectDeploymentWorkflowId}. The persisted row points at 901, so passing 902 models a source
     * endpoint that moved onto another workflow.
     */
    private void stubEndpointUpdate(long resolvedProjectDeploymentWorkflowId) {
        ApiCollectionEndpoint persistedApiCollectionEndpoint = new ApiCollectionEndpoint();

        persistedApiCollectionEndpoint.setId(501L);
        persistedApiCollectionEndpoint.setApiCollectionId(100L);
        persistedApiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
        persistedApiCollectionEndpoint.setName("getOrders");
        persistedApiCollectionEndpoint.setPath("orders");
        persistedApiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

        when(apiCollectionEndpointService.getOpenApiEndpoint(501L)).thenReturn(persistedApiCollectionEndpoint);

        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(100L);
        apiCollection.setProjectDeploymentId(200L);

        when(apiCollectionService.getApiCollection(100L)).thenReturn(apiCollection);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(eq(200L), any()))
            .thenReturn(Optional.of("workflow-1"));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(resolvedProjectDeploymentWorkflowId);
        projectDeploymentWorkflow.setProjectDeploymentId(200L);
        projectDeploymentWorkflow.setWorkflowId("workflow-1");

        when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(200L, "workflow-1"))
            .thenReturn(Optional.of(projectDeploymentWorkflow));
        when(apiCollectionEndpointService.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(resolvedProjectDeploymentWorkflowId))
            .thenReturn(projectDeploymentWorkflow);
        when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-1")).thenReturn("wf-uuid");
    }

    private static ApiCollectionEndpointDTO endpointDTO(String workflowUuid) {
        return new ApiCollectionEndpointDTO(
            100L, null, null, true, HttpMethod.GET, 501L, null, null, "listOrders", "orders", 901L, 0, workflowUuid);
    }

    private void stubPersistedApiCollectionAndDeployment() {
        ApiCollection persistedApiCollection = new ApiCollection();

        persistedApiCollection.setId(100L);
        persistedApiCollection.setProjectDeploymentId(200L);

        when(apiCollectionService.getApiCollection(100L)).thenReturn(persistedApiCollection);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(200L);
        projectDeployment.setProjectId(42L);
        projectDeployment.setProjectVersion(3);
        projectDeployment.setEnvironment(Environment.STAGING);

        when(projectDeploymentService.getProjectDeployment(200L)).thenReturn(projectDeployment);

        Project project = new Project();

        project.setWorkspaceId(5L);

        when(projectService.getProjectDeploymentProject(200L)).thenReturn(project);
    }

    private static ApiCollectionDTO createApiCollectionDTO() {
        return new ApiCollectionDTO(
            1, "billing-v1", null, null, null, false, List.of(), Environment.STAGING, null, null, null, "billing",
            null, 42L, null, 0, 3, List.of(), 0, null);
    }

    private static ApiCollectionDTO updateApiCollectionDTO(String name) {
        // id=100L matches the persisted collection stubbed by stubPersistedApiCollectionAndDeployment();
        // projectDeploymentId=0L mirrors production reality — the REST model marks projectDeploymentId
        // readOnly, so a PUT body never carries it and toApiCollection() falls back to the primitive default.
        return new ApiCollectionDTO(
            1, "billing-v1", null, null, null, false, List.of(), Environment.STAGING, 100L, null, null, name,
            null, 42L, null, 0, 3, List.of(), 0, null);
    }
}
