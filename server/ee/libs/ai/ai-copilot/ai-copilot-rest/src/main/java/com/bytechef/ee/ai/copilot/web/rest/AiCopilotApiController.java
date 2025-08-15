/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.AiCopilot;
import com.bytechef.ee.ai.copilot.dto.ContextDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotApiController {

    private final AiCopilot aiCopilot;

    public AiCopilotApiController(AiCopilot aiCopilot) {
        this.aiCopilot = aiCopilot;
    }

    @PostMapping(value = "/ai/chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, ?>> chat(
        @RequestBody Request request, @RequestHeader("X-Copilot-Conversation-Id") String conversationId) {

        return aiCopilot.chat(request.message, request.context, conversationId);
    }

    @SuppressFBWarnings("EI")
    public record Request(String message, ContextDTO context) {
    }

}
