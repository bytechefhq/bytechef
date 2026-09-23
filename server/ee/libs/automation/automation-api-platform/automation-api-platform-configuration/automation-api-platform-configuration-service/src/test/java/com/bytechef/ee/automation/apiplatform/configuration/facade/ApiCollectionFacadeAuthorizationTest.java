/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link ApiCollectionFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionFacadeAuthorizationTest {

    private static final long API_COLLECTION_ENDPOINT_ID = 19L;
    private static final String API_COLLECTION_ENDPOINT_TYPE = "ApiCollectionEndpoint";
    private static final long API_COLLECTION_ID = 7L;
    private static final String API_COLLECTION_TYPE = "ApiCollection";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 13L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 23L;
    private static final long PROJECT_ID = 11L;
    private static final long TAG_ID = 29L;
    private static final long WORKSPACE_ID = 42L;
    private static final String WORKSPACE_TYPE = "Workspace";

    @Test
    void testCreateApiCollectionDeniesWhenTheApiPlatformCreateScopeIsRefusedOnTheProjectInItsEnvironment()
        throws Exception {

        assertCreateApiCollectionEnvironmentGuard(false);
    }

    @Test
    void testCreateApiCollectionAllowsWhenTheApiPlatformCreateScopeIsGrantedOnTheProjectInItsEnvironment()
        throws Exception {

        assertCreateApiCollectionEnvironmentGuard(true);
    }

    @Test
    void testCreateApiCollectionDeniesWhenTheApiPlatformCreateScopeIsRefusedOnTheProjectWithoutAnEnvironment()
        throws Exception {

        assertResourceGuard(createApiCollectionMethod(), new Object[] {
            apiCollectionDTO(null)
        }, PROJECT_ID, "Project", "API_PLATFORM_CREATE", false);
    }

    @Test
    void testCreateApiCollectionAllowsWhenTheApiPlatformCreateScopeIsGrantedOnTheProjectWithoutAnEnvironment()
        throws Exception {

        assertResourceGuard(createApiCollectionMethod(), new Object[] {
            apiCollectionDTO(null)
        }, PROJECT_ID, "Project", "API_PLATFORM_CREATE", true);
    }

    @Test
    void testCreateApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() throws Exception {
        assertResourceGuard(createApiCollectionEndpointMethod(), new Object[] {
            apiCollectionEndpointDTO()
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testCreateApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() throws Exception {
        assertResourceGuard(createApiCollectionEndpointMethod(), new Object[] {
            apiCollectionEndpointDTO()
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testDeleteApiCollectionDeniesWhenTheApiPlatformDeleteScopeIsRefused() throws Exception {
        assertResourceGuard(deleteApiCollectionMethod(), new Object[] {
            API_COLLECTION_ID
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_DELETE", false);
    }

    @Test
    void testDeleteApiCollectionAllowsWhenTheApiPlatformDeleteScopeIsGranted() throws Exception {
        assertResourceGuard(deleteApiCollectionMethod(), new Object[] {
            API_COLLECTION_ID
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_DELETE", true);
    }

    @Test
    void testGetApiCollectionDeniesWhenTheApiPlatformViewScopeIsRefused() throws Exception {
        assertResourceGuard(getApiCollectionMethod(), new Object[] {
            API_COLLECTION_ID
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetApiCollectionAllowsWhenTheApiPlatformViewScopeIsGranted() throws Exception {
        assertResourceGuard(getApiCollectionMethod(), new Object[] {
            API_COLLECTION_ID
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testGetApiCollectionsDeniesWhenTheApiPlatformViewScopeIsRefusedInTheNamedEnvironment() throws Exception {
        assertGetApiCollectionsGuard(false);
    }

    @Test
    void testGetApiCollectionsAllowsWhenTheApiPlatformViewScopeIsGrantedInTheNamedEnvironment() throws Exception {
        assertGetApiCollectionsGuard(true);
    }

    @Test
    void testGetApiCollectionTagsDeniesWhenTheApiPlatformViewScopeIsRefusedOnTheWorkspace() throws Exception {
        assertResourceGuard(getApiCollectionTagsMethod(), new Object[] {
            WORKSPACE_ID
        }, WORKSPACE_ID, WORKSPACE_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetApiCollectionTagsAllowsWhenTheApiPlatformViewScopeIsGrantedOnTheWorkspace() throws Exception {
        assertResourceGuard(getApiCollectionTagsMethod(), new Object[] {
            WORKSPACE_ID
        }, WORKSPACE_ID, WORKSPACE_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testGetWorkspaceProjectsDeniesWhenTheApiPlatformViewScopeIsRefusedOnTheWorkspace() throws Exception {
        assertResourceGuard(getWorkspaceProjectsMethod(), new Object[] {
            WORKSPACE_ID
        }, WORKSPACE_ID, WORKSPACE_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetWorkspaceProjectsAllowsWhenTheApiPlatformViewScopeIsGrantedOnTheWorkspace() throws Exception {
        assertResourceGuard(getWorkspaceProjectsMethod(), new Object[] {
            WORKSPACE_ID
        }, WORKSPACE_ID, WORKSPACE_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testUpdateApiCollectionDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() throws Exception {
        assertResourceGuard(updateApiCollectionMethod(), new Object[] {
            apiCollectionDTO(Environment.values()[(int) ENVIRONMENT_ID])
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() throws Exception {
        assertResourceGuard(updateApiCollectionMethod(), new Object[] {
            apiCollectionDTO(Environment.values()[(int) ENVIRONMENT_ID])
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testDeleteApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheEndpoint() throws Exception {
        assertResourceGuard(deleteApiCollectionEndpointMethod(), new Object[] {
            API_COLLECTION_ENDPOINT_ID
        }, API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testDeleteApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheEndpoint() throws Exception {
        assertResourceGuard(deleteApiCollectionEndpointMethod(), new Object[] {
            API_COLLECTION_ENDPOINT_ID
        }, API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testGetOpenApiSpecificationDeniesWhenTheApiPlatformViewScopeIsRefused() throws Exception {
        assertResourceGuard(getOpenApiSpecificationMethod(), new Object[] {
            API_COLLECTION_ID
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_VIEW", false);
    }

    @Test
    void testGetOpenApiSpecificationAllowsWhenTheApiPlatformViewScopeIsGranted() throws Exception {
        assertResourceGuard(getOpenApiSpecificationMethod(), new Object[] {
            API_COLLECTION_ID
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_VIEW", true);
    }

    @Test
    void testUpdateApiCollectionEndpointDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheStoredEndpoint()
        throws Exception {

        // Keyed on the endpoint id, not the collection id the request carries: the update writes by endpoint id.
        assertResourceGuard(updateApiCollectionEndpointMethod(), new Object[] {
            apiCollectionEndpointDTO()
        }, API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionEndpointAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheStoredEndpoint()
        throws Exception {

        assertResourceGuard(updateApiCollectionEndpointMethod(), new Object[] {
            apiCollectionEndpointDTO()
        }, API_COLLECTION_ENDPOINT_ID, API_COLLECTION_ENDPOINT_TYPE, "API_PLATFORM_EDIT", true);
    }

    @Test
    void testUpdateApiCollectionTagsDeniesWhenTheApiPlatformEditScopeIsRefusedOnTheCollection() throws Exception {
        assertResourceGuard(updateApiCollectionTagsMethod(), new Object[] {
            API_COLLECTION_ID, List.of()
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", false);
    }

    @Test
    void testUpdateApiCollectionTagsAllowsWhenTheApiPlatformEditScopeIsGrantedOnTheCollection() throws Exception {
        assertResourceGuard(updateApiCollectionTagsMethod(), new Object[] {
            API_COLLECTION_ID, List.of()
        }, API_COLLECTION_ID, API_COLLECTION_TYPE, "API_PLATFORM_EDIT", true);
    }

    private void assertResourceGuard(
        Method method, Object[] arguments, long resourceId, String resourceType, String expectedScope,
        boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(resourceId, resourceType, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", resourceId, resourceType, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(resourceId, resourceType, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertCreateApiCollectionEnvironmentGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasResourceScopeInEnvironment(PROJECT_ID, "Project", "API_PLATFORM_CREATE", environment))
            .thenReturn(granted);

        assertThat(evaluateGuard(permissionService, createApiCollectionMethod(), new Object[] {
            apiCollectionDTO(environment)
        }))
            .isEqualTo(granted);

        verify(permissionService).hasResourceScopeInEnvironment(
            PROJECT_ID, "Project", "API_PLATFORM_CREATE", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertGetApiCollectionsGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_PLATFORM_VIEW", environment)).thenReturn(granted);

        Method method = ApiCollectionFacadeImpl.class.getMethod(
            "getApiCollections", long.class, Long.class, Long.class, Long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, ENVIRONMENT_ID, PROJECT_ID, TAG_ID
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_PLATFORM_VIEW", environment);
        verifyNoMoreInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, Object[] arguments) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static ApiCollectionDTO apiCollectionDTO(Environment environment) {
        return new ApiCollectionDTO(
            1, "context", null, null, null, false, List.of(), environment, API_COLLECTION_ID, null, null, "name", null,
            PROJECT_ID, null, PROJECT_DEPLOYMENT_ID, 1, List.of(), 0);
    }

    private static ApiCollectionEndpointDTO apiCollectionEndpointDTO() {
        return new ApiCollectionEndpointDTO(
            API_COLLECTION_ID, null, null, false, null, API_COLLECTION_ENDPOINT_ID, null, null, "name", "path",
            PROJECT_DEPLOYMENT_WORKFLOW_ID, 0, "workflowUuid");
    }

    private static Method createApiCollectionMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("createApiCollection", ApiCollectionDTO.class);
    }

    private static Method createApiCollectionEndpointMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("createApiCollectionEndpoint", ApiCollectionEndpointDTO.class);
    }

    private static Method deleteApiCollectionMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("deleteApiCollection", long.class);
    }

    private static Method deleteApiCollectionEndpointMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("deleteApiCollectionEndpoint", long.class);
    }

    private static Method getOpenApiSpecificationMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("getOpenApiSpecification", long.class);
    }

    private static Method updateApiCollectionEndpointMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("updateApiCollectionEndpoint", ApiCollectionEndpointDTO.class);
    }

    private static Method updateApiCollectionTagsMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("updateApiCollectionTags", long.class, List.class);
    }

    private static Method getApiCollectionMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("getApiCollection", long.class);
    }

    private static Method getApiCollectionTagsMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("getApiCollectionTags", long.class);
    }

    private static Method getWorkspaceProjectsMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("getWorkspaceProjects", long.class);
    }

    private static Method updateApiCollectionMethod() throws NoSuchMethodException {
        return ApiCollectionFacadeImpl.class.getMethod("updateApiCollection", ApiCollectionDTO.class);
    }
}
