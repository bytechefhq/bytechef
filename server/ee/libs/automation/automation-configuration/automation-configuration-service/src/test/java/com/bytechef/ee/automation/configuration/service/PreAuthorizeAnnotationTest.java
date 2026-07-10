/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PreAuthorizeAnnotationTest {

    @Test
    void testWorkspaceUserServiceMutationsAreProtected() throws NoSuchMethodException {
        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "addWorkspaceUser", long.class, long.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class),
            "hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "updateWorkspaceUserRole", long.class, long.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class),
            "hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod("removeWorkspaceUser", long.class, long.class),
            "hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod("getWorkspaceWorkspaceUsers", long.class),
            "hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')");
    }

    @Test
    void testEePermissionServiceMyScopeReadsRequireAuthentication() throws Exception {
        // getMyWorkspaceScopes / getMyWorkspaceRole replaced the deleted getMyProjectScopes lookup. Both are
        // self-scoped reads gated only by isAuthenticated() — they expose the caller's own membership, so no
        // workspace-admin check is required, but an unauthenticated caller must not reach them.
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.service.PermissionServiceImpl");

        assertMethodPreAuthorizeExists(clazz, "getMyWorkspaceScopes", "isAuthenticated()");
        assertMethodPreAuthorizeExists(clazz, "getMyWorkspaceRole", "isAuthenticated()");
    }

    @Disabled("Ahead of production on this rebuild commit: WorkspaceServiceImpl gains these @PreAuthorize annotations "
        + "in the 'Add custom roles and permissions' commit. Re-enable once that commit is replayed.")
    @Test
    void testEeWorkspaceServiceMutationsAreProtected() throws Exception {
        // EE WorkspaceServiceImpl gained @PreAuthorize on create/delete/update/getWorkspace in the RBAC PR.
        // Reflect directly because WorkspaceService is a CE interface whose CE impl does not have these annotations.
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.service.WorkspaceServiceImpl");

        assertMethodPreAuthorizeExists(clazz, "create", "isTenantAdmin()");
        assertMethodPreAuthorizeExists(clazz, "delete", "isTenantAdmin()");
        assertMethodPreAuthorizeExists(clazz, "update",
            "hasPermission(#workspace.id, 'Workspace', 'WORKSPACE_MANAGE')");
        assertMethodPreAuthorizeExists(
            clazz, "getWorkspace", "hasPermission(#id, 'Workspace', 'WORKSPACE_VIEW')");
    }

    @Test
    void testCeProjectFacadeCreateMutationsAreProtected() throws Exception {
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectFacadeImpl");

        // createProject / importProject / importProjectTemplate all require workspace EDITOR membership. Assert the
        // annotation is present on at least one method per name rather than enumerating overloads — the important
        // property is that SOME overload gates the entry point.
        assertAnyMethodHasPreAuthorize(
            clazz, "createProject", "hasPermission(#projectDTO.workspaceId, 'Workspace', 'PROJECT_CREATE')");
        assertAnyMethodHasPreAuthorize(
            clazz, "importProject", "hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')");
        assertAnyMethodHasPreAuthorize(
            clazz, "importProjectTemplate", "hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')");
    }

    @Test
    void testCeProjectFacadeMutatingMethodsAreProtected() throws Exception {
        // Every mutating facade method must enforce authorization at the facade layer BEFORE any non-transactional
        // side effect runs (file-storage deletes, project-deployment cleanup, zip streaming). Deeper service-layer
        // @PreAuthorize provides defense-in-depth but fires too late to stop external state from being mutated.
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectFacadeImpl");

        assertMethodPreAuthorizeExists(
            clazz, "deleteProject", "hasPermission(#id, 'Project', 'PROJECT_DELETE')");
        assertMethodPreAuthorizeExists(
            clazz, "deleteSharedProject", "hasPermission(#id, 'Project', 'PROJECT_SETTINGS')");
        assertMethodPreAuthorizeExists(
            clazz, "duplicateProject", "hasPermission(#id, 'Project', 'WORKFLOW_VIEW')");
        assertMethodPreAuthorizeExists(
            clazz, "exportProject", "hasPermission(#id, 'Project', 'WORKFLOW_VIEW')");
        assertMethodPreAuthorizeExists(
            clazz, "exportSharedProject", "hasPermission(#id, 'Project', 'PROJECT_SETTINGS')");
        assertMethodPreAuthorizeExists(
            clazz, "publishProject", "hasPermission(#id, 'Project', 'WORKFLOW_EDIT')");
        assertMethodPreAuthorizeExists(
            clazz, "updateProject", "hasPermission(#projectDTO.id, 'Project', 'WORKFLOW_EDIT')");
    }

    @Test
    void testCeProjectFacadeReadMethodsAreProtected() throws Exception {
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectFacadeImpl");

        // Read methods on the facade must be gated independently of the underlying service — the facade is a public
        // API surface (REST/GraphQL) and an unauthenticated cross-workspace enumeration is a security hole even if
        // the service eventually filters.
        assertMethodPreAuthorizeExists(
            clazz, "getProject", "hasPermission(#id, 'Project', 'WORKFLOW_VIEW')");
        assertAnyMethodHasPreAuthorize(
            clazz, "getWorkspaceProjects", "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')");
        assertMethodPreAuthorizeExists(
            clazz, "getWorkspaceProjectWorkflows", "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')");
        // Tenant-wide listing without a workspaceId is admin-only.
        assertAnyMethodHasPreAuthorize(clazz, "getProjects", "isTenantAdmin()");
    }

    @Test
    void testCeProjectServiceDeleteIsProtectedByProjectDelete() throws Exception {
        // ProjectServiceImpl.delete switched from WORKFLOW_DELETE to PROJECT_DELETE in the PR. Pin the new scope so
        // a future edit reverting the change is caught.
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.service.ProjectServiceImpl");

        assertMethodPreAuthorizeExists(clazz, "delete", "hasPermission(#id, 'Project', 'PROJECT_DELETE')");
    }

    @Disabled("Ahead of production on this rebuild commit: WorkspaceFacadeImpl is introduced by the 'Add custom roles "
        + "and permissions' commit. Re-enable once that commit is replayed.")
    @Test
    void testEeWorkspaceFacadeGetUserWorkspacesIsGated() throws Exception {
        // getUserWorkspaces(long id) was previously ungated — any authenticated user could enumerate another user's
        // workspace memberships by passing their id. The annotation pins the self-or-admin check. Pair with
        // PermissionService.isCurrentUser(long) which backs the SpEL expression.
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.facade.WorkspaceFacadeImpl");

        assertMethodPreAuthorizeExists(
            clazz, "getUserWorkspaces",
            "isTenantAdmin() or isCurrentUser(#id)");
    }

    private void assertPreAuthorize(Method interfaceMethod, String expectedExpression) {
        // The annotation lives on the implementation, not the interface, so resolve through the impl class. We
        // accept any class in the same package whose name follows the conventional ServiceImpl pattern.
        Method implMethod = findImplMethod(interfaceMethod);

        PreAuthorize preAuthorize = implMethod.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method " + implMethod.getDeclaringClass()
                    .getSimpleName() + "." + implMethod.getName() + " must be @PreAuthorize-protected")
            .isNotNull();

        assertThat(preAuthorize.value())
            .as(
                "Method " + implMethod.getDeclaringClass()
                    .getSimpleName() + "." + implMethod.getName() + " must use the documented SpEL expression")
            .isEqualTo(expectedExpression);
    }

    /**
     * Locates a method by name on the given class (first arity match) and asserts that it has the expected
     * {@code @PreAuthorize} expression. Used for facades/services where the interface hierarchy does not follow the
     * simple {@code *Service → *ServiceImpl} convention.
     */
    private void assertMethodPreAuthorizeExists(Class<?> clazz, String methodName, String expectedExpression) {
        Method match = null;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName()
                .equals(methodName) && method.isAnnotationPresent(PreAuthorize.class)) {
                match = method;

                break;
            }
        }

        assertThat(match)
            .as("Class " + clazz.getSimpleName() + " must have a @PreAuthorize-annotated method named " + methodName)
            .isNotNull();

        assertThat(match.getAnnotation(PreAuthorize.class)
            .value())
                .as(clazz.getSimpleName() + "." + methodName + " must use the documented SpEL expression")
                .isEqualTo(expectedExpression);
    }

    /**
     * Asserts that at least one overload of {@code methodName} on {@code clazz} has the expected {@code @PreAuthorize}.
     * Used when multiple overloads exist (e.g., import-from-file vs. import-from-url) and we only require that the
     * entry point is gated.
     */
    private void assertAnyMethodHasPreAuthorize(Class<?> clazz, String methodName, String expectedExpression) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName()
                .equals(methodName)) {
                continue;
            }

            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

            if (preAuthorize != null && preAuthorize.value()
                .equals(expectedExpression)) {
                return;
            }
        }

        throw new AssertionError(
            clazz.getSimpleName() + " has no overload of " + methodName
                + " annotated with @PreAuthorize(" + expectedExpression + ")");
    }

    private Method findImplMethod(Method interfaceMethod) {
        String interfaceName = interfaceMethod.getDeclaringClass()
            .getName();
        // e.g. com.bytechef.ee.automation.configuration.service.WorkspaceUserService
        // -> com.bytechef.ee.automation.configuration.service.WorkspaceUserServiceImpl
        String implName = interfaceName + "Impl";

        try {
            Class<?> implClass = Class.forName(implName);

            return implClass.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            throw new AssertionError(
                "Could not resolve implementation method for " + interfaceMethod, exception);
        }
    }
}
