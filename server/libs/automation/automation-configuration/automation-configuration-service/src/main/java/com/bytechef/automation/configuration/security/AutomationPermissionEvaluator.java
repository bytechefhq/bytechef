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
 * <p>
 * The current-user check uses a sentinel identifier — {@code hasPermission(#id, 'User', 'SELF')} — and the tenant-admin
 * check uses the target-less {@code hasPermission('Tenant', 'ADMIN')}.
 *
 * <p>
 * When {@link AutomationAuthorizationContext#isSkipChecks()} is active (embedded → automation delegation), every check
 * short-circuits to {@code true} without touching {@link PermissionService}.
 *
 * <p>
 * Registered as a {@code @Bean} by {@link AutomationMethodSecurityConfiguration} (an {@code @AutoConfiguration}), not
 * as a component-scanned {@code @Component}, so it is not swept into unrelated integration-test slices.
 *
 * @author Ivica Cardic
 */
public class AutomationPermissionEvaluator implements PermissionEvaluator {

    static final String ADMIN = "ADMIN";
    static final String SELF = "SELF";
    static final String TENANT = "Tenant";
    static final String USER = "User";

    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public AutomationPermissionEvaluator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        return TENANT.equals(targetDomainObject) && ADMIN.equals(String.valueOf(permission)) &&
            permissionService.isTenantAdmin();
    }

    @Override
    public boolean hasPermission(
        Authentication authentication, Serializable targetId, String targetType, Object permission) {

        if (AutomationAuthorizationContext.isSkipChecks()) {
            return true;
        }

        long id = ((Number) targetId).longValue();
        String value = String.valueOf(permission);

        return switch (targetType) {
            case USER -> SELF.equals(value) && permissionService.isCurrentUser(id);
            default -> false;
        };
    }
}
