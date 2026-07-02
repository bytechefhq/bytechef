/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
import com.bytechef.ee.ai.copilot.util.Mode;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ConnectedUserCopilotApiController {

    private final AgUiService agUiService;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final EnvironmentService environmentService;
    private final Map<String, LocalAgent> localAgentMap;

    @SuppressFBWarnings("EI")
    public ConnectedUserCopilotApiController(
        AgUiService agUiService, ConnectedUserProjectFacade connectedUserProjectFacade,
        EnvironmentService environmentService, List<LocalAgent> localAgents) {

        this.agUiService = agUiService;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.environmentService = environmentService;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }

    @PostMapping(
        value = "/automation/workflows/{workflowUuid}/copilot/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public SseEmitter copilotChat(
        @PathVariable("workflowUuid") String workflowUuid, @RequestBody AgUiParameters agUiParameters,
        @RequestHeader(value = "X-Environment", required = false) @Nullable EnvironmentModel xEnvironment) {

        String externalUserId = OptionalUtils.get(SecurityUtils.fetchCurrentUserLogin(), "User not found");
        Environment environment = getEnvironment(xEnvironment);

        CopilotChatContextDTO context =
            connectedUserProjectFacade.prepareCopilotChat(externalUserId, workflowUuid, environment);

        State state = agUiParameters.getState();
        Map<String, Object> stateMap = state.getState();

        stateMap.put("workflowId", context.workflowId());
        stateMap.put("mode", Mode.BUILD.name());
        stateMap.put("autonomous", false);
        stateMap.put(CopilotStateKeys.STATE_TENANT_ID, TenantContext.getCurrentTenantId());

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        if (authentication != null) {
            stateMap.put(CopilotStateKeys.STATE_AUTHENTICATION, authentication);
        }

        stateMap.remove(CopilotStateKeys.STATE_ADDITIONAL_SYSTEM_PROMPT);

        Object additionalSystemPromptValue = stateMap.remove("additionalSystemPrompt");

        if (additionalSystemPromptValue instanceof String additionalSystemPrompt && !additionalSystemPrompt.isBlank()) {
            String trimmed = additionalSystemPrompt.strip();

            if (trimmed.length() > CopilotStateKeys.ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH) {
                trimmed = trimmed.substring(0, CopilotStateKeys.ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH);
            }

            stateMap.put(CopilotStateKeys.STATE_ADDITIONAL_SYSTEM_PROMPT, trimmed);
        }

        Set<String> allowedComponentNames = context.allowedComponentNames();

        if (allowedComponentNames != null && !allowedComponentNames.isEmpty()) {
            stateMap.put(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
        }

        String agentId = (Source.WORKFLOW_EDITOR.name() + "_" + Mode.BUILD.name()).toLowerCase();

        LocalAgent localAgent = localAgentMap.get(agentId);

        if (localAgent == null) {
            throw new IllegalStateException("Workflow editor BUILD agent not available: " + agentId);
        }

        return agUiService.runAgent(localAgent, agUiParameters);
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private Environment getEnvironment(@Nullable EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }
}
