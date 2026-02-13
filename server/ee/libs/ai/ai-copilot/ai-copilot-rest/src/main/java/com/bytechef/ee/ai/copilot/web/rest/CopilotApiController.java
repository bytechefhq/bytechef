/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotApiController {

    private final Map<String, LocalAgent> localAgentMap;
    private final AgUiService agUiService;

    @SuppressFBWarnings("EI")
    public CopilotApiController(AgUiService agUiService, List<LocalAgent> localAgents) {
        this.agUiService = agUiService;
        this.localAgentMap = localAgents.stream()
            .collect(Collectors.toMap(LocalAgent::getAgentId, localAgent -> localAgent));
    }

    @Validated
    @PostMapping(value = "/ai/chat/{agentId}")
    public SseEmitter chat(
        @NonNull @PathVariable("agentId") String agentId, @NonNull @RequestBody() AgUiParameters agUiParameters) {

        if(agentId.equals("workflow_editor")) {
            switch ((String) agUiParameters.getState().getState().get("mode")) {
                case "BUILD" -> agentId = "workflow_editor_build";
                default -> agentId = "workflow_editor_ask";
            }
        }

        LocalAgent localAgent = localAgentMap.get(agentId);

        return this.agUiService.runAgent(localAgent, agUiParameters);
    }
}
