/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AutomationPermissionEvaluatorTest {

    private ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;
    private PermissionService permissionService;
    private AutomationPermissionEvaluator evaluator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        permissionService = mock(PermissionService.class);
        resourceMembershipResolverProvider = mock(ObjectProvider.class);
        evaluator = new AutomationPermissionEvaluator(permissionService, resourceMembershipResolverProvider);
    }

    @Test
    void testNumericResourceTypeDelegatesToHasResourceScope() {
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_DELETE")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 5L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testWorkspaceTokenDelegatesToHasResourceScope() {
        when(permissionService.hasResourceScope(3L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 3L, "Workspace", "WORKFLOW_VIEW")).isTrue();
    }

    @Test
    void testStringWorkflowIdDelegatesToHasResourceScope() {
        // The String workflow UUID flows through the same hasResourceScope(Serializable, ...) path.
        when(permissionService.hasResourceScope("workflow-1", "Workflow", "WORKFLOW_EDIT")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, "workflow-1", "Workflow", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testDeniedWhenHasResourceScopeReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 9L, "Bogus", "X")).isFalse();
    }

    @Test
    void testTwoArgFormAlwaysDeniesAndNeverTouchesPermissionService() {
        // The former hasPermission('Tenant', 'ADMIN') tenant-admin check is now the isTenantAdmin() SpEL built-in on
        // AutomationMethodSecurityExpressionRoot, so the two-argument overload no longer authorizes anything.
        assertThat(evaluator.hasPermission(null, "Tenant", "ADMIN")).isFalse();
        assertThat(evaluator.hasPermission(null, "Something", "ADMIN")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testSkipChecksShortCircuitsWithoutTouchingPermissionService() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 1L, "Project", "PROJECT_DELETE")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testFullSkipShortCircuitsWorkspaceTargetToo() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 3L, "Workspace", "WORKFLOW_VIEW")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    // -- Ticket 1051 Stage 2: the resolver decides -----------------------------------------------------------------

    // Blast-radius containment. Rules 1 and 2 of the precedence rule are the only thing keeping Community Edition and
    // every non-connected-user principal on the path they were on before this seam existed, so they are asserted
    // directly against the pre-seam answer rather than only in terms of the resolver.

    /**
     * Rule 1, no skip mode: an absent resolver (Community Edition) delegates to permissionService exactly as before.
     */
    @Test
    void testAbsentResolverNeverConsulted() {
        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(null);
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_DELETE")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 5L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    /**
     * Rule 1 under skip mode: an absent resolver leaves @SkipAutomationAuthorization doing exactly what it did before.
     * Community Edition has no connected users, and this is what says so.
     */
    @Test
    void testAbsentResolverLeavesSkipModeUntouched() throws Throwable {
        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(null);
        when(permissionService.hasResourceScope(1L, "Project", "PROJECT_DELETE")).thenReturn(false);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 1L, "Project", "PROJECT_DELETE")).isTrue();

            return null;
        });

        assertThat(evaluator.hasPermission(null, 1L, "Project", "PROJECT_DELETE")).isFalse();
    }

    /**
     * Rule 2, no skip mode: a principal the resolver does not govern is never resolved, and gets permissionService's
     * answer.
     */
    @Test
    void testUngovernedPrincipalNeverCallsResolve() {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(false);
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_DELETE")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 5L, "Connection", "CONNECTION_DELETE")).isTrue();

        verify(resourceMembershipResolver, never()).resolve(5L, "Connection", "CONNECTION_DELETE");
    }

    /**
     * Rule 2 under skip mode, asserted against an evaluator with no resolver at all: an ungoverned principal must get
     * byte-for-byte the pre-seam answer, skip mode included. This is the whole containment story now that the resolver
     * is authoritative -- the resolver here would answer DENIED if it were ever asked, and must not be.
     */
    @Test
    void testUngovernedPrincipalGetsThePreSeamAnswerUnderSkipMode() throws Throwable {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(false);
        when(resourceMembershipResolver.resolve(any(), anyString(), anyString())).thenReturn(Decision.DENIED);
        when(permissionService.hasResourceScope(1L, "Project", "PROJECT_DELETE")).thenReturn(false);

        AutomationPermissionEvaluator withoutResolver = new AutomationPermissionEvaluator(
            permissionService, emptyProvider());

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 1L, "Project", "PROJECT_DELETE"))
                .isEqualTo(withoutResolver.hasPermission(null, 1L, "Project", "PROJECT_DELETE"))
                .isTrue();

            return null;
        });

        assertThat(evaluator.hasPermission(null, 1L, "Project", "PROJECT_DELETE"))
            .isEqualTo(withoutResolver.hasPermission(null, 1L, "Project", "PROJECT_DELETE"))
            .isFalse();

        verify(resourceMembershipResolver, never()).resolve(any(), anyString(), anyString());
    }

    /**
     * Rule 3: a governed GRANTED answer grants even when permissionService would refuse, and permissionService is never
     * asked.
     */
    @Test
    void testGovernedGrantedGrantsWithoutConsultingPermissionService() {
        governedResolver(Decision.GRANTED, "workflow-1", "Workflow", "WORKFLOW_EDIT");

        when(permissionService.hasResourceScope("workflow-1", "Workflow", "WORKFLOW_EDIT")).thenReturn(false);

        assertThat(evaluator.hasPermission(null, "workflow-1", "Workflow", "WORKFLOW_EDIT")).isTrue();

        verifyNoInteractions(permissionService);
    }

    /**
     * Rule 4: a governed DENIED answer denies even though permissionService would allow.
     */
    @Test
    void testGovernedDeniedDeniesWithoutConsultingPermissionService() {
        governedResolver(Decision.DENIED, "workflow-1", "Workflow", "WORKFLOW_EDIT");

        when(permissionService.hasResourceScope("workflow-1", "Workflow", "WORKFLOW_EDIT")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, "workflow-1", "Workflow", "WORKFLOW_EDIT")).isFalse();

        verifyNoInteractions(permissionService);
    }

    /**
     * Rule 5: a governed principal asking about a kind of check the resolver has no predicate for is denied, not handed
     * to the ordinary path.
     */
    @Test
    void testGovernedNotApplicableDenies() {
        governedResolver(Decision.NOT_APPLICABLE, 9L, "SomethingElse", "X");

        assertThat(evaluator.hasPermission(null, 9L, "SomethingElse", "X")).isFalse();
    }

    /**
     * The load-bearing test for Stage 2, and the reason isSkipChecks(...) is deliberately absent from the precedence
     * rule: a governed principal is denied under skip mode, which {@code @SkipAutomationAuthorization} arms on
     * embedded's own facades. Let isSkipChecks back into the rule and this goes red. It is also what makes the
     * now-deleted resource-scoped skip mode unnecessary: the decider returns before any skip state is read, so no
     * narrower mode has a decision left to make for a governed principal.
     */
    @Test
    void testGovernedDeniedIsNotRescuedBySkipMode() throws Throwable {
        governedResolver(Decision.DENIED, 42L, "Project", "PROJECT_DELETE");

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 42L, "Project", "PROJECT_DELETE")).isFalse();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    /**
     * A governed NOT_APPLICABLE is not rescued by skip mode either -- otherwise every resource type the resolver has no
     * predicate for would still be granted to a connected user.
     */
    @Test
    void testGovernedNotApplicableIsNotRescuedBySkipMode() throws Throwable {
        governedResolver(Decision.NOT_APPLICABLE, 3L, "Workspace", "WORKFLOW_VIEW");

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 3L, "Workspace", "WORKFLOW_VIEW")).isFalse();

            return null;
        });
    }

    @Test
    void testProjectDeploymentBranchIsDecidedByTheResolverAheadOfSkip() throws Throwable {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(7L, "Project", "DEPLOYMENT_PUSH")).thenReturn(Decision.DENIED);

        ProjectDeploymentDTO projectDeploymentDTO = mock(ProjectDeploymentDTO.class);

        when(projectDeploymentDTO.projectId()).thenReturn(7L);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, projectDeploymentDTO, "DEPLOYMENT_PUSH")).isFalse();

            return null;
        });

        verify(resourceMembershipResolver).resolve(7L, "Project", "DEPLOYMENT_PUSH");
        verifyNoInteractions(permissionService);
    }

    @Test
    void testProjectDeploymentBranchGrantsWhenGovernedResolverGrants() {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(7L, "Project", "DEPLOYMENT_PUSH")).thenReturn(Decision.GRANTED);

        ProjectDeploymentDTO projectDeploymentDTO = mock(ProjectDeploymentDTO.class);

        when(projectDeploymentDTO.projectId()).thenReturn(7L);

        assertThat(evaluator.hasPermission(null, projectDeploymentDTO, "DEPLOYMENT_PUSH")).isTrue();

        verifyNoInteractions(permissionService);
    }

    /**
     * An ungoverned principal keeps the pre-seam ProjectDeploymentDTO behaviour, environment-keyed check included.
     */
    @Test
    void testProjectDeploymentBranchUnchangedForUngovernedPrincipal() {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(false);

        ProjectDeploymentDTO projectDeploymentDTO = mock(ProjectDeploymentDTO.class);

        when(projectDeploymentDTO.projectId()).thenReturn(7L);
        when(projectDeploymentDTO.environment()).thenReturn(Environment.PRODUCTION);
        when(permissionService.hasWorkspaceScopeForProject(7L, "DEPLOYMENT_PUSH", Environment.PRODUCTION))
            .thenReturn(true);

        assertThat(evaluator.hasPermission(null, projectDeploymentDTO, "DEPLOYMENT_PUSH")).isTrue();

        verify(resourceMembershipResolver, never()).resolve(any(), anyString(), anyString());
    }

    private void governedResolver(Decision decision, Serializable id, String resourceType, String scope) {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(id, resourceType, scope)).thenReturn(decision);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceMembershipResolver> emptyProvider() {
        ObjectProvider<ResourceMembershipResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(null);

        return objectProvider;
    }
}
