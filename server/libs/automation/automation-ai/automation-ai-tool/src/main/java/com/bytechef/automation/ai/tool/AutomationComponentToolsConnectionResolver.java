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

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.platform.ai.tool.ComponentToolsConnectionResolver;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/**
 * Resolves only a connection of the workspace, and of the environment when one is given, that the tool context names.
 * Without a workspace in the tool context there is no connection, so a caller never runs an action with a connection of
 * another workspace.
 *
 * @author Ivica Cardic
 */
@Component
class AutomationComponentToolsConnectionResolver implements ComponentToolsConnectionResolver {

    private final ConnectionService connectionService;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings("EI")
    AutomationComponentToolsConnectionResolver(
        ConnectionService connectionService, WorkspaceConnectionService workspaceConnectionService) {

        this.connectionService = connectionService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    public Optional<Long> resolveConnectionId(
        String componentName, int componentVersion, @Nullable ToolContext toolContext) {

        AutomationToolInvocationContext automationToolInvocationContext =
            AutomationToolInvocationContext.fromToolContext(toolContext);

        if (automationToolInvocationContext == null) {
            return Optional.empty();
        }

        Long workspaceId = automationToolInvocationContext.workspaceId();

        if (workspaceId == null) {
            return Optional.empty();
        }

        Set<Long> workspaceConnectionIds = workspaceConnectionService.getWorkspaceConnections(workspaceId)
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .collect(Collectors.toSet());

        Long environmentId = automationToolInvocationContext.environmentId();

        return connectionService.getConnections(componentName, componentVersion, PlatformType.AUTOMATION)
            .stream()
            .filter(connection -> workspaceConnectionIds.contains(connection.getId()))
            .filter(connection -> environmentId == null || connection.getEnvironmentId() == environmentId)
            .map(Connection::getId)
            .findFirst();
    }
}
