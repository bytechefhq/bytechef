/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.remote.web.rest.facade;

import com.bytechef.ee.ai.copilot.dto.ContextDTO;
import com.bytechef.ee.ai.copilot.facade.AiCopilotFacade;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/ai-copilot")
public class RemoteAiCopilotFacadeController {

    private final AiCopilotFacade aiCopilotFacade;

    public RemoteAiCopilotFacadeController(AiCopilotFacade aiCopilotFacade) {
        this.aiCopilotFacade = aiCopilotFacade;
    }

    @PostMapping(value = "/chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Map<String, ?>> chat(@RequestBody Request request) {
        return aiCopilotFacade.chat(request.message, request.conversationId, request.context);
    }

    public record Request(String message, String conversationId, ContextDTO context) {
    }
}
