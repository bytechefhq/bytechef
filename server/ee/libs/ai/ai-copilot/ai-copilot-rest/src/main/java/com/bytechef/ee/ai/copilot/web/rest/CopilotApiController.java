/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import com.agui.server.spring.AgUiParameters;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.web.rest.facade.CopilotChatFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotApiController {

    private final CopilotChatFacade copilotChatFacade;

    @SuppressFBWarnings("EI")
    public CopilotApiController(CopilotChatFacade copilotChatFacade) {
        this.copilotChatFacade = copilotChatFacade;
    }

    @Validated
    @PostMapping(value = "/ai/chat/{agentId}")
    public SseEmitter chat(
        @NonNull @PathVariable("agentId") String agentId, @NonNull @RequestBody() AgUiParameters agUiParameters) {

        return copilotChatFacade.chat(agentId, agUiParameters);
    }
}
