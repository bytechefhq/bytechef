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

/**
 * Thread-local flag marking that the current synchronous call stack is a delegation during which automation RBAC checks
 * are bypassed. Entered only via {@link #callSkippingChecks(SkippableCall)}, which is used where an equivalent boundary
 * has already been verified, or where the caller is not principal-driven at all.
 *
 * <p>
 * Its one production arming site is {@code SkipAutomationAuthorizationAspect}, and after ticket 1051 that aspect arms
 * nothing for a principal {@link ResourceMembershipResolver} governs — so this flag now governs only non-connected-user
 * principals.
 *
 * <p>
 * <b>Why there is no narrower mode any more.</b> A second, restricted mode used to exist
 * ({@code SkipMode.RESOURCE_SCOPED_ONLY}) that bypassed only resource-scoped checks, together with a {@code CheckKind}
 * classification and a monotonic-narrowing rule that stopped a nested full skip from widening it. All three existed to
 * contain a thread-local grant handed to embedded connected users — first by
 * {@code EmbeddedAutomationAuthorizationSkipFilter} on every embedded request, then by the two copilot hand-offs that
 * outlived it. {@link ResourceMembershipDecider} replaced that containment with a stronger one: a governed principal is
 * answered from its own membership <em>ahead of</em> every read of this flag, so once the copilot's worker threads
 * gained a bound {@code TenantContext} — and with it a resolver that recognises the connected user on those threads —
 * the restricted mode had no reachable decision left to make. It was removed rather than left in place, because a skip
 * mode nothing consults is a grant waiting for a future caller to rediscover.
 *
 * <p>
 * The rule that replaced the narrowing rule is {@link ResourceMembershipDecider}'s precedence list, and it is not
 * thread-local at all: it is a property of the principal, so it survives a {@code SecurityUtils.runAs} and an async
 * hand-off that this flag does not.
 *
 * @author Ivica Cardic
 */
public final class AutomationAuthorizationContext {

    private static final ThreadLocal<Boolean> SKIP_CHECKS = new ThreadLocal<>();

    private AutomationAuthorizationContext() {
    }

    /**
     * Whether every automation RBAC check on this thread is currently bypassed.
     *
     * <p>
     * Callers that guard a resource-scoped check with this MUST consult {@link ResourceMembershipDecider} first and
     * return on anything other than {@code NOT_GOVERNED} — a governed principal is never granted anything by this flag.
     */
    public static boolean isSkipChecks() {
        return Boolean.TRUE.equals(SKIP_CHECKS.get());
    }

    /**
     * Runs {@code call} under skip mode: every automation RBAC check is bypassed for its synchronous duration.
     */
    public static <V> V callSkippingChecks(SkippableCall<V> call) throws Throwable {
        boolean previous = isSkipChecks();

        SKIP_CHECKS.set(Boolean.TRUE);

        try {
            return call.call();
        } finally {
            if (previous) {
                SKIP_CHECKS.set(Boolean.TRUE);
            } else {
                SKIP_CHECKS.remove();
            }
        }
    }

    @FunctionalInterface
    public interface SkippableCall<V> {

        V call() throws Throwable;
    }
}
