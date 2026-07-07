/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

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
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotApiController {

    private final Map<String, LocalAgent> localAgentMap;
    private final AgUiService agUiService;
    private final PermissionService permissionService;
    private final ProjectWorkflowService projectWorkflowService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public CopilotApiController(
        AgUiService agUiService, List<LocalAgent> localAgents, Optional<PermissionService> permissionService,
        Optional<ProjectWorkflowService> projectWorkflowService, Optional<UserService> userService) {

        this.agUiService = agUiService;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
        this.permissionService = permissionService.orElse(null);
        this.projectWorkflowService = projectWorkflowService.orElse(null);
        this.userService = userService.orElse(null);
    }

    @Validated
    @PostMapping(value = "/ai/chat/{agentId}")
    public SseEmitter chat(
        @NonNull @PathVariable("agentId") String agentId, @NonNull @RequestBody() AgUiParameters agUiParameters) {

        State state = agUiParameters.getState();
        Map<String, Object> stateMap = state.getState();
        Object mode = stateMap.get("mode");

        authorizeWorkflowAccess(stateMap, mode);

        injectAuthenticatedUserId(stateMap);
        stateMap.put(CopilotConstants.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        if (agentId.equals("workflow_editor")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "workflow_editor_build";
            } else {
                agentId = "workflow_editor_ask";
            }
        } else if (agentId.equals("workflow_execution")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "workflow_execution_build";
            } else {
                agentId = "workflow_execution_ask";
            }
        } else if (agentId.equals("code_editor")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "code_editor_build";
            } else {
                agentId = "code_editor_ask";
            }
        } else if (agentId.equals("workflow_code_editor")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "workflow_code_editor_build";
            } else {
                agentId = "workflow_code_editor_ask";
            }
        } else if (agentId.equals("cluster_element")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "cluster_element_build";
            } else {
                agentId = "cluster_element_ask";
            }
        } else if (agentId.equals("converter")) {
            agentId = "converter_build";
        } else if (agentId.equals("skills")) {
            if (Mode.valueOf((String) mode) == Mode.BUILD) {
                agentId = "skills_build";
            } else {
                agentId = "skills_ask";
            }
        }

        LocalAgent localAgent = localAgentMap.get(agentId);

        return this.agUiService.runAgent(localAgent, agUiParameters);
    }

    private void injectAuthenticatedUserId(Map<String, Object> stateMap) {
        if (userService == null) {
            return;
        }

        SecurityUtils.fetchCurrentUserLogin()
            .flatMap(userService::fetchUserByLogin)
            .map(User::getId)
            .ifPresent(userId -> stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, userId));
    }

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
