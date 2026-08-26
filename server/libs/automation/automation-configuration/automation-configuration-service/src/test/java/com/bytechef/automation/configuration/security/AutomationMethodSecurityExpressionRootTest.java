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

import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import java.util.List;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class AutomationMethodSecurityExpressionRootTest {

    private static final Long DEVELOPMENT_ENVIRONMENT_ID = (long) Environment.DEVELOPMENT.ordinal();
    private static final Long PRODUCTION_ENVIRONMENT_ID = (long) Environment.PRODUCTION.ordinal();

    private ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;
    private PermissionService permissionService;
    private AutomationMethodSecurityExpressionRoot root;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        permissionService = mock(PermissionService.class);
        resourceMembershipResolverProvider = mock(ObjectProvider.class);

        Supplier<Authentication> authentication = () -> mock(Authentication.class);
        MethodInvocation methodInvocation = mock(MethodInvocation.class);

        root = new AutomationMethodSecurityExpressionRoot(
            authentication, methodInvocation, permissionService, resourceMembershipResolverProvider);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsCurrentUserDelegates() {
        when(permissionService.isCurrentUser(7L)).thenReturn(true);

        assertThat(root.isCurrentUser(7L)).isTrue();
    }

    @Test
    void testIsTenantAdminDelegates() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(root.isTenantAdmin()).isTrue();
    }

    @Test
    void testIsResourceOwnerDelegates() {
        when(permissionService.isResourceOwner("ApiKey", 9L)).thenReturn(true);

        assertThat(root.isResourceOwner(9L, "ApiKey")).isTrue();
    }

    @Test
    void testIsCurrentUserShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isCurrentUser(7L)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsTenantAdminShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isTenantAdmin()).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsResourceOwnerShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isResourceOwner(9L, "ApiKey")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentResolvesTheOrdinalAndDelegates() {
        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.PRODUCTION)).thenReturn(true);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", PRODUCTION_ENVIRONMENT_ID)).isTrue();

        verify(permissionService).hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentDeniesNullEnvironmentId() {
        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", null)).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentDeniesNegativeEnvironmentId() {
        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", -1L)).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentDeniesOutOfRangeEnvironmentId() {
        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", (long) Environment.values().length))
            .isFalse();

        verifyNoInteractions(permissionService);
    }

    /**
     * Pins the order of the two guards. The skip check must run before the ordinal is validated, so that validating
     * first cannot deny a delegation the skip is meant to permit -- a security fix silently taking out a working
     * feature. Swap the two blocks and this test goes red.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentPermitsUnderSkipModeEvenWhenEnvironmentIdIsInvalid() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", null)).isTrue();
            assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", -1L)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    // -- Ticket 1051: the decider governs this built-in too -------------------------------------------------------

    /**
     * The Critical this fix round closed, at unit level. {@code WorkflowTestApiController.startWorkflowTest} -- the
     * embedded builder's Run button -- is the only production gate using this built-in, and it reads the skip state
     * itself rather than going through {@code hasResourceScope}. Remove the decider call and this goes red: the short
     * circuit below would grant any workflow id in the tenant.
     *
     * <p>
     * This is also why the resource-scoped skip mode ticket 1051 Stage 4 deleted had no decision left to make. The
     * decider returns before any skip state is read, so a narrower mode could not have changed this answer.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentDeniesGovernedPrincipalUnderSkipMode() throws Throwable {
        governedResolver(Decision.DENIED);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", PRODUCTION_ENVIRONMENT_ID))
                .isFalse();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentDeniesGovernedNotApplicable() {
        governedResolver(Decision.NOT_APPLICABLE);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", PRODUCTION_ENVIRONMENT_ID)).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentGrantsGovernedPrincipalWithoutConsultingPermissionService() {
        governedResolver(Decision.GRANTED);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", PRODUCTION_ENVIRONMENT_ID)).isTrue();

        verifyNoInteractions(permissionService);
    }

    /**
     * Membership says whose workflow it is, not which environment it may run in. A granted governed principal is still
     * held to the ordinal check, or this gate's entire reason for existing -- that an environment which cannot be
     * identified cannot be authorised -- would be waived for exactly the principals it was tightened for.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentStillValidatesTheOrdinalForAGrantedGovernedPrincipal() {
        governedResolver(Decision.GRANTED);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", null)).isFalse();
        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", -1L)).isFalse();
        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", (long) Environment.values().length))
            .isFalse();

        verifyNoInteractions(permissionService);
    }

    /**
     * Containment: an ungoverned principal keeps the pre-seam behaviour of this built-in exactly, skip mode and invalid
     * ordinal included. That is the invariant
     * {@link #testHasWorkflowScopeInEnvironmentPermitsUnderSkipModeEvenWhenEnvironmentIdIsInvalid()} pins, this time
     * with a resolver present that would have denied had it been asked.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentUnchangedForUngovernedPrincipal() throws Throwable {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(false);
        when(resourceMembershipResolver.resolve(any(), anyString(), anyString())).thenReturn(Decision.DENIED);
        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.PRODUCTION)).thenReturn(true);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", null)).isTrue();

            return null;
        });

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", PRODUCTION_ENVIRONMENT_ID)).isTrue();

        verify(resourceMembershipResolver, never()).resolve(any(), anyString(), anyString());
    }

    // -- Ticket 1051: the effective environment ---------------------------------------------------------------------

    /**
     * The case the reverted environment-match check 403'd. The embedded client sends {@code environmentId=0} because
     * its store defaults to DEVELOPMENT and the embed never moves it, while the principal is confined to PRODUCTION.
     * Nothing unusual was asked for, so it must be granted -- and it is, because the parameter is ignored rather than
     * compared.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentGrantsAConfinedPrincipalWhateverOrdinalTheClientSent() {
        authenticateAsConfinedPrincipal(PRODUCTION_ENVIRONMENT_ID);
        governedResolver(Decision.GRANTED);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", DEVELOPMENT_ENVIRONMENT_ID)).isTrue();

        verifyNoInteractions(permissionService);
    }

    /**
     * The substitution reaches the scope check too, not just the range check: an api-key principal the membership
     * resolver does not govern is still confined, so the scope is checked in ITS environment rather than the one the
     * request named.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentChecksTheScopeInThePrincipalsEnvironment() {
        authenticateAsConfinedPrincipal(PRODUCTION_ENVIRONMENT_ID);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(null);
        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.PRODUCTION)).thenReturn(true);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", DEVELOPMENT_ENVIRONMENT_ID)).isTrue();

        verify(permissionService).hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    /**
     * Containment: a session principal is not confined, so its requested environment is honoured unchanged. Every
     * non-embedded caller of this built-in depends on that.
     */
    @Test
    void testHasWorkflowScopeInEnvironmentHonoursTheRequestedEnvironmentForASessionPrincipal() {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(null);
        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT)).thenReturn(true);

        assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", DEVELOPMENT_ENVIRONMENT_ID)).isTrue();

        verify(permissionService).hasWorkflowScope("wf-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT);
    }

    private static void authenticateAsConfinedPrincipal(long environmentId) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new TestApiKeyAuthenticationToken(environmentId, new User("connected-user-1", "", List.of())));
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }

    private void governedResolver(Decision decision) {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve("wf-1", "Workflow", "WORKFLOW_EDIT")).thenReturn(decision);
    }

    @Test
    void testAllSixOverridesStillShortCircuitUnderFullSkip() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isTenantAdmin()).isTrue();
            assertThat(root.isCurrentUser(7L)).isTrue();
            assertThat(root.isResourceOwner(9L, "Connection")).isTrue();
            assertThat(root.hasWorkspaceScopeInEveryEnvironment(1L, "WORKSPACE_MEMBER_MANAGE")).isTrue();
            assertThat(root.hasWorkspaceScopeInEnvironment(1L, "WORKSPACE_MEMBER_MANAGE", Environment.PRODUCTION))
                .isTrue();
            assertThat(root.hasWorkflowScopeInEnvironment("wf-1", "WORKFLOW_EDIT", PRODUCTION_ENVIRONMENT_ID)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }
}
