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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
                apiCollectionService, apiCollectionEndpointService, environmentService,
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
            mock(ApiCollectionService.class), apiCollectionEndpointService, mock(EnvironmentService.class),
            projectDeploymentFacade, mock(ProjectDeploymentService.class), projectDeploymentWorkflowService,
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

            ApiCollectionEndpointDTO apiCollectionEndpointDTO = apiCollectionFacade.enableApiCollectionEndpoint(7L, true);

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

            projectDeploymentWorkflow.setConnections(List.of(new ProjectDeploymentWorkflowConnection(61L, "key", "node")));
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

            List<ApiCollectionEndpointDTO> apiCollectionEndpointDTOs = apiCollectionFacade.getApiCollectionEndpoints(100L);

            assertThat(apiCollectionEndpointDTOs).extracting(ApiCollectionEndpointDTO::id)
                .containsExactly(3L, 1L, 2L);
            assertThat(apiCollectionEndpointDTOs).extracting(ApiCollectionEndpointDTO::workflowUuid)
                .containsExactly("uuid-903", "uuid-901", "uuid-902");
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
}
