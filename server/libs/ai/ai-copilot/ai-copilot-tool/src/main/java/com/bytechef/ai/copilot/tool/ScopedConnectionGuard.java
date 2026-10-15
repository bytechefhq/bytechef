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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnection;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnectionRequest;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Limits a confined run (one that carries a scoped project or skips automation authorization) to the connections its
 * caller is entitled to use.
 *
 * @author Ivica Cardic
 */
@Component
public class ScopedConnectionGuard {

    public static final String NOT_ENTITLED_MESSAGE = "This chat can only use connections its user is entitled to use.";
    public static final String UNVERIFIED_MESSAGE =
        "This chat could not verify that its user may use the connection. Try again later.";

    private static final Logger log = LoggerFactory.getLogger(ScopedConnectionGuard.class);

    private final ObjectProvider<CopilotConnectionLister> connectionListersProvider;

    @SuppressFBWarnings("EI")
    public ScopedConnectionGuard(ObjectProvider<CopilotConnectionLister> connectionListersProvider) {
        this.connectionListersProvider = connectionListersProvider;
    }

    public boolean isEntitled(
        @Nullable String componentName, @Nullable Long connectionId, @Nullable ToolContext toolContext) {

        return fetchDenialMessage(componentName, connectionId, toolContext) == null;
    }

    public @Nullable String toolErrorIfNotEntitled(
        @Nullable String componentName, @Nullable Long connectionId, @Nullable ToolContext toolContext) {

        String denialMessage = fetchDenialMessage(componentName, connectionId, toolContext);

        return denialMessage == null ? null : ToolErrors.toolError(denialMessage);
    }

    public @Nullable String fetchDenialMessage(
        @Nullable String componentName, @Nullable Long connectionId, @Nullable ToolContext toolContext) {

        if (connectionId == null || !ScopedProjectToolContext.isConfined(toolContext)) {
            return null;
        }

        if (ScopedProjectToolContext.fetchScopedProjectId(toolContext) == null) {
            log.warn(
                "Rejected connection {} for component {}: the run skips automation authorization but carries no "
                    + "scoped project",
                connectionId, componentName);

            return NOT_ENTITLED_MESSAGE;
        }

        AgentToolInvocationContext invocationContext = AgentToolInvocationContext.fromToolContext(toolContext);

        if (invocationContext == null) {
            log.warn(
                "Rejected connection {} for component {}: the scoped run carries no invocation context", connectionId,
                componentName);

            return UNVERIFIED_MESSAGE;
        }

        CopilotConnectionRequest request = invocationContext.toConnectionRequest(componentName, null);

        List<CopilotConnectionLister> connectionListers = connectionListersProvider.orderedStream()
            .toList();

        Optional<CopilotConnectionLister> connectionListerOptional = CopilotConnectionLister.findSupporting(
            connectionListers, request);

        if (connectionListerOptional.isEmpty()) {
            log.warn(
                "Rejected connection {} for component {}: no connection lister supports the scoped run", connectionId,
                componentName);

            return UNVERIFIED_MESSAGE;
        }

        CopilotConnectionLister connectionLister = connectionListerOptional.get();

        List<CopilotConnection> connections;

        try {
            connections = connectionLister.listConnections(request);
        } catch (RuntimeException exception) {
            log.warn(
                "Rejected connection {} for component {}: listing the caller's connections failed", connectionId,
                componentName, exception);

            return UNVERIFIED_MESSAGE;
        }

        boolean entitled = connections.stream()
            .map(CopilotConnection::id)
            .anyMatch(id -> Objects.equals(id, connectionId));

        return entitled ? null : NOT_ENTITLED_MESSAGE;
    }
}
