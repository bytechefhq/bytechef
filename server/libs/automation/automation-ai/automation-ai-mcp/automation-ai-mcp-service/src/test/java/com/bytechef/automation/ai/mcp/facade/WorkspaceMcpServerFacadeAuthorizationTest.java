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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that workspace-scope MCP server operations (T20), enforced at the facade
 * tier. Per-server delete resolves the owning workspace via {@code McpServer:ResourceRole}; list/create take a
 * {@code workspaceId} argument.
 *
 * @author Ivica Cardic
 */
class WorkspaceMcpServerFacadeAuthorizationTest {

    @Test
    void testGetWorkspaceMcpServersRequiresViewer() {
        assertExpression("getWorkspaceMcpServers", "hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')");
    }

    @Test
    void testGetWorkspaceMcpServerTagsRequiresViewer() {
        assertExpression("getWorkspaceMcpServerTags", "hasPermission(#workspaceId, 'WorkspaceRole', 'VIEWER')");
    }

    @Test
    void testCreateRequiresEditor() {
        assertExpression("createWorkspaceMcpServer", "hasPermission(#workspaceId, 'WorkspaceRole', 'EDITOR')");
    }

    @Test
    void testDeleteRequiresServerEditor() {
        assertExpression("deleteWorkspaceMcpServer", "hasPermission(#mcpServerId, 'McpServer:ResourceRole', 'EDITOR')");
    }

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : WorkspaceMcpServerFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
