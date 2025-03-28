/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.ai.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.ai.facade.AiCopilotFacade;
import com.bytechef.platform.ai.facade.dto.ContextDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotApiController {

    private final AiCopilotFacade aiCopilotFacade;

    public AiCopilotApiController(AiCopilotFacade aiCopilotFacade) {
        this.aiCopilotFacade = aiCopilotFacade;
    }

    @PostMapping("/ai/chat")
    public Flux<Map<String, ?>> chat(
        @RequestBody Request request, @RequestHeader("X-Copilot-Conversation-Id") String conversationId) {

        return aiCopilotFacade.chat(request.message, request.context, conversationId);
    }

    @SuppressFBWarnings("EI")
    public record Request(String message, ContextDTO context) {
    }

}
