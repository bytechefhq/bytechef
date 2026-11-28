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

package com.bytechef.ai.copilot.web.rest;

import com.agui.server.spring.AgUiParameters;
import com.bytechef.ai.copilot.facade.CopilotChatFacade;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
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
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
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
