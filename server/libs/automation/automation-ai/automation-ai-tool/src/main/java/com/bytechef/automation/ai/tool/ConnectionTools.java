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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ConnectionTools {

    private final WorkspaceConnectionFacade workspaceConnectionFacade;

    @SuppressFBWarnings("EI")
    public ConnectionTools(WorkspaceConnectionFacade workspaceConnectionFacade) {
        this.workspaceConnectionFacade = workspaceConnectionFacade;
    }

    @Tool(
        name = "listConnections",
        description = "List the connections of a workspace in one environment, optionally only those of one component. Use an id from here for saveWorkflowTestConnection or an AI Agent MODEL element; create new connections in the ByteChef UI.")
    public List<ConnectionInfo> listConnections(
        @ToolParam(
            required = false,
            description = "Only connections of this component, e.g. slack") @Nullable String componentName,
        ToolContext toolContext) {

        AutomationToolInvocationContext context = AutomationToolInvocationContext.fromToolContext(toolContext);

        Long workspaceId = context == null ? null : context.workspaceId();

        if (workspaceId == null) {
            throw new IllegalArgumentException(
                "workspaceId is required - supply workspaceId (call listWorkspaces to find it).");
        }

        long environmentId = resolveEnvironmentId(toolContext);

        List<ConnectionDTO> connectionDTOs = workspaceConnectionFacade.getConnections(
            workspaceId, componentName, null, environmentId, null);

        return connectionDTOs.stream()
            .map(
                connectionDTO -> new ConnectionInfo(
                    connectionDTO.id(), connectionDTO.name(), connectionDTO.componentName(),
                    toEnvironmentName(connectionDTO.environmentId()), connectionDTO.active()))
            .toList();
    }

    private static long resolveEnvironmentId(ToolContext toolContext) {
        Map<String, Object> context = toolContext.getContext();

        if (context.containsKey(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY)) {
            return AutomationToolInvocationContext.resolveEnvironmentId(null, toolContext);
        }

        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        return environment == null ? Environment.DEVELOPMENT.ordinal() : environment.ordinal();
    }

    private static String toEnvironmentName(int environmentId) {
        Environment[] environments = Environment.values();

        if (environmentId < 0 || environmentId >= environments.length) {
            return String.valueOf(environmentId);
        }

        return environments[environmentId].name();
    }

    public record ConnectionInfo(
        long id, String name, String componentName, String environment, boolean active) {
    }
}
