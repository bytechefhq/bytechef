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

import java.io.Serializable;

/**
 * SPI answering a resource-scoped {@code hasPermission(...)} check directly from the current principal's own
 * membership, rather than from the RBAC role/scope model {@link ResourceOwnershipResolver} and
 * {@link ResourceVisibilityProvider} serve. Contributed by a principal family that owns resources outright instead of
 * belonging to a workspace — today, embedded connected users.
 *
 * <p>
 * The resolver reads the caller from the {@code SecurityContext} itself, never from a method argument, so a check
 * cannot be satisfied by naming somebody else's external id. See
 * {@code com.bytechef.ee.embedded.configuration.security.ConnectedUserResourceMembershipResolver} for the one
 * implementation and {@code AutomationAuthorizationContext} for how a governed principal's answer relates to
 * {@code @SkipAutomationAuthorization}.
 *
 * <p>
 * As of ticket 1051 Stage 2, both consumption points ({@code AutomationPermissionEvaluator} and the EE
 * {@code PermissionServiceImpl.hasResourceScope}) treat this resolver as authoritative: for a principal it governs, its
 * answer decides, ahead of the skip check and of the ordinary RBAC path alike. A resolver that is absent (Community
 * Edition) or that does not govern the caller leaves both call sites on exactly the path they were on before this seam
 * existed. See {@code ResourceMembershipDecider} for the precedence rule.
 *
 * @author Ivica Cardic
 */
public interface ResourceMembershipResolver {

    /**
     * The answer to a single {@link #resolve(Serializable, String, String)} call.
     */
    enum Decision {

        /**
         * This resolver does not govern the current principal at all (an ordinary workspace user, a background thread),
         * or it governs the principal but has no predicate for {@code resourceType}.
         */
        NOT_APPLICABLE,

        /**
         * The current principal's own membership grants the check.
         */
        GRANTED,

        /**
         * The current principal is governed by this resolver but does not own {@code id}.
         */
        DENIED
    }

    /**
     * Answers a resource-scoped check for the current principal, or {@link Decision#NOT_APPLICABLE} when this resolver
     * does not govern the current principal at all.
     *
     * @param id           the resource id being checked; the {@link Serializable} key type matches whatever the
     *                     {@code @PreAuthorize} token uses for {@code resourceType} (e.g. {@code long} for
     *                     {@code 'Project'}, {@code String} for {@code 'Workflow'})
     * @param resourceType the {@code @PreAuthorize} resource-type token, e.g. {@code "Workflow"}
     * @param scope        the scope being checked, e.g. {@code "WORKFLOW_EDIT"}
     */
    Decision resolve(Serializable id, String resourceType, String scope);

    /**
     * Whether the current principal is governed by this resolver — i.e. whether a check this resolver returns
     * {@link Decision#NOT_APPLICABLE} for (because it is workspace-scoped, or an unknown type) must be denied rather
     * than handed to the ordinary path. Answering this first, and separately from
     * {@link #resolve(Serializable, String, String)}, lets a caller distinguish "not my principal" (fall through to the
     * ordinary check) from "my principal, but this particular check is unrecognised" (deny) without resolving every
     * check twice.
     */
    boolean governsCurrentPrincipal();
}
