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

import com.bytechef.automation.configuration.security.ResourceMembershipResolver.Decision;
import java.io.Serializable;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The single implementation of ticket 1051's precedence rule, shared by both consumption points
 * ({@code AutomationPermissionEvaluator} and the EE {@code PermissionServiceImpl.hasResourceScope}) so the two cannot
 * drift on an ordering that is load-bearing and easy to get backwards:
 *
 * <ol>
 * <li>resolver absent (Community Edition) &rarr; {@link Outcome#NOT_GOVERNED}, the ordinary check decides</li>
 * <li>{@code !governsCurrentPrincipal()} &rarr; {@link Outcome#NOT_GOVERNED}, the ordinary check decides (admin
 * console, background threads, every non-connected-user principal)</li>
 * <li>{@link Decision#GRANTED} &rarr; {@link Outcome#GRANT}</li>
 * <li>{@link Decision#DENIED} &rarr; {@link Outcome#DENY}</li>
 * <li>{@link Decision#NOT_APPLICABLE} &rarr; {@link Outcome#DENY} (governed principal, ungoverned kind of check)</li>
 * </ol>
 *
 * <p>
 * Deliberately absent from that list: {@link AutomationAuthorizationContext#isSkipChecks}. Once a principal is
 * governed, {@code @SkipAutomationAuthorization} grants it nothing — which is the whole point, and is strictly stronger
 * than a thread-local skip mode because it does not depend on that thread-local surviving the call stack. Callers must
 * therefore consult this decider BEFORE their skip check, and return on anything other than
 * {@link Outcome#NOT_GOVERNED}.
 *
 * <p>
 * Rules 1 and 2 are what keep Community Edition and every non-connected-user principal on exactly the path they were on
 * before this seam existed, and they do it per principal rather than per deployment.
 *
 * <p>
 * Rule 2 reached by an api-key principal means the seam failed to recognise a caller it was built for. That used to be
 * masked by the embedded skip filter and is now a 403, so it is instrumented — see
 * {@link ResourceMembershipGovernanceGapLog}.
 *
 * @author Ivica Cardic
 */
public final class ResourceMembershipDecider {

    /**
     * What the caller must do with a single resource-scoped check.
     */
    public enum Outcome {

        /**
         * This seam does not govern the current principal; the caller's ordinary path decides, skip check included.
         */
        NOT_GOVERNED,

        /**
         * The current principal's own membership grants the check; the caller must return granted immediately.
         */
        GRANT,

        /**
         * The current principal is governed and its membership does not cover the resource; the caller must return
         * denied immediately, whatever skip mode is active.
         */
        DENY
    }

    private ResourceMembershipDecider() {
    }

    /**
     * Applies the precedence rule above and logs a denial via {@link ResourceMembershipDenialLog} on the way out. The
     * resolver is only resolved once per call, and {@link ResourceMembershipResolver#governsCurrentPrincipal()} is
     * asked first so an ordinary request — the overwhelming majority of traffic — costs nothing beyond that one cheap
     * check.
     */
    public static Outcome decide(
        ObjectProvider<ResourceMembershipResolver> resourceMembershipResolverProvider, Serializable id,
        String resourceType, String scope) {

        ResourceMembershipResolver resourceMembershipResolver = resourceMembershipResolverProvider.getIfAvailable();

        if (resourceMembershipResolver == null) {
            return Outcome.NOT_GOVERNED;
        }

        if (!resourceMembershipResolver.governsCurrentPrincipal()) {
            // Ticket 1051: an api-key principal reaching here is the seam failing to recognise a caller it was built
            // for. This was the last case EmbeddedAutomationAuthorizationSkipFilter was load-bearing for, and deleting
            // that filter in Stage 4 turned it from a silent grant into a 403 -- which is why it is worth a WARN.
            // Deliberately not logged for the branch above: an absent resolver is Community Edition, where
            // NOT_GOVERNED is the correct and universal answer.
            ResourceMembershipGovernanceGapLog.logIfGovernanceExpected(id, resourceType, scope);

            return Outcome.NOT_GOVERNED;
        }

        Decision decision = resourceMembershipResolver.resolve(id, resourceType, scope);

        ResourceMembershipDenialLog.logIfNoteworthy(decision, id, resourceType, scope);

        return decision == Decision.GRANTED ? Outcome.GRANT : Outcome.DENY;
    }
}
