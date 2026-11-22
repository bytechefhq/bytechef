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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agui.server.spring.AgUiParameters;
import com.bytechef.ai.copilot.facade.CopilotChatFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = CopilotApiController.class)
@TestPropertySource(properties = {
    "bytechef.coordinator.enabled=true",
    "bytechef.ai.copilot.enabled=true"
})
@WebMvcTest(CopilotApiController.class)
class CopilotApiControllerIntTest {

    @MockitoBean
    private CopilotChatFacade copilotChatFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testChatDelegatesToTheGuardedFacade() throws Exception {
        when(copilotChatFacade.chat(eq("workflow_editor"), any(AgUiParameters.class)))
            .thenReturn(new SseEmitter());

        mockMvc
            .perform(
                post("/internal/ai/chat/{agentId}", "workflow_editor")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"threadId\":\"thread-1\",\"state\":{\"state\":{}}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        verify(copilotChatFacade).chat(eq("workflow_editor"), any(AgUiParameters.class));
    }

    @Test
    void testChatPropagatesTheFacadeAccessDenial() {
        when(copilotChatFacade.chat(eq("workflow_editor"), any(AgUiParameters.class)))
            .thenThrow(new AccessDeniedException("Access denied"));

        assertThatThrownBy(() -> mockMvc.perform(
            post("/internal/ai/chat/{agentId}", "workflow_editor")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"threadId\":\"thread-1\",\"state\":{\"state\":{}}}")
                .accept(MediaType.TEXT_EVENT_STREAM)))
                    .hasCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testChatReturnsNotFoundForAnUnknownAgent() throws Exception {
        when(copilotChatFacade.chat(eq("unknown"), any(AgUiParameters.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown agentId: unknown"));

        mockMvc
            .perform(
                post("/internal/ai/chat/{agentId}", "unknown")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"threadId\":\"thread-1\",\"state\":{\"state\":{}}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isNotFound());
    }
}
