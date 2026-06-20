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

package com.bytechef.platform.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close MCP-server IDOR (T20) at the service/facade tier. The per-id
 * {@code McpServer:ResourceRole} token resolves the owning workspace via the automation-tier resolver; the
 * {@code secretKey}-keyed {@code getMcpServer(String)} variant used by the MCP runtime is intentionally not gated.
 *
 * @author Ivica Cardic
 */
class McpServerAuthorizationTest {

    @Test
    void testGetMcpServerRequiresViewer() {
        assertExpression(
            McpServerServiceImpl.class, "getMcpServer",
            "hasPermission(#mcpServerId, 'McpServer:ResourceRole', 'VIEWER')",
            long.class);
    }

    @Test
    void testGetSecretKeyRequiresTenantAdmin() {
        assertExpression(
            McpServerServiceImpl.class, "getMcpServerSecretKey", "hasPermission('Tenant', 'ADMIN')", long.class);
    }

    @Test
    void testUpdateRequiresEditor() {
        assertExpression(
            McpServerServiceImpl.class, "update", "hasPermission(#id, 'McpServer:ResourceRole', 'EDITOR')",
            long.class, String.class, Boolean.class);
    }

    @Test
    void testDeleteMcpServerRequiresEditor() {
        assertExpression(
            McpServerFacadeImpl.class, "deleteMcpServer",
            "hasPermission(#mcpServerId, 'McpServer:ResourceRole', 'EDITOR')", long.class);
    }

    @Test
    void testUpdateMcpServerTagsRequiresEditor() {
        assertExpression(
            McpServerFacadeImpl.class, "updateMcpServerTags",
            "hasPermission(#id, 'McpServer:ResourceRole', 'EDITOR')", long.class, java.util.List.class);
    }

    @Test
    void testCreateMcpComponentRequiresServerEditor() {
        assertExpression(
            McpServerFacadeImpl.class, "create",
            "hasPermission(#mcpComponent.mcpServerId, 'McpServer:ResourceRole', 'EDITOR')",
            McpComponent.class, java.util.List.class);
    }

    @Test
    void testUpdateMcpComponentRequiresEditor() {
        assertExpression(
            McpServerFacadeImpl.class, "update",
            "hasPermission(#mcpComponent.id, 'McpComponent:ResourceRole', 'EDITOR')",
            McpComponent.class, java.util.List.class);
    }

    @Test
    void testDeleteMcpComponentRequiresEditor() {
        assertExpression(
            McpServerFacadeImpl.class, "deleteMcpComponent",
            "hasPermission(#mcpComponentId, 'McpComponent:ResourceRole', 'EDITOR')", long.class);
    }

    @Test
    void testCreateMcpComponentServiceRequiresServerEditor() {
        assertExpression(
            McpComponentServiceImpl.class, "create",
            "hasPermission(#mcpComponent.mcpServerId, 'McpServer:ResourceRole', 'EDITOR')", McpComponent.class);
    }

    @Test
    void testGetMcpComponentsRequiresTenantAdmin() {
        assertExpression(McpComponentServiceImpl.class, "getMcpComponents", "hasPermission('Tenant', 'ADMIN')");
    }

    @Test
    void testCreateMcpToolRequiresComponentEditor() {
        assertExpression(
            McpToolServiceImpl.class, "create",
            "hasPermission(#mcpTool.mcpComponentId, 'McpComponent:ResourceRole', 'EDITOR')", McpTool.class);
    }

    @Test
    void testFetchMcpToolRequiresViewer() {
        assertExpression(
            McpToolServiceImpl.class, "fetchMcpTool", "hasPermission(#mcpToolId, 'McpTool:ResourceRole', 'VIEWER')",
            long.class);
    }

    @Test
    void testGetMcpToolsRequiresTenantAdmin() {
        assertExpression(McpToolServiceImpl.class, "getMcpTools", "hasPermission('Tenant', 'ADMIN')");
    }

    @Test
    void testUpdateMcpToolRequiresEditor() {
        assertExpression(
            McpToolServiceImpl.class, "update", "hasPermission(#mcpTool.id, 'McpTool:ResourceRole', 'EDITOR')",
            McpTool.class);
    }

    @Test
    void testDeleteMcpToolRequiresEditor() {
        assertExpression(
            McpToolServiceImpl.class, "delete", "hasPermission(#mcpTool.id, 'McpTool:ResourceRole', 'EDITOR')",
            McpTool.class);
    }

    private static void assertExpression(
        Class<?> clazz, String methodName, String expression, Class<?>... parameterTypes) {

        Method method;

        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
