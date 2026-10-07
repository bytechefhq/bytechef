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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
public final class PrincipalEnvironment {
    private PrincipalEnvironment() {
    }

    public static Optional<Long> fetchCurrentPrincipalEnvironmentId() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        if (!(authentication instanceof AbstractApiKeyAuthenticationToken apiKeyAuthenticationToken)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            apiKeyAuthenticationToken.getEnvironmentId() == -1 ? null : apiKeyAuthenticationToken.getEnvironmentId());
    }

    public static long resolveEffectiveEnvironmentId(long requestedEnvironmentId) {
        return fetchCurrentPrincipalEnvironmentId().orElse(requestedEnvironmentId);
    }
}
