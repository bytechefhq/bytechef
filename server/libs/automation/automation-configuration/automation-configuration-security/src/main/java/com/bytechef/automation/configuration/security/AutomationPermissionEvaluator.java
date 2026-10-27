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

import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * Adapts the automation {@link PermissionService} to Spring Security's {@link PermissionEvaluator} so that
 * authorization is expressed as the standard {@code hasPermission(...)} SpEL built-in. The evaluator is deliberately
 * thin: it routes on {@code targetType} and delegates; all RBAC logic and CE/EE conditioning live in
 * {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
public class AutomationPermissionEvaluator implements PermissionEvaluator {

    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public AutomationPermissionEvaluator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // The two-argument hasPermission(target, permission) form carries no resource type and is not used by any
        // @PreAuthorize gate (every gate passes a type via the three-argument overload); fail closed.
        return false;
    }

    @Override
    public boolean hasPermission(
        Authentication authentication, Serializable targetId, String targetType, Object permission) {

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return permissionService.hasResourceScope(targetId, targetType, String.valueOf(permission));
    }
}
