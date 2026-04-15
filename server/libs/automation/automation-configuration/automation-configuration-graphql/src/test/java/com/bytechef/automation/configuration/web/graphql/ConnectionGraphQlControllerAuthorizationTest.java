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

package com.bytechef.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link ConnectionGraphQlController} mutations. A refactor that
 * accidentally drops an annotation would silently let any authenticated user perform admin-only mutations — something
 * no other test in this module catches. The tests here intentionally use reflection rather than a full Spring Security
 * integration harness, because the regression we are guarding against is specifically the removal of the annotation,
 * not the behavior of Spring Security evaluating it.
 *
 * <p>
 * Also pins the absence of {@code @PreAuthorize} on {@code demoteConnectionToPrivate} — that mutation's authorization
 * is done inside {@code WorkspaceConnectionFacadeImpl} because it must allow the connection's creator to demote their
 * own WORKSPACE connection when no admins remain (orphan-recovery path). An over-eager future refactor that added
 * {@code @PreAuthorize("hasAuthority(ADMIN)")} here would break that recovery flow.
 *
 * @author Ivica Cardic
 */
class ConnectionGraphQlControllerAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testDisconnectConnectionRequiresAdmin() throws NoSuchMethodException {
        assertAdminOnly("disconnectConnection", long.class);
    }

    @Test
    void testShareConnectionToProjectRequiresAdmin() throws NoSuchMethodException {
        assertAdminOnly("shareConnectionToProject", long.class, long.class, long.class);
    }

    @Test
    void testRevokeConnectionFromProjectRequiresAdmin() throws NoSuchMethodException {
        assertAdminOnly("revokeConnectionFromProject", long.class, long.class, long.class);
    }

    @Test
    void testPromoteConnectionToWorkspaceRequiresAdmin() throws NoSuchMethodException {
        assertAdminOnly("promoteConnectionToWorkspace", long.class, long.class);
    }

    @Test
    void testPromoteAllPrivateConnectionsToWorkspaceRequiresAdmin() throws NoSuchMethodException {
        assertAdminOnly("promoteAllPrivateConnectionsToWorkspace", long.class);
    }

    @Test
    void testSetConnectionProjectsRequiresAdmin() throws NoSuchMethodException {
        assertAdminOnly("setConnectionProjects", long.class, long.class, java.util.List.class);
    }

    @Test
    void testDemoteConnectionToPrivateIsNotAnnotatedSoFacadeCanDoAdminOrCreatorCheck() throws NoSuchMethodException {
        // Authorization lives in WorkspaceConnectionFacadeImpl.demoteToPrivate so the creator can
        // demote their own WORKSPACE connection even without admin role (orphan-recovery path).
        Method method = ConnectionGraphQlController.class.getDeclaredMethod(
            "demoteConnectionToPrivate", long.class, long.class);

        assertThat(method.getAnnotation(PreAuthorize.class))
            .as(
                "demoteConnectionToPrivate must NOT have @PreAuthorize — the facade enforces admin-OR-creator so "
                    + "connections remain recoverable when a workspace has no admins. See CLAUDE.md.")
            .isNull();
    }

    private static void assertAdminOnly(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = ConnectionGraphQlController.class.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Mutation '%s' must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\")); dropping it would silently let "
                    + "every authenticated user perform an admin-only mutation.",
                methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Mutation '%s' @PreAuthorize expression must require ROLE_ADMIN", methodName)
            .isEqualTo(ADMIN_EXPRESSION);
    }
}
