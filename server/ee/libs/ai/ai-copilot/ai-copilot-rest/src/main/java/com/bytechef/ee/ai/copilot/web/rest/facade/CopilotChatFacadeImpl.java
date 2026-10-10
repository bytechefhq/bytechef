/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest.facade;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.util.Mode;
import com.bytechef.ai.copilot.util.Source;
import com.bytechef.ai.copilot.util.Source.WorkflowOwner;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
class CopilotChatFacadeImpl implements CopilotChatFacade {

    private static final Logger log = LoggerFactory.getLogger(CopilotChatFacadeImpl.class);

    private final Map<String, LocalAgent> localAgentMap;
    private final AgUiService agUiService;
    private final IntegrationWorkflowFacade integrationWorkflowFacade;
    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    CopilotChatFacadeImpl(
        AgUiService agUiService, List<LocalAgent> localAgents, Optional<PermissionService> permissionService,
        Optional<ProjectWorkflowService> projectWorkflowService, Optional<UserService> userService,
        Optional<IntegrationWorkflowFacade> integrationWorkflowFacade) {

        this.agUiService = agUiService;
        this.integrationWorkflowFacade = integrationWorkflowFacade.orElse(null);
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
        this.permissionService = permissionService.orElse(null);
        this.projectWorkflowService = projectWorkflowService.orElse(null);
        this.userService = userService.orElse(null);
    }

    @Override
    public SseEmitter chat(String agentId, AgUiParameters agUiParameters) {
        State state = agUiParameters.getState();
        Map<String, Object> stateMap = state.getState();

        Source source = Source.fromId(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown agentId: " + agentId));

        Mode mode = resolveMode(source, parseMode(stateMap.get("mode")));

        String resolvedAgentId = source.agentId(mode);

        LocalAgent localAgent = localAgentMap.get(resolvedAgentId);

        if (localAgent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown agentId: " + resolvedAgentId);
        }

        boolean build = mode == Mode.BUILD;

        if (source.workflowOwner() == WorkflowOwner.INTEGRATION) {
            authorizeIntegrationWorkflowAccess(stateMap);
        } else {
            authorizeWorkflowAccess(stateMap, build);
        }

        authorizeWorkspaceAccess(stateMap, build);

        stateMap.remove(CopilotConstants.STATE_AUTHENTICATED_USER_ID);

        injectAuthenticatedUserId(stateMap);
        stateMap.put(CopilotConstants.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        return agUiService.runAgent(localAgent, agUiParameters);
    }

    private static Mode resolveMode(Source source, @Nullable Mode mode) {
        try {
            return source.resolveMode(mode);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The run state has no mode", exception);
        }
    }

    private void injectAuthenticatedUserId(Map<String, Object> stateMap) {
        if (userService == null) {
            return;
        }

        Optional<String> currentUserLogin = SecurityUtils.fetchCurrentUserLogin();

        Optional<Long> userId = currentUserLogin.flatMap(userService::fetchUserByLogin)
            .map(User::getId);

        if (userId.isEmpty()) {
            log.warn(
                "No user resolved for login '{}'; the Copilot run state carries no authenticated user id",
                currentUserLogin.orElse(null));

            return;
        }

        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, userId.get());
    }

    private void authorizeIntegrationWorkflowAccess(Map<String, Object> stateMap) {
        if (!(stateMap.get("workflowId") instanceof String workflowId) || workflowId.isBlank()) {
            return;
        }

        if (integrationWorkflowFacade == null) {
            throw new AccessDeniedException("Workflow authorization is not available");
        }

        try {
            integrationWorkflowFacade.getIntegrationWorkflow(workflowId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Access denied to workflow " + workflowId, exception);
        }
    }

    private void authorizeWorkflowAccess(Map<String, Object> stateMap, boolean build) {
        if (!(stateMap.get("workflowId") instanceof String workflowId) || workflowId.isBlank()) {
            return;
        }

        if (permissionService == null || projectWorkflowService == null) {
            throw new AccessDeniedException("Workflow authorization is not available");
        }

        ProjectWorkflow projectWorkflow;

        try {
            projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Access denied to workflow " + workflowId, exception);
        }

        long projectId = projectWorkflow.getProjectId();

        if (!permissionService.hasWorkspaceScopeForProject(projectId, build ? "WORKFLOW_EDIT" : "WORKFLOW_VIEW")) {
            throw new AccessDeniedException("Access denied to workflow " + workflowId);
        }
    }

    private void authorizeWorkspaceAccess(Map<String, Object> stateMap, boolean build) {
        Object workspaceIdValue = stateMap.get(CopilotConstants.STATE_WORKSPACE_ID);

        if (workspaceIdValue == null) {
            return;
        }

        long workspaceId = parseWorkspaceId(workspaceIdValue);

        if (permissionService == null) {
            throw new AccessDeniedException("Workspace authorization is not available");
        }

        if (!permissionService.hasWorkspaceRole(workspaceId, build ? "EDITOR" : "VIEWER")) {
            throw new AccessDeniedException("Access denied to workspace " + workspaceId);
        }
    }

    private static long parseWorkspaceId(Object workspaceIdValue) {
        if (workspaceIdValue instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(workspaceIdValue));
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Invalid workspace id", exception);
        }
    }

    private static @Nullable Mode parseMode(@Nullable Object mode) {
        if (mode == null) {
            return null;
        }

        try {
            return Mode.valueOf(String.valueOf(mode));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown mode: " + mode);
        }
    }
}
