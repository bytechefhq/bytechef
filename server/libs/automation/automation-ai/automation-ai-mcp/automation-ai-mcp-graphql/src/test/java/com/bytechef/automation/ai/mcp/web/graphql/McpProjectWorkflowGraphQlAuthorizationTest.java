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

package com.bytechef.automation.ai.mcp.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins authorization for the MCP-project-workflow GraphQL surface.
 * <p>
 * The guard sits on the controller rather than on {@code McpProjectWorkflowService} because the service's list reads
 * are also called by trusted internal paths that run without a usable {@code SecurityContext} - the MCP serve path, the
 * delete-cascade listeners, and an agent tool callback on a worker thread - which a service-level guard would reject.
 * This mirrors the API-facade-owns-authorization convention used where a shared service is reachable from both HTTP and
 * runtime agent tools.
 *
 * @author Ivica Cardic
 */
class McpProjectWorkflowGraphQlAuthorizationTest {

    @Test
    void testByMcpProjectIdRequiresProjectViewer() {
        Method method = declaredMethod("mcpProjectWorkflowsByMcpProjectId", long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on mcpProjectWorkflowsByMcpProjectId")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission(#mcpProjectId, 'McpProject', 'MCP_VIEW')");
        assertThat(Modifier.isPublic(method.getModifiers()))
            .as("proxy-based method security is only guaranteed to intercept public methods, so narrowing this back "
                + "to package-private would disable the guard without failing anything else")
            .isTrue();
    }

    /**
     * Unlike the two removed queries, this one HAS a consumer — the MCP and A2A "add workflow" dialogs — so it had to
     * be guarded rather than deleted. It is keyed on a bare {@code projectId}, so without the guard any caller could
     * enumerate any project's workflow ids and labels by guessing a number.
     */
    @Test
    void testToolEligibleProjectVersionWorkflowsRequiresProjectWorkflowViewer() {
        Method method = declaredMethod("toolEligibleProjectVersionWorkflows", long.class, int.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on toolEligibleProjectVersionWorkflows")
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')");
        assertThat(Modifier.isPublic(method.getModifiers()))
            .as("proxy-based method security is only guaranteed to intercept public methods")
            .isTrue();
    }

    /**
     * Both were unguarded root queries with no consumer anywhere - not the client, not the server, not the docs. The
     * no-argument one returned every {@code mcp_project_workflow} row and could not be guarded by id at all; the other
     * keyed on a {@code projectDeploymentWorkflowId}, for which no ownership resolver exists, so no
     * {@code hasPermission} expression could be written for it either. Removing them was the only fix that actually
     * closed the exposure rather than checking it.
     */
    @Test
    void testUnguardableRootQueriesAreNotExposed() {
        assertThat(McpProjectWorkflowGraphQlController.class.getDeclaredMethods())
            .noneMatch(
                method -> method.isAnnotationPresent(QueryMapping.class) &&
                    (method.getName()
                        .equals("mcpProjectWorkflows") ||
                        method.getName()
                            .equals("mcpProjectWorkflowsByProjectDeploymentWorkflowId")));
    }

    private static Method declaredMethod(String name, Class<?>... parameterTypes) {
        Method match = Arrays.stream(McpProjectWorkflowGraphQlController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName()
                .equals(name))
            .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameterTypes))
            .findFirst()
            .orElse(null);

        assertThat(match)
            .as("method %s%s", name, Arrays.toString(parameterTypes))
            .isNotNull();

        return match;
    }
}
