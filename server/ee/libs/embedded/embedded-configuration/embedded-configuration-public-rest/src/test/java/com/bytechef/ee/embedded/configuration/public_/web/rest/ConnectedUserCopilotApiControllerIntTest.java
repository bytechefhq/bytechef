/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agui.json.ObjectMapperFactory;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.ee.ai.copilot.util.CopilotStateKeys;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.platform.ai.tool.TaskTools;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(
    classes = {
        EmbeddedConfigurationPublicRestTestConfiguration.class,
        ConnectedUserCopilotApiControllerIntTest.CopilotTestConfiguration.class
    })
@TestPropertySource(properties = {
    "bytechef.edition=ee",
    "bytechef.ai.copilot.enabled=true"
})
@WebMvcTest(ConnectedUserCopilotApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
public class ConnectedUserCopilotApiControllerIntTest {

    private static final String WORKFLOW_UUID = "uuid-1";

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private AgUiService agUiService;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        Mockito.reset(agUiService, connectedUserProjectFacade);

        when(environmentService.getEnvironment(any()))
            .thenReturn(Environment.PRODUCTION);
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatAuthorizesResolvesStateAndRunsBuildAgent() throws Exception {
        SseEmitter completedEmitter = new SseEmitter();

        when(connectedUserProjectFacade.prepareCopilotChat(
            eq("ext-user-1"), eq(WORKFLOW_UUID), eq(Environment.PRODUCTION)))
                .thenReturn(new CopilotChatContextDTO("wf-99", Set.of("slack")));

        when(agUiService.runAgent(any(LocalAgent.class), any(AgUiParameters.class)))
            .thenReturn(completedEmitter);

        MvcResult mvcResult = mockMvc
            .perform(
                post("/v1/automation/workflows/{workflowUuid}/copilot/chat", WORKFLOW_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"threadId\":\"thread-1\",\"state\":{\"state\":{}}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/event-stream")))
            .andReturn();

        // SSE endpoints are async — verify the async result is a SseEmitter (not the response body).
        assertThat(mvcResult.getRequest()
            .isAsyncStarted()).isTrue();

        verify(connectedUserProjectFacade).prepareCopilotChat("ext-user-1", WORKFLOW_UUID, Environment.PRODUCTION);

        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);
        ArgumentCaptor<LocalAgent> agentCaptor = ArgumentCaptor.forClass(LocalAgent.class);

        verify(agUiService).runAgent(agentCaptor.capture(), parametersCaptor.capture());

        assertThat(agentCaptor.getValue()
            .getAgentId()).isEqualTo("workflow_editor_build");

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        assertThat(stateMap).containsEntry("workflowId", "wf-99");
        assertThat(stateMap).containsEntry("mode", "BUILD");
        assertThat(stateMap).containsEntry("autonomous", false);
        assertThat(stateMap).containsKey(CopilotStateKeys.STATE_TENANT_ID);
        assertThat(stateMap).containsKey(CopilotStateKeys.STATE_AUTHENTICATION);
        assertThat(stateMap.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY))
            .isEqualTo(Set.of("slack"));
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatCapsAndReappliesClientAdditionalSystemPrompt() throws Exception {
        SseEmitter completedEmitter = new SseEmitter();

        when(connectedUserProjectFacade.prepareCopilotChat(
            eq("ext-user-1"), eq(WORKFLOW_UUID), eq(Environment.PRODUCTION)))
                .thenReturn(new CopilotChatContextDTO("wf-99", Set.of("slack")));

        when(agUiService.runAgent(any(LocalAgent.class), any(AgUiParameters.class)))
            .thenReturn(completedEmitter);

        mockMvc
            .perform(
                post("/v1/automation/workflows/{workflowUuid}/copilot/chat", WORKFLOW_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"threadId\":\"thread-1\",\"state\":{\"additionalSystemPrompt\":\"  Prefer Slack.  \"}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(any(LocalAgent.class), parametersCaptor.capture());

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        // The untrusted short client key is consumed (trimmed) and re-applied under the authoritative key.
        assertThat(stateMap).containsEntry(CopilotStateKeys.STATE_ADDITIONAL_SYSTEM_PROMPT, "Prefer Slack.");
        assertThat(stateMap).doesNotContainKey("additionalSystemPrompt");
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatDropsClientSuppliedCanonicalSystemPromptKey() throws Exception {
        SseEmitter completedEmitter = new SseEmitter();

        when(connectedUserProjectFacade.prepareCopilotChat(
            eq("ext-user-1"), eq(WORKFLOW_UUID), eq(Environment.PRODUCTION)))
                .thenReturn(new CopilotChatContextDTO("wf-99", Set.of("slack")));

        when(agUiService.runAgent(any(LocalAgent.class), any(AgUiParameters.class)))
            .thenReturn(completedEmitter);

        mockMvc
            .perform(
                post("/v1/automation/workflows/{workflowUuid}/copilot/chat", WORKFLOW_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"threadId\":\"thread-1\",\"state\":{\"bytechef.copilot.additionalSystemPrompt\":\"smuggled\"}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(any(LocalAgent.class), parametersCaptor.capture());

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        // The canonical key must be absent — the controller drops any client-supplied canonical key before the
        // short-key sanitization block runs, so it can never be smuggled in directly.
        assertThat(stateMap).doesNotContainKey(CopilotStateKeys.STATE_ADDITIONAL_SYSTEM_PROMPT);
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatBlocksAccessForForeignWorkflowUuid() throws Exception {
        when(connectedUserProjectFacade.prepareCopilotChat(
            eq("ext-user-1"), eq("foreign-uuid"), eq(Environment.PRODUCTION)))
                .thenThrow(new AccessDeniedException("Access denied"));

        // In the @WebMvcTest slice, AccessDeniedException is wrapped in a ServletException rather than
        // translated to 403 (no full Spring Security ExceptionTranslationFilter). The important invariant
        // is that runAgent is NEVER called when the ownership check fails — ownership comes before agent dispatch.
        boolean exceptionThrown = false;

        try {
            mockMvc
                .perform(
                    post("/v1/automation/workflows/{workflowUuid}/copilot/chat", "foreign-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"threadId\":\"thread-1\",\"state\":{\"state\":{}}}")
                        .accept(MediaType.TEXT_EVENT_STREAM));
        } catch (Exception exception) {
            assertThat(exception.getCause()).isInstanceOf(AccessDeniedException.class);
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Configuration
    static class CopilotTestConfiguration {

        @Bean
        @Primary
        public JsonMapper jsonMapper() {
            return JsonMapper.builder()
                .addModule(ObjectMapperFactory.createModule())
                .build();
        }

        @Bean
        public AgUiService agUiService() {
            return mock(AgUiService.class);
        }

        @Bean
        public LocalAgent workflowEditorBuildAgent() {
            LocalAgent agent = mock(LocalAgent.class);

            when(agent.getAgentId()).thenReturn("workflow_editor_build");

            return agent;
        }
    }
}
