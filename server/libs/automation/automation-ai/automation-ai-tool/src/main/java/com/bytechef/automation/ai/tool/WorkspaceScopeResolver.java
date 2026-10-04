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

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkspaceScopeResolver {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;
    private final WorkspaceService workspaceService;

    @SuppressFBWarnings("EI")
    public WorkspaceScopeResolver(
        UserService userService, WorkspaceFacade workspaceFacade, WorkspaceService workspaceService) {

        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
        this.workspaceService = workspaceService;
    }

    public WorkspaceScope resolve(@Nullable Long requestedWorkspaceId, @Nullable String requestedEnvironment) {
        Optional<List<Workspace>> candidateWorkspaces = getCandidateWorkspaces();

        if (candidateWorkspaces.isEmpty()) {
            return new Rejected(
                ToolErrors.toolError(jsonMapper, "No authenticated user — the workspace cannot be resolved."));
        }

        List<Workspace> workspaces = candidateWorkspaces.get();
        long workspaceId;

        if (requestedWorkspaceId != null) {
            boolean accessible = workspaces.stream()
                .anyMatch(workspace -> Objects.equals(workspace.getId(), requestedWorkspaceId));

            if (!accessible) {
                return new Rejected(
                    ToolErrors.toolError(
                        jsonMapper, "Workspace " + requestedWorkspaceId + " is not accessible to the caller."));
            }

            workspaceId = requestedWorkspaceId;
        } else if (workspaces.size() == 1) {
            Workspace workspace = workspaces.getFirst();

            workspaceId = Objects.requireNonNull(workspace.getId());
        } else {
            return new Rejected(
                jsonMapper.writeValueAsString(
                    Map.of(
                        "error", "workspace_required",
                        "message",
                        "No single workspace could be auto-selected — retry with an explicit workspaceId from the list.",
                        "workspaces", workspaces.stream()
                            .map(workspace -> Map.of("id", workspace.getId(), "name", workspace.getName()))
                            .toList())));
        }

        long environmentId = Environment.DEVELOPMENT.ordinal();

        if (requestedEnvironment != null && !requestedEnvironment.isBlank()) {
            try {
                Environment environment = Environment.valueOf(requestedEnvironment.toUpperCase(Locale.ROOT));

                environmentId = environment.ordinal();
            } catch (IllegalArgumentException exception) {
                return new Rejected(
                    ToolErrors.toolError(
                        jsonMapper,
                        "Unknown environment '" + requestedEnvironment
                            + "'. Supported: DEVELOPMENT, STAGING, PRODUCTION"));
            }
        }

        return new Resolved(workspaceId, environmentId);
    }

    private Optional<List<Workspace>> getCandidateWorkspaces() {
        if (isAnonymousManagementMcpPrincipal()) {
            return Optional.of(workspaceService.getWorkspaces());
        }

        Optional<User> currentUser = userService.fetchCurrentUser();

        return currentUser.map(user -> workspaceFacade.getUserWorkspaces(Objects.requireNonNull(user.getId())));
    }

    private static boolean isAnonymousManagementMcpPrincipal() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        Authentication authentication = securityContext.getAuthentication();

        return authentication instanceof McpAnonymousAuthenticationToken mcpAnonymousAuthenticationToken &&
            mcpAnonymousAuthenticationToken.isManagementMcpServer();
    }

    public sealed interface WorkspaceScope permits Resolved, Rejected {
    }

    public record Resolved(long workspaceId, long environmentId) implements WorkspaceScope {

        public Map<String, Object> toForwardedContext(@Nullable ToolContext toolContext) {
            Map<String, Object> forwardedContext = new HashMap<>();

            if (toolContext != null) {
                forwardedContext.putAll(toolContext.getContext());
            }

            forwardedContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId);
            forwardedContext.put(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId);
            forwardedContext.putAll(
                AgentToolInvocationContext.builder()
                    .workspaceId(workspaceId)
                    .environmentId(environmentId)
                    .build()
                    .toToolContext());

            return forwardedContext;
        }
    }

    public record Rejected(String response) implements WorkspaceScope {
    }
}
