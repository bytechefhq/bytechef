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
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Implementation of {@link CopilotChatFacade}. Carries the workflow-scope guard that used to sit in
 * {@code CopilotApiController.chat}'s body, so it is enforced for every caller of the facade rather than only the REST
 * entry point.
 *
 * <p>
 * The guard cannot be a single {@code @PreAuthorize} string: the required scope is chosen per run from the run's mode,
 * and collapsing the two to the weaker {@code WORKFLOW_VIEW} so one annotation fits would let a viewer drive a BUILD
 * turn — a privilege escalation, not a tidy-up.
 *
 * <p>
 * The optional {@code PermissionService} / {@code ProjectWorkflowService} / {@code UserService} dependencies are
 * carried over unchanged from the controller, along with what absence means for each: the two authorization services
 * fail the run closed, while an absent {@code UserService} only skips the user-id injection.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
class CopilotChatFacadeImpl implements CopilotChatFacade {

    private final Map<String, LocalAgent> localAgentMap;
    private final AgUiService agUiService;
    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    CopilotChatFacadeImpl(
        AgUiService agUiService, List<LocalAgent> localAgents, Optional<PermissionService> permissionService,
        Optional<ProjectWorkflowService> projectWorkflowService, Optional<UserService> userService) {

        this.agUiService = agUiService;
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
        Object mode = stateMap.get("mode");

        authorizeWorkflowAccess(stateMap, mode);

        injectAuthenticatedUserId(stateMap);
        stateMap.put(CopilotConstants.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        String resolvedAgentId;

        if (agentId.equals("converter")) {
            resolvedAgentId = "converter_build";
        } else {
            resolvedAgentId = agentId + "_" + Mode.valueOf((String) mode)
                .name()
                .toLowerCase();
        }

        LocalAgent localAgent = localAgentMap.get(resolvedAgentId);

        if (localAgent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown agentId: " + resolvedAgentId);
        }

        return agUiService.runAgent(localAgent, agUiParameters);
    }

    /**
     * Injects the authenticated user id into the run state, resolved server-side from the request security context, so
     * the agent's tool context can carry it to the shared connection/property picker tools (which rehydrate the user's
     * security context from it). Never trusts a client-supplied user id. A no-op when the user cannot be resolved or
     * the user service is not wired in the running app variant — the pickers then simply report missing context rather
     * than acting as the wrong principal.
     */
    private void injectAuthenticatedUserId(Map<String, Object> stateMap) {
        if (userService == null) {
            return;
        }

        SecurityUtils.fetchCurrentUserLogin()
            .flatMap(userService::fetchUserByLogin)
            .map(User::getId)
            .ifPresent(userId -> stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, userId));
    }

    /**
     * Authorizes the client-supplied {@code workflowId} carried in the request state before any agent reads or mutates
     * that workflow's data, preventing cross-tenant access (IDOR). BUILD turns mutate the workflow and require the
     * WORKFLOW_EDIT scope; other turns only read and require WORKFLOW_VIEW. Fails closed when the authorization
     * services are not wired in the running app variant.
     */
    private void authorizeWorkflowAccess(Map<String, Object> stateMap, Object mode) {
        if (!(stateMap.get("workflowId") instanceof String workflowId) || workflowId.isBlank()) {
            return;
        }

        if (permissionService == null || projectWorkflowService == null) {
            throw new AccessDeniedException("Workflow authorization is not available");
        }

        long projectId = projectWorkflowService.getWorkflowProjectWorkflow(workflowId)
            .getProjectId();

        boolean build = mode instanceof String modeValue && Mode.valueOf(modeValue) == Mode.BUILD;

        if (!permissionService.hasWorkspaceScopeForProject(projectId, build ? "WORKFLOW_EDIT" : "WORKFLOW_VIEW")) {
            throw new AccessDeniedException("Access denied to workflow " + workflowId);
        }
    }
}
