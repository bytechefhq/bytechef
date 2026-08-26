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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceMembershipDecider.Outcome;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins ticket 1051's precedence rule, one test per rule, in the order the rules are written down. The rule is
 * load-bearing and inverting any two of its arms compiles cleanly, so each arm is asserted on its own rather than only
 * through the two call sites that consume it.
 */
class ResourceMembershipDeciderTest {

    private ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider;
    private ResourceMembershipResolver resourceMembershipResolver;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        resourceMembershipResolver = mock(ResourceMembershipResolver.class);
        resourceMembershipResolverProvider = mock(ObjectProvider.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);
    }

    /**
     * Rule 1: no resolver bean at all -- Community Edition -- hands the check back to the ordinary path.
     */
    @Test
    @SuppressWarnings("unchecked")
    void testAbsentResolverIsNotGoverned() {
        ObjectProvider<ResourceMembershipResolver> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        assertThat(ResourceMembershipDecider.decide(emptyProvider, 1L, "Project", "PROJECT_DELETE"))
            .isEqualTo(Outcome.NOT_GOVERNED);
    }

    /**
     * Rule 2: a principal the resolver does not govern -- an ordinary workspace user, a tenant admin, a background
     * thread -- hands the check back to the ordinary path, and is never resolved at all.
     */
    @Test
    void testUngovernedPrincipalIsNotGovernedAndNeverResolved() {
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(false);

        assertThat(
            ResourceMembershipDecider.decide(resourceMembershipResolverProvider, 1L, "Project", "PROJECT_DELETE"))
                .isEqualTo(Outcome.NOT_GOVERNED);

        verify(resourceMembershipResolver, never()).resolve(any(), anyString(), anyString());
    }

    /**
     * Rule 3.
     */
    @Test
    void testGovernedGrantedGrants() {
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT"))
            .thenReturn(Decision.GRANTED);

        assertThat(
            ResourceMembershipDecider.decide(
                resourceMembershipResolverProvider, "workflow-1", "Workflow", "WORKFLOW_EDIT"))
                    .isEqualTo(Outcome.GRANT);
    }

    /**
     * Rule 4.
     */
    @Test
    void testGovernedDeniedDenies() {
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve("workflow-1", "Workflow", "WORKFLOW_EDIT")).thenReturn(Decision.DENIED);

        assertThat(
            ResourceMembershipDecider.decide(
                resourceMembershipResolverProvider, "workflow-1", "Workflow", "WORKFLOW_EDIT"))
                    .isEqualTo(Outcome.DENY);
    }

    /**
     * Rule 5, the arm most easily written the other way round: a governed principal asking about a kind of check the
     * resolver has no predicate for is DENIED, not handed back to the ordinary path. Handing it back would restore the
     * hole for every resource type the resolver does not yet know about.
     */
    @Test
    void testGovernedNotApplicableDenies() {
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(9L, "SomethingElse", "X")).thenReturn(Decision.NOT_APPLICABLE);

        assertThat(ResourceMembershipDecider.decide(resourceMembershipResolverProvider, 9L, "SomethingElse", "X"))
            .isEqualTo(Outcome.DENY);
    }

    /**
     * governsCurrentPrincipal() must be asked before resolve(...), not after: it is the cheap check that keeps ordinary
     * traffic off the membership queries entirely.
     */
    @Test
    void testGovernanceIsAskedBeforeResolving() {
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(1L, "Project", "PROJECT_DELETE")).thenReturn(Decision.GRANTED);

        ResourceMembershipDecider.decide(resourceMembershipResolverProvider, 1L, "Project", "PROJECT_DELETE");

        InOrder inOrder = inOrder(resourceMembershipResolver);

        inOrder.verify(resourceMembershipResolver)
            .governsCurrentPrincipal();
        inOrder.verify(resourceMembershipResolver)
            .resolve(1L, "Project", "PROJECT_DELETE");
    }

    /**
     * The skip mode is deliberately not part of the rule. A governed principal is denied under skip mode and outside it
     * alike -- the decider never reads {@link AutomationAuthorizationContext} at all. This is what made ticket 1051
     * Stage 4's removal of the narrower resource-scoped mode a no-op for a governed principal: the decider returns
     * before any skip state is read, so no mode had a decision left to make.
     */
    @Test
    void testSkipModeDoesNotEnterTheRule() throws Throwable {
        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(1L, "Project", "PROJECT_DELETE")).thenReturn(Decision.DENIED);

        assertThat(
            ResourceMembershipDecider.decide(resourceMembershipResolverProvider, 1L, "Project", "PROJECT_DELETE"))
                .isEqualTo(Outcome.DENY);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(
                ResourceMembershipDecider.decide(resourceMembershipResolverProvider, 1L, "Project", "PROJECT_DELETE"))
                    .isEqualTo(Outcome.DENY);

            return null;
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAbsentResolverIsNotGovernedUnderFullSkipToo() throws Throwable {
        ObjectProvider<ResourceMembershipResolver> emptyProvider = mock(ObjectProvider.class);

        when(emptyProvider.getIfAvailable()).thenReturn(null);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(ResourceMembershipDecider.decide(emptyProvider, 1L, "Project", "PROJECT_DELETE"))
                .isEqualTo(Outcome.NOT_GOVERNED);

            return null;
        });

        verifyNoInteractions(resourceMembershipResolver);
    }
}
