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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceMembershipDecider.Outcome;
import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AutomationAuthorizationContextTest {

    @Test
    void testIsSkipChecksDefaultsFalse() {
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testCallSkippingChecksEnablesDuringCallAndRestoresAfter() throws Throwable {
        AtomicBoolean insideValue = new AtomicBoolean(false);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            insideValue.set(AutomationAuthorizationContext.isSkipChecks());

            return null;
        });

        assertThat(insideValue.get()).isTrue();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testCallSkippingChecksRestoresOnException() {
        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testNestedCallsRestoreToOuterValue() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            AutomationAuthorizationContext.callSkippingChecks(() -> null);

            assertThat(AutomationAuthorizationContext.isSkipChecks()).isTrue();

            return null;
        });

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testNestedCallsRestoreToOuterValueOnException() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class);

            assertThat(AutomationAuthorizationContext.isSkipChecks()).isTrue();

            return null;
        });

        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testSkipDoesNotLeakToNewThread() throws Throwable {
        AtomicBoolean otherThreadValue = new AtomicBoolean(true);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            Thread thread = new Thread(() -> otherThreadValue.set(AutomationAuthorizationContext.isSkipChecks()));

            thread.start();
            thread.join();

            return null;
        });

        assertThat(otherThreadValue.get()).isFalse();
    }

    /**
     * The guarantee that replaced the monotonic-narrowing rule.
     *
     * <p>
     * Narrowing existed so that a stack which had entered the resource-scoped skip mode could not be re-widened to full
     * by a nested {@code @SkipAutomationAuthorization} facade and hand an embedded connected user tenant-admin status.
     * With that mode gone the containment is no longer a property of this thread-local at all: it is
     * {@link ResourceMembershipDecider}, which answers a governed principal from its own membership and returns
     * <em>before</em> any caller reads {@link AutomationAuthorizationContext#isSkipChecks()}. That is strictly
     * stronger, because it survives an async hand-off and a {@code SecurityUtils.runAs} where a thread-local does not.
     *
     * <p>
     * This asserts the load-bearing half directly: skip mode active, and a governed principal still denied. Every
     * consumption point is a {@code decide(...) -> return on anything but NOT_GOVERNED} followed by the skip check, so
     * an implementation that read the flag first would go red here.
     */
    @Test
    void testGovernedPrincipalIsDeniedEvenUnderSkipMode() throws Throwable {
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider = mockProviderReturning(
            Decision.DENIED);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(AutomationAuthorizationContext.isSkipChecks()).isTrue();

            assertThat(
                ResourceMembershipDecider.decide(
                    resourceMembershipResolverProvider, 42L, "Project", "PROJECT_DELETE"))
                        .isEqualTo(Outcome.DENY);

            return null;
        });
    }

    /**
     * The same for a resource type the resolver has no predicate for. This is the arm that turns skip removal into real
     * denials, so it is pinned rather than left implied: a governed principal reaching an unhandled type is denied, and
     * an active skip mode does not rescue it.
     */
    @Test
    void testGovernedPrincipalOnUnhandledResourceTypeIsDeniedEvenUnderSkipMode() throws Throwable {
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider = mockProviderReturning(
            Decision.NOT_APPLICABLE);

        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(
                ResourceMembershipDecider.decide(
                    resourceMembershipResolverProvider, 3L, "Workspace", "CONNECTION_VIEW"))
                        .isEqualTo(Outcome.DENY);

            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceMembershipResolver> mockProviderReturning(Decision decision) {
        ResourceMembershipResolver resourceMembershipResolver = mock(ResourceMembershipResolver.class);

        when(resourceMembershipResolver.governsCurrentPrincipal()).thenReturn(true);
        when(resourceMembershipResolver.resolve(any(), anyString(), anyString())).thenReturn(decision);

        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider = mock(ObjectProvider.class);

        when(resourceMembershipResolverProvider.getIfAvailable()).thenReturn(resourceMembershipResolver);

        return resourceMembershipResolverProvider;
    }
}
