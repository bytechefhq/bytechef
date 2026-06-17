/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link UserManagementFacadeImpl}. The guards were moved off
 * {@code UserGraphQlController} onto the facade so they protect every caller of the facade, not just the GraphQL entry
 * point; this reflection test catches a refactor that silently drops one. The shared
 * {@code UserService}/{@code AuthorityService}/{@code MailService}/{@code TenantService} are deliberately left
 * unguarded (non-admin self-service and SCIM flows depend on them), so the facade is the only place the admin check can
 * live.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class UserManagementFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testDeleteUserRequiresAdmin() {
        assertAdminOnly("deleteUser");
    }

    @Test
    void testInviteUserRequiresAdmin() {
        assertAdminOnly("inviteUser");
    }

    @Test
    void testFetchUserRequiresAdmin() {
        assertAdminOnly("fetchUser");
    }

    @Test
    void testGetUsersRequiresAdmin() {
        assertAdminOnly("getUsers");
    }

    @Test
    void testUpdateUserRoleRequiresAdmin() {
        assertAdminOnly("updateUserRole");
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
        List<Method> matches = Arrays.stream(UserManagementFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on UserManagementFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}
