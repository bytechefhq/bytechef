/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingConnection;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionVersionUpdateDTO.MissingInput;
import com.bytechef.ee.automation.apiplatform.configuration.exception.ApiCollectionErrorType;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiEndpointRepository;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointServiceImpl;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls {@link ApiCollectionFacadeImpl} through Spring Security's real {@code @PreAuthorize} method interceptor, backed
 * by the real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}. Every collaborator a method
 * body touches first throws, so an allowed call proves it entered the body and a denied one proves it never did.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionFacadeTest {

    private static final long API_COLLECTION_ENDPOINT_ID = 19L;
    private static final String API_COLLECTION_ENDPOINT_TYPE = "ApiCollectionEndpoint";
    private static final long API_COLLECTION_ID = 7L;
    private static final String API_COLLECTION_TYPE = "ApiCollection";
    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 13L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 23L;
    private static final long PROJECT_ID = 11L;
    private static final long TAG_ID = 29L;
    private static final long WORKSPACE_ID = 42L;
    private static final String WORKSPACE_TYPE = "Workspace";

    private final ApiCollectionEndpointService apiCollectionEndpointService = mock(ApiCollectionEndpointService.class);
    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    private ApiCollectionFacade apiCollectionFacade;

    @BeforeEach
    void beforeEach() {
        IllegalStateException bodyReachedException = new IllegalStateException(BODY_REACHED);

        when(apiCollectionEndpointService.getOpenApiEndpoint(anyLong())).thenThrow(bodyReachedException);
        when(apiCollectionEndpointService.getApiEndpoints(anyLong())).thenThrow(bodyReachedException);
        when(apiCollectionEndpointService.update(any(ApiCollectionEndpoint.class))).thenThrow(bodyReachedException);
        when(apiCollectionService.getApiCollection(anyLong())).thenThrow(bodyReachedException);
        when(apiCollectionService.getApiCollectionProjectIds(anyLong())).thenThrow(bodyReachedException);
        when(apiCollectionService.getApiCollections(anyLong(), any(), any(), any())).thenThrow(bodyReachedException);
        when(apiCollectionService.update(any(ApiCollection.class))).thenThrow(bodyReachedException);
        when(apiCollectionService.update(anyLong(), anyList())).thenThrow(bodyReachedException);
        when(environmentService.getEnvironment(anyLong())).thenThrow(bodyReachedException);
        when(projectDeploymentService.create(any(ProjectDeployment.class))).thenThrow(bodyReachedException);

        doThrow(bodyReachedException).when(apiCollectionEndpointService)
            .delete(anyLong());

        apiCollectionFacade = secure(
            new ApiCollectionFacadeImpl(
                apiCollectionService, apiCollectionEndpointService, mock(ComponentConnectionFacade.class),
                environmentService,
                mock(ProjectDeploymentFacade.class), projectDeploymentService,
                mock(ProjectDeploymentWorkflowService.class), mock(ProjectService.class),
                mock(ProjectWorkflowService.class), mock(TagService.class), mock(WorkflowService.class)));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateApiCollectionDeniesWhenTheApiPlatformCreateScopeIsRefusedOnTheProjectInItsEnvironment() {
        assertCreateApiCollectionEnvironmentGuard(false);
    }

    @Test
    void testCreateApiCollectionAllowsWhenTheApiPlatformCreateScopeIsGrantedOnTheProjectInItsEnvironment() {
        assertCreateApiCollectionEnvironmentGuard(true);
    }

    @Test
    void testCreateApiCollectionDeniesWhenTheApiPlatformCreateScopeIsRefusedOnTheProjectWithoutAnEnvironment() {
        assertResourceGuard(() -> apiCollectionFacade.createApiCollection(apiCollectionDTO(null)), PROJECT_ID,
            "Project", "API_PLATFORM_CREATE", false);
    }

    @Test
    void testCreateApiCollectionAllowsWhenTheApiPlatformCreateScopeIsGrantedOnTheProjectWithoutAnEnvironment() {
        assertResourceGuard(() -> apiCollectionFacade.createApiCollection(apiCollectionDTO(null)), PROJECT_ID,
            "Project", "API_PLATFORM_CREATE", true);
    }

    @Test
    void testCreateApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.createApiCollectionEndpoint(apiCollectionEndpointDTO()),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testCreateApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.createApiCollectionEndpoint(apiCollectionEndpointDTO()),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testDeleteApiCollectionDeniesWhenTheApiPlatformDeleteScopeIsRefused() {
        assertResourceGuard(() -> apiCollectionFacade.deleteApiCollection(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_DELETE", false);
    }

    @Test
    void testDeleteApiCollectionAllowsWhenTheApiPlatformDeleteScopeIsGranted() {
        assertResourceGuard(() -> apiCollectionFacade.deleteApiCollection(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_DELETE", true);
    }

    @Test
    void testGetApiCollectionDeniesWhenTheApiPlatformViewScopeIsRefused() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollection(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetApiCollectionAllowsWhenTheApiPlatformViewScopeIsGranted() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollection(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testGetApiCollectionsDeniesWhenTheApiPlatformViewScopeIsRefusedInTheNamedEnvironment() {
        assertGetApiCollectionsGuard(false);
    }

    @Test
    void testGetApiCollectionsAllowsWhenTheApiPlatformViewScopeIsGrantedInTheNamedEnvironment() {
        assertGetApiCollectionsGuard(true);
    }

    @Test
    void testGetApiCollectionTagsDeniesWhenTheApiPlatformViewScopeIsRefusedOnTheWorkspace() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollectionTags(WORKSPACE_ID), WORKSPACE_ID, WORKSPACE_TYPE,
            "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetApiCollectionTagsAllowsWhenTheApiPlatformViewScopeIsGrantedOnTheWorkspace() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollectionTags(WORKSPACE_ID), WORKSPACE_ID, WORKSPACE_TYPE,
            "API_PLATFORM_VIEW", true);
    }

    @Test
    void testGetWorkspaceProjectsDeniesWhenTheApiPlatformViewScopeIsRefusedOnTheWorkspace() {
        assertResourceGuard(() -> apiCollectionFacade.getWorkspaceProjects(WORKSPACE_ID), WORKSPACE_ID, WORKSPACE_TYPE,
            "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetWorkspaceProjectsAllowsWhenTheApiPlatformViewScopeIsGrantedOnTheWorkspace() {
        assertResourceGuard(() -> apiCollectionFacade.getWorkspaceProjects(WORKSPACE_ID), WORKSPACE_ID, WORKSPACE_TYPE,
            "API_PLATFORM_VIEW", true);
    }

    @Test
    void testUpdateApiCollectionDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() {
        assertResourceGuard(
            () -> apiCollectionFacade.updateApiCollection(apiCollectionDTO(Environment.values()[(int) ENVIRONMENT_ID])),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() {
        assertResourceGuard(
            () -> apiCollectionFacade.updateApiCollection(apiCollectionDTO(Environment.values()[(int) ENVIRONMENT_ID])),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testDeleteApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.deleteApiCollectionEndpoint(API_COLLECTION_ENDPOINT_ID),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testDeleteApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.deleteApiCollectionEndpoint(API_COLLECTION_ENDPOINT_ID),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testEnableApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.enableApiCollectionEndpoint(API_COLLECTION_ENDPOINT_ID, true),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testEnableApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.enableApiCollectionEndpoint(API_COLLECTION_ENDPOINT_ID, true),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testGetApiCollectionEndpointDeniesWhenTheApiPlatformViewScopeIsRefusedOnTheEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollectionEndpoint(API_COLLECTION_ENDPOINT_ID),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetApiCollectionEndpointAllowsWhenTheApiPlatformViewScopeIsGrantedOnTheEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollectionEndpoint(API_COLLECTION_ENDPOINT_ID),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testGetApiCollectionEndpointsDeniesWhenTheApiPlatformViewScopeIsRefusedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollectionEndpoints(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetApiCollectionEndpointsAllowsWhenTheApiPlatformViewScopeIsGrantedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.getApiCollectionEndpoints(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testUpdateApiCollectionProjectVersionDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.updateApiCollectionProjectVersion(API_COLLECTION_ID, 2, false),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionProjectVersionAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.updateApiCollectionProjectVersion(API_COLLECTION_ID, 2, false),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testCreateApiCollectionEndpointWithConnectionsDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() {
        assertResourceGuard(
            () -> apiCollectionFacade.createApiCollectionEndpoint(apiCollectionEndpointDTO(), List.of(), null),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testCreateApiCollectionEndpointWithConnectionsAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() {
        assertResourceGuard(
            () -> apiCollectionFacade.createApiCollectionEndpoint(apiCollectionEndpointDTO(), List.of(), null),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testUpdateApiCollectionEndpointWithConnectionsDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheEndpoint() {
        assertResourceGuard(
            () -> apiCollectionFacade.updateApiCollectionEndpoint(apiCollectionEndpointDTO(), List.of(), null),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionEndpointWithConnectionsAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheEndpoint() {
        assertResourceGuard(
            () -> apiCollectionFacade.updateApiCollectionEndpoint(apiCollectionEndpointDTO(), List.of(), null),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testGetOpenApiSpecificationDeniesWhenTheApiPlatformViewScopeIsRefused() {
        assertResourceGuard(() -> apiCollectionFacade.getOpenApiSpecification(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetOpenApiSpecificationAllowsWhenTheApiPlatformViewScopeIsGranted() {
        assertResourceGuard(() -> apiCollectionFacade.getOpenApiSpecification(API_COLLECTION_ID), API_COLLECTION_ID,
            API_COLLECTION_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testUpdateApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheStoredEndpoint() {
        // Keyed on the endpoint id, not the collection id the request carries: the update writes by endpoint id.
        assertResourceGuard(() -> apiCollectionFacade.updateApiCollectionEndpoint(apiCollectionEndpointDTO()),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheStoredEndpoint() {
        assertResourceGuard(() -> apiCollectionFacade.updateApiCollectionEndpoint(apiCollectionEndpointDTO()),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testUpdateApiCollectionEndpointWorkflowDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheEndpoint() {
        assertResourceGuard(
            () -> apiCollectionFacade.updateApiCollectionEndpointWorkflow(API_COLLECTION_ENDPOINT_ID, List.of(), null),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionEndpointWorkflowAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheEndpoint() {
        assertResourceGuard(
            () -> apiCollectionFacade.updateApiCollectionEndpointWorkflow(API_COLLECTION_ENDPOINT_ID, List.of(), null),
            API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testUpdateApiCollectionTagsDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.updateApiCollectionTags(API_COLLECTION_ID, List.of()),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionTagsAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() {
        assertResourceGuard(() -> apiCollectionFacade.updateApiCollectionTags(API_COLLECTION_ID, List.of()),
            API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Nested
    class EndpointWorkflow {

        private final ApiCollectionEndpointService apiCollectionEndpointService =
            mock(ApiCollectionEndpointService.class);
        private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
            mock(ProjectDeploymentWorkflowService.class);
        private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        private final ApiCollectionFacade apiCollectionFacade = new ApiCollectionFacadeImpl(
            mock(ApiCollectionService.class), apiCollectionEndpointService, mock(ComponentConnectionFacade.class),
            mock(EnvironmentService.class), projectDeploymentFacade, mock(ProjectDeploymentService.class),
            projectDeploymentWorkflowService,
            mock(ProjectService.class), projectWorkflowService, mock(TagService.class), mock(WorkflowService.class));

        @Test
        void testEnableApiCollectionEndpointEnablesTheWorkflowOnTheBackingDeployment() {
            ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

            apiCollectionEndpoint.setApiCollectionId(100L);
            apiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
            apiCollectionEndpoint.setId(7L);
            apiCollectionEndpoint.setName("list-invoices");
            apiCollectionEndpoint.setPath("invoices");
            apiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setEnabled(false);
            projectDeploymentWorkflow.setProjectDeploymentId(200L);
            projectDeploymentWorkflow.setWorkflowId("workflow-1");

            when(apiCollectionEndpointService.getOpenApiEndpoint(7L)).thenReturn(apiCollectionEndpoint);
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L))
                .thenReturn(projectDeploymentWorkflow);
            when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-1")).thenReturn("uuid-1");
            doAnswer(invocation -> {
                projectDeploymentWorkflow.setEnabled(invocation.getArgument(2));

                return null;
            }).when(projectDeploymentFacade)
                .enableProjectDeploymentWorkflow(200L, "workflow-1", true);

            ApiCollectionEndpointDTO apiCollectionEndpointDTO =
                apiCollectionFacade.enableApiCollectionEndpoint(7L, true);

            verify(projectDeploymentFacade).enableProjectDeploymentWorkflow(200L, "workflow-1", true);

            assertThat(apiCollectionEndpointDTO.id()).isEqualTo(7L);
            assertThat(apiCollectionEndpointDTO.enabled()).isTrue();
        }

        @Test
        void testGetApiCollectionEndpointReturnsTheEndpointWithItsWorkflowUuid() {
            ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

            apiCollectionEndpoint.setApiCollectionId(100L);
            apiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
            apiCollectionEndpoint.setId(7L);
            apiCollectionEndpoint.setName("list-invoices");
            apiCollectionEndpoint.setPath("invoices");
            apiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setProjectDeploymentId(200L);
            projectDeploymentWorkflow.setWorkflowId("workflow-1");

            when(apiCollectionEndpointService.getOpenApiEndpoint(7L)).thenReturn(apiCollectionEndpoint);
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L))
                .thenReturn(projectDeploymentWorkflow);
            when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-1")).thenReturn("uuid-1");

            ApiCollectionEndpointDTO apiCollectionEndpointDTO = apiCollectionFacade.getApiCollectionEndpoint(7L);

            assertThat(apiCollectionEndpointDTO.name()).isEqualTo("list-invoices");
            assertThat(apiCollectionEndpointDTO.workflowUuid()).isEqualTo("uuid-1");
        }

        @Test
        void testGetApiCollectionEndpointReturnsConnectionsAndInputs() {
            stubEndpointWorkflow(false, 4);

            ApiCollectionEndpointDTO apiCollectionEndpointDTO = apiCollectionFacade.getApiCollectionEndpoint(7L);

            assertThat(apiCollectionEndpointDTO.connections()).hasSize(1);
            assertThat(apiCollectionEndpointDTO.connections()
                .get(0)
                .getConnectionId()).isEqualTo(61L);
            assertThat(apiCollectionEndpointDTO.inputs()
                .get("region")).isEqualTo("eu");
        }

        @Test
        void testUpdateApiCollectionEndpointWorkflowKeepsEnabledAndVersion() {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = stubEndpointWorkflow(true, 4);

            ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection =
                new ProjectDeploymentWorkflowConnection(62L, "key", "node");

            apiCollectionFacade.updateApiCollectionEndpointWorkflow(
                7L, List.of(projectDeploymentWorkflowConnection), Map.of("region", "us"));

            ArgumentCaptor<ProjectDeploymentWorkflow> captor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentFacade).updateProjectDeploymentWorkflow(captor.capture());

            ProjectDeploymentWorkflow updatedProjectDeploymentWorkflow = captor.getValue();

            assertThat(updatedProjectDeploymentWorkflow.getId()).isEqualTo(901L);
            assertThat(updatedProjectDeploymentWorkflow.getConnections()).containsExactly(
                projectDeploymentWorkflowConnection);
            assertThat(updatedProjectDeploymentWorkflow.getInputs()
                .get("region")).isEqualTo("us");
            assertThat(updatedProjectDeploymentWorkflow.isEnabled()).isEqualTo(projectDeploymentWorkflow.isEnabled());
            assertThat(updatedProjectDeploymentWorkflow.isEnabled()).isTrue();
            assertThat(updatedProjectDeploymentWorkflow.getVersion()).isEqualTo(4);
        }

        private ProjectDeploymentWorkflow stubEndpointWorkflow(boolean enabled, int version) {
            ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

            apiCollectionEndpoint.setApiCollectionId(100L);
            apiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
            apiCollectionEndpoint.setId(7L);
            apiCollectionEndpoint.setName("list-invoices");
            apiCollectionEndpoint.setPath("invoices");
            apiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow
                .setConnections(List.of(new ProjectDeploymentWorkflowConnection(61L, "key", "node")));
            projectDeploymentWorkflow.setEnabled(enabled);
            projectDeploymentWorkflow.setId(901L);
            projectDeploymentWorkflow.setInputs(Map.of("region", "eu"));
            projectDeploymentWorkflow.setProjectDeploymentId(200L);
            projectDeploymentWorkflow.setVersion(version);
            projectDeploymentWorkflow.setWorkflowId("workflow-1");

            when(apiCollectionEndpointService.getOpenApiEndpoint(7L)).thenReturn(apiCollectionEndpoint);
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L))
                .thenReturn(projectDeploymentWorkflow);
            lenient().when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-1"))
                .thenReturn("uuid-1");

            return projectDeploymentWorkflow;
        }

        @Test
        void testGetApiCollectionEndpointsOrdersByPathThenMethod() {
            ApiCollectionEndpoint postInvoices = createEndpoint(1L, "invoices", HttpMethod.POST, 901L);
            ApiCollectionEndpoint getOrders = createEndpoint(2L, "orders", HttpMethod.GET, 902L);
            ApiCollectionEndpoint getInvoices = createEndpoint(3L, "invoices", HttpMethod.GET, 903L);

            when(apiCollectionEndpointService.getApiEndpoints(100L))
                .thenReturn(List.of(postInvoices, getOrders, getInvoices));

            for (long projectDeploymentWorkflowId : List.of(901L, 902L, 903L)) {
                ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

                projectDeploymentWorkflow.setProjectDeploymentId(200L);
                projectDeploymentWorkflow.setWorkflowId("workflow-" + projectDeploymentWorkflowId);

                when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentWorkflowId))
                    .thenReturn(projectDeploymentWorkflow);
                when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-" + projectDeploymentWorkflowId))
                    .thenReturn("uuid-" + projectDeploymentWorkflowId);
            }

            List<ApiCollectionEndpointDTO> apiCollectionEndpointDTOs =
                apiCollectionFacade.getApiCollectionEndpoints(100L);

            assertThat(apiCollectionEndpointDTOs).extracting(ApiCollectionEndpointDTO::id)
                .containsExactly(3L, 1L, 2L);
            assertThat(apiCollectionEndpointDTOs).extracting(ApiCollectionEndpointDTO::workflowUuid)
                .containsExactly("uuid-903", "uuid-901", "uuid-902");
        }

    }

    @Nested
    class EndpointUpdate {

        private final ApiCollectionEndpointService apiCollectionEndpointService =
            mock(ApiCollectionEndpointService.class);
        private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
        private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
            mock(ProjectDeploymentWorkflowService.class);
        private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        private final ApiCollectionFacade apiCollectionFacade = new ApiCollectionFacadeImpl(
            apiCollectionService, apiCollectionEndpointService, mock(ComponentConnectionFacade.class),
            mock(EnvironmentService.class), projectDeploymentFacade, mock(ProjectDeploymentService.class),
            projectDeploymentWorkflowService, mock(ProjectService.class), projectWorkflowService,
            mock(TagService.class), mock(WorkflowService.class));

        /**
         * The pointer is what ties an endpoint to the workflow it serves, and every {@code api_collection_endpoint} row
         * FKs to it. Because {@link ApiCollectionEndpointDTO#toApiCollectionEndpoint()} does not map it, an update
         * whose DTO carries no pointer must leave the persisted one alone rather than blanking it.
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
         * A uuid naming no workflow of this collection is bad caller input, not a server fault: before the endpoint
         * update learned to re-resolve the pointer it was silently ignored, and the naive re-resolution reported it as
         * an untyped {@code IllegalArgumentException} — a 500.
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

        @Test
        void testUpdateEndpointWithConnectionsAndInputsAppliesBothInOneCall() {
            stubEndpointUpdate(901L);

            ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection =
                new ProjectDeploymentWorkflowConnection(62L, "key", "node");

            apiCollectionFacade.updateApiCollectionEndpoint(
                endpointDTO("wf-uuid"), List.of(projectDeploymentWorkflowConnection), Map.of("region", "us"));

            assertThat(capturedUpdatedEndpoint().getProjectDeploymentWorkflowId()).isEqualTo(901L);

            ArgumentCaptor<ProjectDeploymentWorkflow> captor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentFacade).updateProjectDeploymentWorkflow(captor.capture());

            assertThat(captor.getValue()
                .getConnections()).containsExactly(projectDeploymentWorkflowConnection);
            assertThat(captor.getValue()
                .getInputs()
                .get("region")).isEqualTo("us");
        }

        @Test
        void testUpdateEndpointWithOnlyInputsKeepsTheCurrentConnections() {
            stubEndpointUpdate(901L);

            ProjectDeploymentWorkflowConnection currentConnection =
                new ProjectDeploymentWorkflowConnection(61L, "key", "node");

            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L);

            projectDeploymentWorkflow.setConnections(List.of(currentConnection));
            projectDeploymentWorkflow.setInputs(Map.of("region", "eu"));

            apiCollectionFacade.updateApiCollectionEndpoint(endpointDTO("wf-uuid"), null, Map.of("region", "us"));

            ArgumentCaptor<ProjectDeploymentWorkflow> captor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentFacade).updateProjectDeploymentWorkflow(captor.capture());

            assertThat(captor.getValue()
                .getConnections()).containsExactly(currentConnection);
            assertThat(captor.getValue()
                .getInputs()
                .get("region")).isEqualTo("us");
        }

        @Test
        void testUpdateEndpointWithOnlyConnectionsKeepsTheCurrentInputs() {
            stubEndpointUpdate(901L);

            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L);

            projectDeploymentWorkflow.setInputs(Map.of("region", "eu"));

            ProjectDeploymentWorkflowConnection newConnection =
                new ProjectDeploymentWorkflowConnection(62L, "key", "node");

            apiCollectionFacade.updateApiCollectionEndpoint(endpointDTO("wf-uuid"), List.of(newConnection), null);

            ArgumentCaptor<ProjectDeploymentWorkflow> captor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentFacade).updateProjectDeploymentWorkflow(captor.capture());

            assertThat(captor.getValue()
                .getConnections()).containsExactly(newConnection);
            assertThat(captor.getValue()
                .getInputs()
                .get("region")).isEqualTo("eu");
        }

        @Test
        void testUpdateEndpointWithoutConnectionsOrInputsLeavesTheWorkflowAlone() {
            stubEndpointUpdate(901L);

            apiCollectionFacade.updateApiCollectionEndpoint(endpointDTO("wf-uuid"), null, null);

            verify(projectDeploymentFacade, never()).updateProjectDeploymentWorkflow(any());
        }

        @Test
        void testCreateEndpointWithConnectionsAppliesThem() {
            ApiCollection apiCollection = new ApiCollection();

            apiCollection.setId(100L);
            apiCollection.setProjectDeploymentId(200L);

            when(apiCollectionService.getApiCollection(100L)).thenReturn(apiCollection);
            when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, "uuid-1"))
                .thenReturn(Optional.of("workflow-1"));

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setId(901L);
            projectDeploymentWorkflow.setProjectDeploymentId(200L);
            projectDeploymentWorkflow.setWorkflowId("workflow-1");

            when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(200L, "workflow-1"))
                .thenReturn(Optional.of(projectDeploymentWorkflow));
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L))
                .thenReturn(projectDeploymentWorkflow);
            when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-1")).thenReturn("uuid-1");

            ApiCollectionEndpoint createdApiCollectionEndpoint = createEndpoint(7L, "orders", HttpMethod.GET, 901L);

            when(apiCollectionEndpointService.create(any(ApiCollectionEndpoint.class)))
                .thenReturn(createdApiCollectionEndpoint);
            when(apiCollectionEndpointService.getOpenApiEndpoint(7L)).thenReturn(createdApiCollectionEndpoint);

            ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection =
                new ProjectDeploymentWorkflowConnection(62L, "key", "node");

            ApiCollectionEndpointDTO apiCollectionEndpointDTO = apiCollectionFacade.createApiCollectionEndpoint(
                new ApiCollectionEndpointDTO(
                    100L, null, null, false, HttpMethod.GET, null, null, null, "orders", "orders", 0L, 0, "uuid-1"),
                List.of(projectDeploymentWorkflowConnection), Map.of("region", "us"));

            ArgumentCaptor<ProjectDeploymentWorkflow> captor = ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentFacade).updateProjectDeploymentWorkflow(captor.capture());

            ProjectDeploymentWorkflow updatedProjectDeploymentWorkflow = captor.getValue();

            assertThat(updatedProjectDeploymentWorkflow.getId()).isEqualTo(901L);
            assertThat(updatedProjectDeploymentWorkflow.getConnections()).containsExactly(
                projectDeploymentWorkflowConnection);
            assertThat(updatedProjectDeploymentWorkflow.getInputs()
                .get("region")).isEqualTo("us");
            assertThat(apiCollectionEndpointDTO.id()).isEqualTo(7L);
            assertThat(apiCollectionEndpointDTO.workflowUuid()).isEqualTo("uuid-1");
        }

        @Test
        void testCreateEndpointWithoutConnectionsOrInputsLeavesTheWorkflowAlone() {
            ApiCollection apiCollection = new ApiCollection();

            apiCollection.setId(100L);
            apiCollection.setProjectDeploymentId(200L);

            when(apiCollectionService.getApiCollection(100L)).thenReturn(apiCollection);
            when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, "uuid-1"))
                .thenReturn(Optional.of("workflow-1"));

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setId(901L);

            when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(200L, "workflow-1"))
                .thenReturn(Optional.of(projectDeploymentWorkflow));
            when(apiCollectionEndpointService.create(any(ApiCollectionEndpoint.class)))
                .thenReturn(createEndpoint(7L, "orders", HttpMethod.GET, 901L));

            apiCollectionFacade.createApiCollectionEndpoint(
                new ApiCollectionEndpointDTO(
                    100L, null, null, false, HttpMethod.GET, null, null, null, "orders", "orders", 0L, 0, "uuid-1"),
                null, null);

            verify(projectDeploymentFacade, never()).updateProjectDeploymentWorkflow(any());
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
                100L, null, null, true, HttpMethod.GET, 501L, null, null, "listOrders", "orders", 901L, 0,
                workflowUuid);
        }
    }

    @Nested
    class EndpointRepoint {

        private final ApiEndpointRepository apiEndpointRepository = mock(ApiEndpointRepository.class);
        private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
        private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
            mock(ProjectDeploymentWorkflowService.class);
        private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        private final ApiCollectionFacade apiCollectionFacade = new ApiCollectionFacadeImpl(
            apiCollectionService, new ApiCollectionEndpointServiceImpl(apiEndpointRepository),
            mock(ComponentConnectionFacade.class), mock(EnvironmentService.class), projectDeploymentFacade,
            mock(ProjectDeploymentService.class), projectDeploymentWorkflowService, mock(ProjectService.class),
            projectWorkflowService, mock(TagService.class), mock(WorkflowService.class));

        @Test
        void testUpdateApiCollectionEndpointRepointsThePersistedEndpointAndAppliesConnectionsToTheNewWorkflow() {
            ApiCollectionEndpoint persistedApiCollectionEndpoint = new ApiCollectionEndpoint();

            persistedApiCollectionEndpoint.setId(501L);
            persistedApiCollectionEndpoint.setApiCollectionId(100L);
            persistedApiCollectionEndpoint.setHttpMethod(HttpMethod.GET);
            persistedApiCollectionEndpoint.setName("getOrders");
            persistedApiCollectionEndpoint.setPath("orders");
            persistedApiCollectionEndpoint.setProjectDeploymentWorkflowId(901L);

            when(apiEndpointRepository.findById(501L)).thenReturn(Optional.of(persistedApiCollectionEndpoint));
            when(apiEndpointRepository.save(any(ApiCollectionEndpoint.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            ApiCollection apiCollection = new ApiCollection();

            apiCollection.setId(100L);
            apiCollection.setProjectDeploymentId(200L);

            when(apiCollectionService.getApiCollection(100L)).thenReturn(apiCollection);
            when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, "other-wf-uuid"))
                .thenReturn(Optional.of("workflow-2"));

            ProjectDeploymentWorkflow oldProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            oldProjectDeploymentWorkflow.setId(901L);
            oldProjectDeploymentWorkflow.setProjectDeploymentId(200L);
            oldProjectDeploymentWorkflow.setWorkflowId("workflow-1");

            ProjectDeploymentWorkflow newProjectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            newProjectDeploymentWorkflow.setId(902L);
            newProjectDeploymentWorkflow.setProjectDeploymentId(200L);
            newProjectDeploymentWorkflow.setWorkflowId("workflow-2");

            when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(200L, "workflow-2"))
                .thenReturn(Optional.of(newProjectDeploymentWorkflow));
            lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L))
                .thenReturn(oldProjectDeploymentWorkflow);
            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(902L))
                .thenReturn(newProjectDeploymentWorkflow);
            when(projectWorkflowService.getProjectWorkflowUuid(200L, "workflow-2")).thenReturn("other-wf-uuid");

            ProjectDeploymentWorkflowConnection projectDeploymentWorkflowConnection =
                new ProjectDeploymentWorkflowConnection(62L, "key", "node");

            apiCollectionFacade.updateApiCollectionEndpoint(
                new ApiCollectionEndpointDTO(
                    100L, null, null, true, HttpMethod.GET, 501L, null, null, "listOrders", "orders", 901L, 0,
                    "other-wf-uuid"),
                List.of(projectDeploymentWorkflowConnection), Map.of("region", "us"));

            ArgumentCaptor<ApiCollectionEndpoint> apiCollectionEndpointCaptor =
                ArgumentCaptor.forClass(ApiCollectionEndpoint.class);

            verify(apiEndpointRepository).save(apiCollectionEndpointCaptor.capture());

            assertThat(apiCollectionEndpointCaptor.getValue()
                .getProjectDeploymentWorkflowId()).isEqualTo(902L);

            ArgumentCaptor<ProjectDeploymentWorkflow> projectDeploymentWorkflowCaptor =
                ArgumentCaptor.forClass(ProjectDeploymentWorkflow.class);

            verify(projectDeploymentFacade).updateProjectDeploymentWorkflow(projectDeploymentWorkflowCaptor.capture());

            ProjectDeploymentWorkflow updatedProjectDeploymentWorkflow = projectDeploymentWorkflowCaptor.getValue();

            assertThat(updatedProjectDeploymentWorkflow.getId()).isEqualTo(902L);
            assertThat(updatedProjectDeploymentWorkflow.getConnections()).containsExactly(
                projectDeploymentWorkflowConnection);
            assertThat(oldProjectDeploymentWorkflow.getConnections()).isEmpty();
        }
    }

    @Nested
    class UpdateApiCollectionProjectVersion {

        private final ApiCollectionEndpointService apiCollectionEndpointService =
            mock(ApiCollectionEndpointService.class);
        private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
        private final ComponentConnectionFacade componentConnectionFacade = mock(ComponentConnectionFacade.class);
        private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
            mock(ProjectDeploymentWorkflowService.class);
        private final ProjectService projectService = mock(ProjectService.class);
        private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        private final WorkflowService workflowService = mock(WorkflowService.class);

        private final ApiCollectionFacade apiCollectionFacade = new ApiCollectionFacadeImpl(
            apiCollectionService, apiCollectionEndpointService, componentConnectionFacade,
            mock(EnvironmentService.class), projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectService, projectWorkflowService, mock(TagService.class),
            workflowService);

        private static final UUID WORKFLOW_A_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        private static final UUID WORKFLOW_B_UUID = UUID.fromString("00000000-0000-0000-0000-00000000000b");

        private final ProjectDeployment projectDeployment = new ProjectDeployment();
        private final ProjectDeploymentWorkflowConnection workflowAConnection =
            new ProjectDeploymentWorkflowConnection(61L, "slack", "postMessage");

        private ApiCollectionEndpoint listOrdersEndpoint;
        private ApiCollectionEndpoint createOrderEndpoint;

        @BeforeEach
        void beforeEach() {
            ApiCollection apiCollection = new ApiCollection();

            apiCollection.setCollectionVersion(1);
            apiCollection.setContextPath("orders");
            apiCollection.setId(100L);
            apiCollection.setName("orders");
            apiCollection.setProjectDeploymentId(200L);

            lenient().when(apiCollectionService.getApiCollection(100L))
                .thenReturn(apiCollection);

            projectDeployment.setEnvironment(Environment.STAGING);
            projectDeployment.setId(200L);
            projectDeployment.setProjectId(42L);
            projectDeployment.setProjectVersion(2);

            lenient().when(projectDeploymentService.getProjectDeployment(200L))
                .thenReturn(projectDeployment);

            Project project = new Project();

            project.setId(42L);
            project.setProjectVersions(
                List.of(
                    publishedProjectVersion(1), publishedProjectVersion(2), publishedProjectVersion(3),
                    new ProjectVersion(4)));
            project.setWorkspaceId(5L);

            lenient().when(projectService.getProject(42L))
                .thenReturn(project);

            lenient().when(projectWorkflowService.getProjectWorkflows(42L, 2))
                .thenReturn(
                    List.of(
                        new ProjectWorkflow(42L, 2, "wf-a-2", WORKFLOW_A_UUID),
                        new ProjectWorkflow(42L, 2, "wf-b-2", WORKFLOW_B_UUID)));

            ProjectDeploymentWorkflow workflowARow = createProjectDeploymentWorkflow(
                901L, "wf-a-2", true, List.of(workflowAConnection), Map.of("region", "eu"));
            ProjectDeploymentWorkflow workflowBRow = createProjectDeploymentWorkflow(
                902L, "wf-b-2", false, List.of(), Map.of("limit", 10));

            lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(200L))
                .thenReturn(List.of(workflowARow, workflowBRow));

            listOrdersEndpoint = createEndpoint(501L, "orders", HttpMethod.GET, 901L);

            listOrdersEndpoint.setName("listOrders");

            createOrderEndpoint = createEndpoint(502L, "orders", HttpMethod.POST, 902L);

            createOrderEndpoint.setName("createOrder");

            lenient().when(apiCollectionEndpointService.getApiEndpoints(100L))
                .thenReturn(List.of(listOrdersEndpoint, createOrderEndpoint));
            lenient().when(apiCollectionEndpointService.getOpenApiEndpoint(501L))
                .thenReturn(listOrdersEndpoint);
            lenient().when(apiCollectionEndpointService.getOpenApiEndpoint(502L))
                .thenReturn(createOrderEndpoint);
            lenient().when(apiCollectionEndpointService.update(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

            lenient().when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, WORKFLOW_A_UUID.toString()))
                .thenReturn(Optional.of("wf-a-3"));
            lenient().when(projectWorkflowService.fetchProjectWorkflowWorkflowId(200L, WORKFLOW_B_UUID.toString()))
                .thenReturn(Optional.of("wf-b-3"));
            ProjectDeploymentWorkflow workflowAVersionThreeRow = createProjectDeploymentWorkflow(
                901L, "wf-a-3", true, List.of(), Map.of());
            ProjectDeploymentWorkflow workflowBVersionThreeRow = createProjectDeploymentWorkflow(
                902L, "wf-b-3", false, List.of(), Map.of());

            lenient().when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(200L, "wf-a-3"))
                .thenReturn(Optional.of(workflowAVersionThreeRow));
            lenient().when(projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(200L, "wf-b-3"))
                .thenReturn(Optional.of(workflowBVersionThreeRow));
            lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(901L))
                .thenReturn(workflowAVersionThreeRow);
            lenient().when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(902L))
                .thenReturn(workflowBVersionThreeRow);

            lenient().when(componentConnectionFacade.getWorkflowComponentConnections("wf-a-2"))
                .thenReturn(List.of(slackSlot("slack")));
            lenient().when(componentConnectionFacade.getWorkflowComponentConnections("wf-a-3"))
                .thenReturn(List.of(slackSlot("slack")));

            Workflow workflowWithoutInputs = mock(Workflow.class);

            lenient().when(workflowWithoutInputs.getInputs())
                .thenReturn(List.of());
            lenient().when(workflowService.getWorkflow(any()))
                .thenReturn(workflowWithoutInputs);
            lenient().when(projectWorkflowService.getProjectWorkflowUuid(eq(200L), any()))
                .thenReturn(WORKFLOW_A_UUID.toString());
        }

        @Test
        void testUpdateProjectVersionRepointsEndpointsByWorkflowUuid() {
            stubVersionThreeWorkflows(true);

            ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
                apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, false);

            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = capturedProjectDeploymentWorkflows();

            assertThat(projectDeployment.getProjectVersion()).isEqualTo(3);
            assertThat(projectDeploymentWorkflows).extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .containsExactly("wf-a-3", "wf-b-3");

            ProjectDeploymentWorkflow workflowARow = projectDeploymentWorkflows.get(0);

            assertThat(workflowARow.getConnections()).containsExactly(workflowAConnection);
            assertThat(workflowARow.getInputs()).isEqualTo(Map.of("region", "eu"));
            assertThat(workflowARow.isEnabled()).isTrue();
            assertThat(workflowARow.getProjectDeploymentId()).isEqualTo(200L);

            ProjectDeploymentWorkflow workflowBRow = projectDeploymentWorkflows.get(1);

            assertThat(workflowBRow.getConnections()).isEmpty();
            assertThat(workflowBRow.getInputs()).isEqualTo(Map.of("limit", 10));
            assertThat(workflowBRow.isEnabled()).isFalse();

            ArgumentCaptor<ApiCollectionEndpoint> apiCollectionEndpointCaptor =
                ArgumentCaptor.forClass(ApiCollectionEndpoint.class);

            verify(apiCollectionEndpointService, times(2))
                .update(apiCollectionEndpointCaptor.capture());

            assertThat(apiCollectionEndpointCaptor.getAllValues())
                .extracting(ApiCollectionEndpoint::getId, ApiCollectionEndpoint::getProjectDeploymentWorkflowId)
                .containsExactly(
                    tuple(501L, 901L), tuple(502L, 902L));

            verify(apiCollectionService, never()).update(any(ApiCollection.class));

            assertThat(apiCollectionVersionUpdateDTO.apiCollection()
                .collectionVersion()).isEqualTo(1);
            assertThat(apiCollectionVersionUpdateDTO.removedEndpointNames()).isEmpty();
            assertThat(apiCollectionVersionUpdateDTO.missingConnections()).isEmpty();
            assertThat(apiCollectionVersionUpdateDTO.disabledEndpointNames()).isEmpty();
        }

        @Test
        void testUpdateProjectVersionRefusesWhenWorkflowRemoved() {
            stubVersionThreeWorkflows(false);

            assertThatThrownBy(() -> apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, false))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("createOrder")
                .satisfies(exception -> {
                    ConfigurationException configurationException = (ConfigurationException) exception;

                    assertThat(configurationException.getErrorKey())
                        .isEqualTo(ApiCollectionErrorType.ENDPOINT_WORKFLOW_REMOVED.getErrorKey());
                    Map<String, ?> inputParameters = configurationException.getInputParameters();

                    assertThat(inputParameters.get("endpointNames")).isEqualTo(List.of("createOrder"));
                });

            verify(projectDeploymentFacade, never()).updateProjectDeployment(any(), any(), any());
            verify(apiCollectionEndpointService, never()).delete(any(Long.class));
            verify(apiCollectionEndpointService, never()).update(any());
        }

        @Test
        void testUpdateProjectVersionRemovesEndpointWhenConfirmed() {
            stubVersionThreeWorkflows(false);

            ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
                apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, true);

            InOrder deleteBeforeMove = inOrder(apiCollectionEndpointService, projectDeploymentFacade);

            deleteBeforeMove.verify(apiCollectionEndpointService)
                .delete(502L);
            deleteBeforeMove.verify(projectDeploymentFacade)
                .updateProjectDeployment(any(), any(), any());

            verify(apiCollectionEndpointService, never()).delete(501L);

            assertThat(capturedProjectDeploymentWorkflows()).extracting(ProjectDeploymentWorkflow::getWorkflowId)
                .containsExactly("wf-a-3");
            assertThat(apiCollectionVersionUpdateDTO.removedEndpointNames()).containsExactly("createOrder");
        }

        @Test
        void testUpdateProjectVersionRefusesSameOrOlderVersion() {
            for (int projectVersion : List.of(1, 2)) {
                assertThatThrownBy(
                    () -> apiCollectionFacade.updateApiCollectionProjectVersion(100L, projectVersion, false))
                        .isInstanceOf(ConfigurationException.class)
                        .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ApiCollectionErrorType.PROJECT_VERSION_INVALID.getErrorKey());
            }

            verify(projectDeploymentFacade, never()).updateProjectDeployment(any(), any(), any());
            verify(apiCollectionEndpointService, never()).delete(any(Long.class));
            verify(apiCollectionEndpointService, never()).update(any());
        }

        @Test
        void testUpdateProjectVersionRefusesUnpublishedVersion() {
            for (int projectVersion : List.of(4, 9)) {
                assertThatThrownBy(
                    () -> apiCollectionFacade.updateApiCollectionProjectVersion(100L, projectVersion, false))
                        .isInstanceOf(ConfigurationException.class)
                        .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(ApiCollectionErrorType.PROJECT_VERSION_INVALID.getErrorKey());
            }

            verify(projectDeploymentFacade, never()).updateProjectDeployment(any(), any(), any());
            verify(apiCollectionEndpointService, never()).delete(any(Long.class));
            verify(apiCollectionEndpointService, never()).update(any());
        }

        @Test
        void testUpdateProjectVersionReportsMissingConnections() {
            stubVersionThreeWorkflows(true);

            when(componentConnectionFacade.getWorkflowComponentConnections("wf-a-3"))
                .thenReturn(
                    List.of(
                        slackSlot("slack"), new ComponentConnection("gmail", 1, "sendEmail", "gmail", true),
                        new ComponentConnection("jira", 1, "createIssue", "jira", false)));

            ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
                apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, false);

            assertThat(apiCollectionVersionUpdateDTO.missingConnections())
                .containsExactly(new MissingConnection("listOrders", "sendEmail", "gmail", "gmail"));
            assertThat(apiCollectionVersionUpdateDTO.disabledEndpointNames()).containsExactly("listOrders");
            assertThat(capturedProjectDeploymentWorkflows().get(0)
                .isEnabled()).isFalse();
        }

        @Test
        void testUpdateProjectVersionDropsConnectionWhoseSlotWasRemoved() {
            stubVersionThreeWorkflows(true);

            when(componentConnectionFacade.getWorkflowComponentConnections("wf-a-3")).thenReturn(List.of());

            ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
                apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, false);

            ProjectDeploymentWorkflow workflowARow = capturedProjectDeploymentWorkflows().get(0);

            assertThat(workflowARow.getConnections()).isEmpty();
            assertThat(workflowARow.isEnabled()).isTrue();
            assertThat(apiCollectionVersionUpdateDTO.missingConnections()).isEmpty();
            assertThat(apiCollectionVersionUpdateDTO.disabledEndpointNames()).isEmpty();
        }

        @Test
        void testUpdateProjectVersionDropsConnectionWhoseSlotChangedComponent() {
            stubVersionThreeWorkflows(true);

            when(componentConnectionFacade.getWorkflowComponentConnections("wf-a-3"))
                .thenReturn(List.of(slackSlot("discord")));

            ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
                apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, false);

            ProjectDeploymentWorkflow workflowARow = capturedProjectDeploymentWorkflows().get(0);

            assertThat(workflowARow.getConnections()).isEmpty();
            assertThat(workflowARow.isEnabled()).isFalse();
            assertThat(apiCollectionVersionUpdateDTO.missingConnections())
                .containsExactly(new MissingConnection("listOrders", "postMessage", "slack", "discord"));
            assertThat(apiCollectionVersionUpdateDTO.disabledEndpointNames()).containsExactly("listOrders");
        }

        @Test
        void testUpdateProjectVersionDisablesWorkflowMissingARequiredInput() {
            stubVersionThreeWorkflows(true);

            Workflow workflowWithRequiredInput = mock(Workflow.class);

            when(workflowWithRequiredInput.getInputs())
                .thenReturn(
                    List.of(
                        new Workflow.Input("region", "Region", "string", true, Map.of()),
                        new Workflow.Input("tenant", "Tenant", "string", true, Map.of()),
                        new Workflow.Input("locale", "Locale", "string", false, Map.of())));
            when(workflowService.getWorkflow("wf-a-3")).thenReturn(workflowWithRequiredInput);

            ApiCollectionVersionUpdateDTO apiCollectionVersionUpdateDTO =
                apiCollectionFacade.updateApiCollectionProjectVersion(100L, 3, false);

            ProjectDeploymentWorkflow workflowARow = capturedProjectDeploymentWorkflows().get(0);

            assertThat(workflowARow.isEnabled()).isFalse();
            assertThat(workflowARow.getInputs()).isEqualTo(Map.of("region", "eu"));
            assertThat(apiCollectionVersionUpdateDTO.missingInputs())
                .containsExactly(new MissingInput("listOrders", "tenant"));
            assertThat(apiCollectionVersionUpdateDTO.disabledEndpointNames()).containsExactly("listOrders");
        }

        private List<ProjectDeploymentWorkflow> capturedProjectDeploymentWorkflows() {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ProjectDeploymentWorkflow>> projectDeploymentWorkflowsCaptor =
                ArgumentCaptor.forClass(List.class);

            verify(projectDeploymentFacade).updateProjectDeployment(
                eq(projectDeployment), projectDeploymentWorkflowsCaptor.capture(), eq(List.of()));

            return projectDeploymentWorkflowsCaptor.getValue();
        }

        private void stubVersionThreeWorkflows(boolean includeWorkflowB) {
            List<ProjectWorkflow> projectWorkflows = includeWorkflowB
                ? List.of(
                    new ProjectWorkflow(42L, 3, "wf-a-3", WORKFLOW_A_UUID),
                    new ProjectWorkflow(42L, 3, "wf-b-3", WORKFLOW_B_UUID))
                : List.of(new ProjectWorkflow(42L, 3, "wf-a-3", WORKFLOW_A_UUID));

            when(projectWorkflowService.getProjectWorkflows(42L, 3)).thenReturn(projectWorkflows);
        }

        private static ProjectDeploymentWorkflow createProjectDeploymentWorkflow(
            long id, String workflowId, boolean enabled, List<ProjectDeploymentWorkflowConnection> connections,
            Map<String, ?> inputs) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setConnections(connections);
            projectDeploymentWorkflow.setEnabled(enabled);
            projectDeploymentWorkflow.setId(id);
            projectDeploymentWorkflow.setInputs(inputs);
            projectDeploymentWorkflow.setProjectDeploymentId(200L);
            projectDeploymentWorkflow.setWorkflowId(workflowId);

            return projectDeploymentWorkflow;
        }

        private static ComponentConnection slackSlot(String componentName) {
            return new ComponentConnection(componentName, 1, "postMessage", "slack", true);
        }

        private static ProjectVersion publishedProjectVersion(int version) {
            return new ProjectVersion(version, ProjectVersion.Status.PUBLISHED.ordinal(), null, null);
        }
    }

    private void assertResourceGuard(
        ThrowingCallable callable, long resourceId, String resourceType, String expectedScope, boolean granted) {

        when(permissionService.hasResourceScope(resourceId, resourceType, expectedScope)).thenReturn(granted);

        assertGuard(callable, granted);

        verify(permissionService).hasResourceScope(resourceId, resourceType, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertCreateApiCollectionEnvironmentGuard(boolean granted) {
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasResourceScopeInEnvironment(PROJECT_ID, "Project", "API_PLATFORM_CREATE", environment))
            .thenReturn(granted);

        assertGuard(() -> apiCollectionFacade.createApiCollection(apiCollectionDTO(environment)), granted);

        verify(permissionService).hasResourceScopeInEnvironment(
            PROJECT_ID, "Project", "API_PLATFORM_CREATE", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertGetApiCollectionsGuard(boolean granted) {
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_PLATFORM_VIEW", environment)).thenReturn(granted);

        assertGuard(
            () -> apiCollectionFacade.getApiCollections(WORKSPACE_ID, ENVIRONMENT_ID, PROJECT_ID, TAG_ID), granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_PLATFORM_VIEW", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertGuard(ThrowingCallable callable, boolean granted) {
        if (granted) {
            assertThatThrownBy(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(callable).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(
                apiCollectionEndpointService, apiCollectionService, environmentService, projectDeploymentService);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T secure(T target) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(
                permissionService,
                new StaticListableBeanFactory().getBeanProvider(MethodSecurityExpressionContributor.class));

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (T) proxyFactory.getProxy();
    }

    private static ApiCollectionDTO apiCollectionDTO(Environment environment) {
        return new ApiCollectionDTO(
            1, "context", null, null, null, false, List.of(), environment, API_COLLECTION_ID, null, null, "name", null,
            PROJECT_ID, null, PROJECT_DEPLOYMENT_ID, 1, List.of(), 0);
    }

    private static ApiCollectionEndpointDTO apiCollectionEndpointDTO() {
        return new ApiCollectionEndpointDTO(
            API_COLLECTION_ID, null, null, false, HttpMethod.GET, API_COLLECTION_ENDPOINT_ID, null, null, "name",
            "path",
            PROJECT_DEPLOYMENT_WORKFLOW_ID, 0, "workflowUuid");
    }

    private static ApiCollectionEndpoint createEndpoint(
        long id, String path, HttpMethod httpMethod, long projectDeploymentWorkflowId) {

        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(100L);
        apiCollectionEndpoint.setHttpMethod(httpMethod);
        apiCollectionEndpoint.setId(id);
        apiCollectionEndpoint.setName(path);
        apiCollectionEndpoint.setPath(path);
        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflowId);

        return apiCollectionEndpoint;
    }
}
