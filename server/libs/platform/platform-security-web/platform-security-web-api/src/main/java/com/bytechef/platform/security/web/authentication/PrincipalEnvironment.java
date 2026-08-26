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

package com.bytechef.platform.security.web.authentication;

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The environment the current principal is confined to, when it is confined to one at all.
 *
 * <p>
 * An api-key principal — an embedded connected user, an embedded MCP caller — is authenticated INTO a single
 * environment: the token was minted for it and every membership it has is scoped to it. A session principal is not; an
 * ordinary platform user moves between environments, and the environment an operation runs in is genuinely theirs to
 * choose per request.
 *
 * <p>
 * That difference is what {@link #resolveEffectiveEnvironmentId(Long)} exists to express. For a confined principal a
 * caller-supplied environment id is not a choice to be validated, it is a degree of freedom that should not exist — so
 * the principal's own environment simply wins and the request parameter is inert. Validating the parameter instead
 * looks equivalent and is not: the two ends are independently defaulted client-side, they disagree in the default
 * embedded configuration (handshake seeds PRODUCTION, the workflow-builder store defaults to DEVELOPMENT), and the
 * comparison then denies a caller asking for nothing unusual. Ticket 1051 shipped that check and reverted it.
 *
 * <p>
 * Callers must resolve on the thread that holds the {@code SecurityContext} and carry the RESULT across any
 * asynchronous hand-off. Resolving inside a {@code CompletableFuture.runAsync} body reads an empty context, silently
 * falls back to the request parameter, and reinstates exactly the hole this closes.
 *
 * @author Ivica Cardic
 */
public final class PrincipalEnvironment {

    private PrincipalEnvironment() {
    }

    /**
     * The environment id carried by the current principal, or empty when it carries none — a session principal, an
     * unauthenticated thread, or an api-key token whose provider did not carry one (see
     * {@link AbstractApiKeyAuthenticationToken#fetchEnvironmentId()}).
     */
    public static Optional<Long> fetchCurrentPrincipalEnvironmentId() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (!(authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken)) {
            return Optional.empty();
        }

        return apiKeyAuthenticationToken.fetchEnvironmentId();
    }

    /**
     * The environment an operation should actually run in: the current principal's own when it has one, and otherwise
     * {@code requestedEnvironmentId} unchanged.
     *
     * <p>
     * Both the authorization gate and the execution that follows it must call this, or one environment is authorised
     * and another executed in — which is the bug, not a detail of it.
     */
    public static @Nullable Long resolveEffectiveEnvironmentId(@Nullable Long requestedEnvironmentId) {
        return fetchCurrentPrincipalEnvironmentId().orElse(requestedEnvironmentId);
    }

    /**
     * As {@link #resolveEffectiveEnvironmentId(Long)}, for a caller whose requested environment cannot be absent — a
     * primitive request field, or a parameter already validated as present. Returns a primitive so such a caller does
     * not have to carry a nullability it does not have.
     */
    public static long resolveEffectiveEnvironmentId(long requestedEnvironmentId) {
        return fetchCurrentPrincipalEnvironmentId().orElse(requestedEnvironmentId);
    }
}
