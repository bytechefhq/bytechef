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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link WorkspaceConnectionFacadeImpl} admin-only mutations. The guards
 * were moved off {@code ConnectionGraphQlController} onto the facade so they protect every caller of the facade, not
 * just the GraphQL entry point; this reflection test catches a refactor that silently drops one. That Spring Security's
 * interceptor actually enforces a {@code hasAuthority("ROLE_ADMIN")} expression at runtime is proven generically by the
 * EE {@code PreAuthorizeProxyEnforcementIntTest} — the regression guarded against here is specifically the removal of
 * the annotation.
 *
 * @author Ivica Cardic
 */
class WorkspaceConnectionFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    private static final String OWNER_OR_ADMIN_EXPRESSION =
        "@permissionService.isResourceOwner('Connection', #connectionId) || " +
            "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')";

    @Test
    void testDisconnectConnectionRequiresAdmin() {
        assertAdminOnly("disconnectConnection");
    }

    @Test
    void testRegisterExistingRequiresAdmin() {
        assertAdminOnly("registerExisting");
    }

    @Test
    void testUpdateConnectionCredentialsRequiresOwnerOrAdmin() {
        PreAuthorize preAuthorize = findMethod("updateConnectionCredentials").getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "updateConnectionCredentials must be owner-or-admin. Without it, any member holding CONNECTION_EDIT "
                    + "on a workspace-shared connection could repoint it at an account they control, and every "
                    + "workflow using that connection would silently follow.")
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method 'updateConnectionCredentials' @PreAuthorize expression must be owner-or-admin")
            .isEqualTo(OWNER_OR_ADMIN_EXPRESSION);
    }

    private static void assertAdminOnly(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method '%s' must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\")); dropping it would silently let "
                    + "every authenticated user perform an admin-only operation.",
                methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' @PreAuthorize expression must require ROLE_ADMIN", methodName)
            .isEqualTo(ADMIN_EXPRESSION);
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(WorkspaceConnectionFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on WorkspaceConnectionFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}
