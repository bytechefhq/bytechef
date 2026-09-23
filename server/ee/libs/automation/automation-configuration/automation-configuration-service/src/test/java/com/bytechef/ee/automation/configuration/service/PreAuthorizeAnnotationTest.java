/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "updateWorkspaceUserRole", long.class, long.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class),
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod("removeWorkspaceUser", long.class, long.class),
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");

        // The per-environment writes grant and revoke a role just as the workspace-wide ones do, so they carry the
        // same scope. Left unguarded, any member could grant themselves ADMIN in an environment.
        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "setEnvironmentRole", long.class, long.class,
                com.bytechef.platform.configuration.domain.Environment.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class, Long.class),
            "hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "removeEnvironmentRole", long.class, long.class,
                com.bytechef.platform.configuration.domain.Environment.class),
            "hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod("getWorkspaceWorkspaceUsers", long.class),
            "hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')");

        // The workspace-level invite provisions a tenant account, so its guard is the one most worth pinning: a scope
        // rather than a role, because a custom role carrying WORKSPACE_MEMBER_MANAGE must work without a special case,
        // and anything weaker would let a non-manager onboard people.
        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "inviteWorkspaceUser", long.class, String.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class),
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");

        // The custom-role overloads are a second way into the same writes, so they need the same guard. Declaring
        // either of the shorter forms as a `default` method delegating here removes its annotation and runs the
        // delegation against the target rather than the proxy — an unguarded path that reads as harmless tidying.
        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "addWorkspaceUser", long.class, long.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class, Long.class),
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");

        assertPreAuthorize(
            WorkspaceUserService.class.getMethod(
                "inviteWorkspaceUser", long.class, String.class,
                com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.class, Long.class),
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");

        // The third way to change a member's effective permissions, and the one that was pinned nowhere: neither this
        // test nor either proxy enforcement test named it, so deleting its annotation left the whole module green. A
        // custom role may carry every management scope, so it needs the same guard as the built-in role writes.
        assertPreAuthorize(
            WorkspaceUserService.class.getMethod("assignCustomRole", long.class, long.class, long.class),
            "hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')");
    }

    @Test
    void testEePermissionServiceMyScopeReadsRequireAuthentication() throws Exception {
        // getMyWorkspaceScopes / getMyWorkspaceRole replaced the deleted getMyProjectScopes lookup. Both are
        // self-scoped reads gated only by isAuthenticated() — they expose the caller's own membership, so no
        // workspace-admin check is required, but an unauthenticated caller must not reach them.
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.service.PermissionServiceImpl");

        assertEveryPublicOverloadIsGated(clazz, "getMyWorkspaceScopes", "isAuthenticated()");
        assertEveryPublicOverloadIsGated(clazz, "getMyWorkspaceRole", "isAuthenticated()");
    }

    @Test
    void testEeWorkspaceServiceMutationsAreProtected() throws Exception {
        // Workspace lifecycle is tenant-admin-only; reading one needs WORKSPACE_VIEW on that workspace.
        // Reflect directly because WorkspaceService is a CE interface whose CE impl does not have these annotations.
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.service.WorkspaceServiceImpl");

        assertEveryPublicOverloadIsGated(clazz, "create", "isTenantAdmin()");
        assertEveryPublicOverloadIsGated(clazz, "delete", "isTenantAdmin()");
        assertEveryPublicOverloadIsGated(clazz, "update",
            "hasPermission(#workspace.id, 'Workspace', 'WORKSPACE_MANAGE')");
        assertEveryPublicOverloadIsGated(
            clazz, "getWorkspace", "hasPermission(#id, 'Workspace', 'WORKSPACE_VIEW')");
    }

    @Test
    void testCeProjectFacadeCreateMutationsAreProtected() throws Exception {
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectFacadeImpl");

        // createProject / importProject / importProjectTemplate all require the PROJECT_CREATE scope in the owning
        // workspace. Asserted per name rather than per overload, but every public overload of each name must be gated:
        // an ungated new overload of a gated entry point is a hole that reads as a harmless convenience method.
        assertEveryPublicOverloadIsGated(
            clazz, "createProject", "hasPermission(#projectDTO.workspaceId, 'Workspace', 'PROJECT_CREATE')");
        assertEveryPublicOverloadIsGated(
            clazz, "importProject", "hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')");
        assertEveryPublicOverloadIsGated(
            clazz, "importProjectTemplate", "hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')");
    }

    @Test
    void testCeProjectFacadeMutatingMethodsAreProtected() throws Exception {
        // Every mutating facade method must enforce authorization at the facade layer BEFORE any non-transactional
        // side effect runs (file-storage deletes, project-deployment cleanup, zip streaming). Deeper service-layer
        // @PreAuthorize provides defense-in-depth but fires too late to stop external state from being mutated.
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectFacadeImpl");

        assertEveryPublicOverloadIsGated(
            clazz, "deleteProject", "hasPermission(#id, 'Project', 'PROJECT_DELETE')");
        assertEveryPublicOverloadIsGated(
            clazz, "deleteSharedProject", "hasPermission(#id, 'Project', 'PROJECT_SETTINGS')");
        // duplicateProject writes: it creates a project and its workflows, and ProjectServiceImpl.create has no gate.
        // Pinned as the full conjunction so that neither half can be dropped — WORKFLOW_VIEW alone was the privilege
        // escalation this replaced, and PROJECT_CREATE alone would let a caller copy a project they cannot read.
        assertEveryPublicOverloadIsGated(
            clazz, "duplicateProject",
            "hasPermission(#id, 'Project', 'WORKFLOW_VIEW') and hasPermission(#id, 'Project', 'PROJECT_CREATE')");
        assertEveryPublicOverloadIsGated(
            clazz, "exportProject", "hasPermission(#id, 'Project', 'WORKFLOW_VIEW')");
        assertEveryPublicOverloadIsGated(
            clazz, "exportSharedProject", "hasPermission(#id, 'Project', 'PROJECT_SETTINGS')");
        assertEveryPublicOverloadIsGated(
            clazz, "publishProject", "hasPermission(#id, 'Project', 'WORKFLOW_EDIT')");
        assertEveryPublicOverloadIsGated(
            clazz, "updateProject", "hasPermission(#projectDTO.id, 'Project', 'WORKFLOW_EDIT')");
    }

    @Test
    void testCeProjectFacadeReadMethodsAreProtected() throws Exception {
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectFacadeImpl");

        // Read methods on the facade must be gated independently of the underlying service — the facade is a public
        // API surface (REST/GraphQL) and an unauthenticated cross-workspace enumeration is a security hole even if
        // the service eventually filters.
        assertEveryPublicOverloadIsGated(
            clazz, "getProject", "hasPermission(#id, 'Project', 'WORKFLOW_VIEW')");
        assertEveryPublicOverloadIsGated(
            clazz, "getWorkspaceProjects", "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')");
        assertEveryPublicOverloadIsGated(
            clazz, "getWorkspaceProjectWorkflows", "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')");
        // Tenant-wide listing without a workspaceId is admin-only.
        assertEveryPublicOverloadIsGated(clazz, "getProjects", "isTenantAdmin()");
    }

    @Test
    void testCeProjectServiceDeleteIsProtectedByProjectDelete() throws Exception {
        // ProjectServiceImpl.delete switched from WORKFLOW_DELETE to PROJECT_DELETE in the PR. Pin the new scope so
        // a future edit reverting the change is caught.
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.service.ProjectServiceImpl");

        assertEveryPublicOverloadIsGated(clazz, "delete", "hasPermission(#id, 'Project', 'PROJECT_DELETE')");
    }

    /**
     * Both Git operations were entirely unguarded: no annotation on the facade, no class-level gate, and neither of the
     * two {@code ProjectGitApiController} classes adds one. {@code PROJECT_PULL} had no call site anywhere in the tree
     * before the pull guard, so this is the pin that keeps a catalogued scope enforced by something.
     */
    @Test
    void testEeProjectGitFacadeMutationsAreProtected() throws Exception {
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.facade.ProjectGitFacadeImpl");

        assertEveryPublicOverloadIsGated(
            clazz, "pullProjectFromGit", "hasPermission(#projectId, 'Project', 'PROJECT_PULL')");
        assertEveryPublicOverloadIsGated(
            clazz, "pushProjectToGit", "hasPermission(#projectId, 'Project', 'PROJECT_PUSH')");
    }

    /**
     * Creating a deployment was authorized; repointing, deleting or running one was not. The guards are keyed on the
     * deployment id rather than on a DTO so that both the owning workspace and the environment come from the stored row
     * — see {@code ProjectDeploymentOwnershipResolver}, without which the {@code 'ProjectDeployment'} token would deny
     * every non-tenant-admin instead of checking anything.
     * <p>
     * Pinned by exact parameter types, not by name: {@code updateProjectDeployment} has three overloads and only one is
     * guarded, so a name-based assertion would stay green if the guard migrated to the wrong overload.
     */
    @Test
    void testCeProjectDeploymentFacadeMutationsAreProtected() throws Exception {
        Class<?> clazz = Class.forName(
            "com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl");

        assertOverloadPreAuthorize(
            clazz, "deleteProjectDeployment", new Class<?>[] {
                long.class
            }, "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_DELETE')");

        assertOverloadPreAuthorize(
            clazz, "createProjectDeploymentWorkflowJob", new Class<?>[] {
                Long.class, String.class
            }, "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')");

        assertOverloadPreAuthorize(
            clazz, "updateProjectDeploymentTags", new Class<?>[] {
                long.class, List.class
            }, "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')");

        // Keyed on the row's own id, which is what ProjectDeploymentWorkflowServiceImpl.update selects by. The REST
        // path supplies the deployment id and the row id independently, so a guard keyed on the argument's
        // projectDeploymentId would check a deployment the write never touches — pinned so it cannot drift back.
        assertOverloadPreAuthorize(
            clazz, "updateProjectDeploymentWorkflow", new Class<?>[] {
                com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow.class
            },
            "hasPermission(#projectDeploymentWorkflow.id, 'ProjectDeploymentWorkflow', 'DEPLOYMENT_EDIT')");

        // Enabling installs the deployment's triggers with its own connections, so leaving it open would have made the
        // job guard above bypassable — unattended repeated execution instead of one on-demand run. Disabling is the
        // first half of deleteProjectDeployment's tear-down and stops running jobs, so leaving it open would have made
        // half the delete guard bypassable too.
        assertOverloadPreAuthorize(
            clazz, "enableProjectDeployment", new Class<?>[] {
                long.class, boolean.class
            }, "hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')");

        assertOverloadPreAuthorize(
            clazz, "enableProjectDeploymentWorkflow", new Class<?>[] {
                long.class, String.class, boolean.class
            }, "hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')");

        // The four-argument overload carries its own annotation because it reaches the three-argument one by
        // self-invocation, which does not cross the proxy. It takes a projectId, so its check names 'Project' and is
        // environment-blind; its only production caller runs under skip-checks.
        assertOverloadPreAuthorize(
            clazz, "enableProjectDeploymentWorkflow", new Class<?>[] {
                long.class, String.class, boolean.class,
                Class.forName("com.bytechef.platform.configuration.domain.Environment")
            }, "hasPermission(#projectId, 'Project', 'DEPLOYMENT_EDIT')");

        assertOverloadPreAuthorize(
            clazz, "updateProjectDeployment", new Class<?>[] {
                com.bytechef.automation.configuration.dto.ProjectDeploymentDTO.class
            }, "hasPermission(#projectDeploymentDTO.id, 'ProjectDeployment', 'DEPLOYMENT_CREATE')");

        // The DTO-form guard on createProjectDeployment must survive: on create the DTO's environment IS the
        // environment the row lands in, so the promotion branch is the correct route there even though it is the wrong
        // one for update.
        assertOverloadPreAuthorize(
            clazz, "createProjectDeployment", new Class<?>[] {
                com.bytechef.automation.configuration.dto.ProjectDeploymentDTO.class
            }, "hasPermission(#projectDeploymentDTO, 'WORKFLOW_EDIT')");
    }

    @Test
    void testEeWorkspaceFacadeGetUserWorkspacesIsGated() throws Exception {
        // getUserWorkspaces(long id) was previously ungated — any authenticated user could enumerate another user's
        // workspace memberships by passing their id. The annotation pins the self-or-admin check. Pair with
        // PermissionService.isCurrentUser(long) which backs the SpEL expression.
        Class<?> clazz = Class.forName(
            "com.bytechef.ee.automation.configuration.facade.WorkspaceFacadeImpl");

        assertEveryPublicOverloadIsGated(
            clazz, "getUserWorkspaces",
            "isTenantAdmin() or isCurrentUser(#id)");
    }

    // NOTE: controller-level delegation is documented in each GraphQL controller's class-level Javadoc. The
    // controllers live in a sibling module (automation-configuration-graphql) and are not on this test's classpath,
    // so their existence is enforced at build time by module wiring rather than at runtime reflection. The delegation
    // itself is not covered by PreAuthorizeProxyEnforcementIntTest, which invokes the guarded services directly and
    // never goes through a controller: what that test proves is that the guards fire once reached, not that every
    // controller reaches them.

    /**
     * The scope catalogue is static metadata about what the server was built with — identical for every tenant, and
     * nobody's data. Gating it harder than authentication would stop a role editor listing what it may compose from.
     */
    @Test
    void testPermissionScopeGroupsRequiresOnlyAuthentication() throws NoSuchMethodException {
        assertPreAuthorize(
            CustomRoleService.class.getMethod("getPermissionScopeGroups"),
            "isAuthenticated()");
    }

    /**
     * The built-in tiers are the same kind of static metadata: what the server grants each role, identical for every
     * tenant and nobody's assignments.
     */
    @Test
    void testBuiltInRolesRequiresOnlyAuthentication() throws NoSuchMethodException {
        assertPreAuthorize(
            CustomRoleService.class.getMethod("getBuiltInRoles"),
            "isAuthenticated()");
    }

    /**
     * Mutations are tenant-admin-only: a custom role is tenant-global and assignable everywhere, so defining one is a
     * tenant-wide act. Pinned in full so a workspace-tier branch cannot quietly reappear.
     */
    @Test
    void testCustomRoleMutationsRequireTenantAdmin() throws NoSuchMethodException {
        assertPreAuthorize(
            CustomRoleService.class.getMethod(
                "createCustomRole", String.class, String.class, java.util.Set.class),
            "isTenantAdmin()");

        assertPreAuthorize(
            CustomRoleService.class.getMethod(
                "updateCustomRole", long.class, String.class, String.class, java.util.Set.class),
            "isTenantAdmin()");

        assertPreAuthorize(
            CustomRoleService.class.getMethod("deleteCustomRole", long.class),
            "isTenantAdmin()");
    }

    /**
     * The read is the deliberate remnant of the old two-tier model: it has two audiences — tenant admins managing
     * roles, and workspace member managers populating the assignment picker. The workspaceId argument is authorization
     * context only; the body returns every role either way.
     */
    @Test
    void testGetCustomRolesIsTieredForAssignmentReads() throws NoSuchMethodException {
        assertPreAuthorize(
            CustomRoleService.class.getMethod("getCustomRoles", Long.class),
            "(#workspaceId == null and isTenantAdmin()) or " +
                "(#workspaceId != null and hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE'))");
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
     * Asserts the {@code @PreAuthorize} on one specific overload, identified by its parameter types. Prefer this over
     * {@link #assertEveryPublicOverloadIsGated} when one specific overload's expression is what matters: that helper
     * requires every public overload to be gated but accepts the expected expression on any one of them, so it cannot
     * say which overload carries which guard.
     * <p>
     * Resolves the signature with {@code getDeclaredMethod} on {@code clazz} alone. A guard that moved onto the
     * interface would therefore read as absent even though Spring honours it -- a false failure rather than a false
     * pass, which is the safe direction for a pin.
     */
    private void assertOverloadPreAuthorize(
        Class<?> clazz, String methodName, Class<?>[] parameterTypes, String expectedExpression) {

        Method method;

        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new AssertionError(
                clazz.getSimpleName() + " has no overload " + methodName + Arrays.toString(parameterTypes),
                noSuchMethodException);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method " + clazz.getSimpleName() + "." + methodName + Arrays.toString(parameterTypes)
                    + " must be @PreAuthorize-protected")
            .isNotNull();

        assertThat(preAuthorize.value())
            .as(
                "Method " + clazz.getSimpleName() + "." + methodName + Arrays.toString(parameterTypes)
                    + " must use the documented SpEL expression")
            .isEqualTo(expectedExpression);
    }

    /**
     * Asserts that <em>every</em> public overload of {@code methodName} on {@code clazz} carries a
     * {@code @PreAuthorize}, and that at least one of them carries {@code expectedExpression}. Used when several
     * overloads exist (e.g., import-from-file vs. import-from-url) and the exact expression differs between them or is
     * not worth restating per overload — {@link #assertOverloadPreAuthorize} remains the stronger choice when one
     * specific overload's expression matters.
     * <p>
     * The "every public overload" half is the part that earns its keep. An assertion that only looked for <em>some</em>
     * annotated overload would be satisfied by the existing ones and so could not see a newly added, entirely ungated
     * overload of an already-gated entry point — which is exactly how a public API surface loses its guard without any
     * test noticing.
     * <p>
     * Private and package-private overloads are excluded because they are internal helpers that the Spring proxy never
     * intercepts; synthetic and bridge methods are excluded because javac generates them without annotations.
     * <p>
     * The search spans the whole type hierarchy — {@code clazz}, its superclasses and every interface it implements,
     * transitively — not just {@code clazz.getDeclaredMethods()}. A declared-methods-only scan cannot see an overload
     * that is not declared on the implementation, and the cheapest way to lose a guard is exactly that: add a
     * {@code default} overload to the interface delegating to the gated method, or move an overload onto an abstract
     * base. Spring proxies the public interface method, no annotation exists anywhere on it, and the entry point is
     * ungated. Each distinct signature is therefore resolved across the hierarchy and passes only if <em>some</em>
     * declaration of it carries a {@code @PreAuthorize} — which is also how Spring itself resolves the annotation, so a
     * guard living on the interface rather than the implementation is accepted.
     */
    private void assertEveryPublicOverloadIsGated(Class<?> clazz, String methodName, String expectedExpression) {
        Map<List<Class<?>>, List<Method>> declarationsBySignature = new LinkedHashMap<>();

        collectPublicOverloads(clazz, methodName, declarationsBySignature);

        boolean expectedExpressionFound = false;

        for (Map.Entry<List<Class<?>>, List<Method>> entry : declarationsBySignature.entrySet()) {
            PreAuthorize preAuthorize = null;

            for (Method declaration : entry.getValue()) {
                PreAuthorize declaredPreAuthorize = declaration.getAnnotation(PreAuthorize.class);

                if (declaredPreAuthorize != null) {
                    preAuthorize = declaredPreAuthorize;

                    if (declaredPreAuthorize.value()
                        .equals(expectedExpression)) {

                        expectedExpressionFound = true;

                        break;
                    }
                }
            }

            assertThat(preAuthorize)
                .as(
                    "Every public overload of " + methodName + " reachable through " + clazz.getSimpleName()
                        + " must be @PreAuthorize-protected, but " + describeSignature(entry.getValue(), methodName)
                        + " carries no annotation anywhere in the type hierarchy")
                .isNotNull();
        }

        assertThat(declarationsBySignature)
            .as(clazz.getSimpleName() + " must declare a public method named " + methodName)
            .isNotEmpty();

        assertThat(expectedExpressionFound)
            .as(
                clazz.getSimpleName() + " has no public overload of " + methodName
                    + " annotated with @PreAuthorize(" + expectedExpression + ")")
            .isTrue();
    }

    /**
     * Collects every public, non-synthetic, non-bridge declaration of {@code methodName} found on {@code type}, its
     * superclasses and its interfaces, keyed by erased parameter types so that an implementation and the interface
     * declaration it overrides land in the same bucket.
     */
    private void collectPublicOverloads(
        Class<?> type, String methodName, Map<List<Class<?>>, List<Method>> declarationsBySignature) {

        if (type == null || type == Object.class) {
            return;
        }

        for (Method method : type.getDeclaredMethods()) {
            if (!method.getName()
                .equals(methodName)) {
                continue;
            }

            if (method.isSynthetic() || method.isBridge() || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            List<Class<?>> signature = Arrays.asList(method.getParameterTypes());

            declarationsBySignature.computeIfAbsent(signature, key -> new ArrayList<>())
                .add(method);
        }

        collectPublicOverloads(type.getSuperclass(), methodName, declarationsBySignature);

        for (Class<?> interfaceType : type.getInterfaces()) {
            collectPublicOverloads(interfaceType, methodName, declarationsBySignature);
        }
    }

    private String describeSignature(List<Method> declarations, String methodName) {
        Method declaration = declarations.getFirst();

        return declaration.getDeclaringClass()
            .getSimpleName() + "." + methodName + Arrays.toString(declaration.getParameterTypes());
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
