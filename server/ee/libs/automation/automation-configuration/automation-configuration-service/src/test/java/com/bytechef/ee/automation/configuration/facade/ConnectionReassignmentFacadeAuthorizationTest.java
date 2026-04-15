/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link ConnectionReassignmentFacadeImpl}. The admin-only guards were
 * moved off {@code ConnectionReassignmentGraphQlController} onto the facade so they protect every caller; this
 * reflection test catches a refactor that silently drops one.
 *
 * <p>
 * It also pins the <em>absence</em> of {@code @PreAuthorize} on {@code markConnectionsPendingReassignment} — that is
 * the unguarded internal variant invoked by {@code WorkspaceUserRemovalListener} on the non-admin system user-removal
 * path. The GraphQL admin entry uses the guarded {@code markConnectionsPendingReassignmentAsAdmin}. Adding a guard to
 * the internal variant would block the listener. Runtime enforcement is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionReassignmentFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testAdminGuardedMethods() {
        for (String methodName : List.of(
            "getUnresolvedConnections", "getAffectedWorkflows", "reassignConnection", "reassignAllConnections",
            "markConnectionsPendingReassignmentAsAdmin")) {

            PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize)
                .as(
                    "Method '%s' must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\")); dropping it would silently "
                        + "let every authenticated user perform an admin-only reassignment operation.",
                    methodName)
                .isNotNull();

            assertThat(preAuthorize.value())
                .as("Method '%s' @PreAuthorize expression must require ROLE_ADMIN", methodName)
                .isEqualTo(ADMIN_EXPRESSION);
        }
    }

    @Test
    void testInternalMarkVariantIsNotAnnotatedSoUserRemovalListenerIsNotBlocked() {
        assertThat(findMethod("markConnectionsPendingReassignment").getAnnotation(PreAuthorize.class))
            .as(
                "markConnectionsPendingReassignment must NOT have @PreAuthorize — it is the internal variant called by "
                    + "WorkspaceUserRemovalListener on the non-admin user-removal path. The admin entry is "
                    + "markConnectionsPendingReassignmentAsAdmin.")
            .isNull();
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(ConnectionReassignmentFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on ConnectionReassignmentFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}
