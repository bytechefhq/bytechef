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
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expression that closes {@code WorkspaceApiController.getUserWorkspaces} IDOR (T25):
 * passing another user's id enumerated their workspace memberships. The {@code 'User'/'SELF'} token restricts the call
 * to the caller's own user id (tenant admins may pass any id); it matches the EE variant exactly. In CE
 * {@code isCurrentUser} is permissive (single-workspace model), so the gate is a no-op there but kept for consistency.
 * The internal membership-check callers (AI Hub / asset-file / auto-memory) all pass
 * {@code userService.getCurrentUser().getId()}, so they satisfy SELF.
 *
 * @author Ivica Cardic
 */
class WorkspaceFacadeAuthorizationTest {

    @Test
    void testGetUserWorkspacesRequiresSelfOrTenantAdmin() {
        Method method;

        try {
            method = WorkspaceFacadeImpl.class.getDeclaredMethod("getUserWorkspaces", long.class);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method getUserWorkspaces not found", exception);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on getUserWorkspaces")
            .isNotNull();
        assertThat(preAuthorize.value())
            .isEqualTo("hasPermission('Tenant', 'ADMIN') or hasPermission(#id, 'User', 'SELF')");
    }
}
