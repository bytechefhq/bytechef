/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agui.json.ObjectMapperFactory;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = CopilotApiControllerIntTest.CopilotApiControllerTestConfiguration.class)
@TestPropertySource(properties = {
    "bytechef.edition=ee",
    "bytechef.ai.copilot.enabled=true"
})
@WebMvcTest(CopilotApiController.class)
class CopilotApiControllerIntTest {

    @Autowired
    private AgUiService agUiService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(agUiService);

        when(agUiService.runAgent(any(LocalAgent.class), any(AgUiParameters.class)))
            .thenReturn(new SseEmitter());
    }

    @Test
    @WithMockUser
    void testMcpServerSourceRunsTheMcpServerBuildAgent() throws Exception {
        mockMvc
            .perform(
                post("/internal/ai/chat/mcp_server")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"threadId\":\"thread-1\",\"state\":{\"mode\":\"ASK\"}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<LocalAgent> localAgentCaptor = ArgumentCaptor.forClass(LocalAgent.class);

        verify(agUiService).runAgent(localAgentCaptor.capture(), any(AgUiParameters.class));

        LocalAgent localAgent = localAgentCaptor.getValue();

        assertThat(localAgent.getAgentId()).isEqualTo("mcp_server_build");
    }

    @Configuration
    @Import(CopilotApiController.class)
    static class CopilotApiControllerTestConfiguration {

        @Bean
        @Primary
        JsonMapper jsonMapper() {
            return JsonMapper.builder()
                .addModule(ObjectMapperFactory.createModule())
                .build();
        }

        @Bean
        AgUiService agUiService() {
            return mock(AgUiService.class);
        }

        @Bean
        LocalAgent mcpServerBuildAgent() {
            return localAgent("mcp_server_build");
        }

        @Bean
        LocalAgent skillsAskAgent() {
            return localAgent("skills_ask");
        }

        private static LocalAgent localAgent(String agentId) {
            LocalAgent localAgent = mock(LocalAgent.class);

            when(localAgent.getAgentId()).thenReturn(agentId);

            return localAgent;
        }
    }
}
