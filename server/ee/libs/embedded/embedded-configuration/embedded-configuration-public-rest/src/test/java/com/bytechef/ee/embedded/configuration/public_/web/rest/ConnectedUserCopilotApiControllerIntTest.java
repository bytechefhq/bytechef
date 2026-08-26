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
import com.bytechef.ai.copilot.constant.CopilotConstants;
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
        assertThat(stateMap).containsKey(CopilotConstants.STATE_TENANT_ID);
        assertThat(stateMap).containsKey(CopilotConstants.STATE_AUTHENTICATION);
        assertThat(stateMap).containsEntry(CopilotConstants.STATE_ENVIRONMENT_ID,
            (long) Environment.PRODUCTION.ordinal());
        assertThat(stateMap.get(TaskTools.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY))
            .isEqualTo(Set.of("slack"));
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatDropsClientSuppliedAuthenticatedUserId() throws Exception {
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
                        "{\"threadId\":\"thread-1\",\"state\":{\"" + CopilotConstants.STATE_AUTHENTICATED_USER_ID
                            + "\":999}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(any(LocalAgent.class), parametersCaptor.capture());

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        // A connected user has no platform user id: a client-supplied authenticatedUserId must never reach the
        // agent, since WorkflowEditorSpringAIAgent gives a non-null userId precedence over the carried
        // Authentication and would rehydrate that user's real SecurityContext.
        assertThat(stateMap).doesNotContainKey(CopilotConstants.STATE_AUTHENTICATED_USER_ID);
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatDropsClientSuppliedWorkspaceId() throws Exception {
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
                        "{\"threadId\":\"thread-1\",\"state\":{\"" + CopilotConstants.STATE_WORKSPACE_ID
                            + "\":999}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(any(LocalAgent.class), parametersCaptor.capture());

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        // Embedded connected-user runs have no workspace; a client-supplied workspaceId must never reach the
        // agent's tool context.
        assertThat(stateMap).doesNotContainKey(CopilotConstants.STATE_WORKSPACE_ID);
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatTrimsClientAdditionalSystemPrompt() throws Exception {
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

        // The client-supplied additionalSystemPrompt is trimmed before being forwarded to the agent.
        assertThat(stateMap).containsEntry(CopilotConstants.STATE_ADDITIONAL_SYSTEM_PROMPT, "Prefer Slack.");
    }

    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatCapsOverlongAdditionalSystemPrompt() throws Exception {
        SseEmitter completedEmitter = new SseEmitter();

        when(connectedUserProjectFacade.prepareCopilotChat(
            eq("ext-user-1"), eq(WORKFLOW_UUID), eq(Environment.PRODUCTION)))
                .thenReturn(new CopilotChatContextDTO("wf-99", Set.of("slack")));

        when(agUiService.runAgent(any(LocalAgent.class), any(AgUiParameters.class)))
            .thenReturn(completedEmitter);

        String overlongPrompt = "a".repeat(CopilotConstants.ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH + 100);

        mockMvc
            .perform(
                post("/v1/automation/workflows/{workflowUuid}/copilot/chat", WORKFLOW_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"threadId\":\"thread-1\",\"state\":{\"additionalSystemPrompt\":\"" + overlongPrompt + "\"}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(any(LocalAgent.class), parametersCaptor.capture());

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        // Oversized prompts are truncated to the maximum allowed length before reaching the agent.
        assertThat((String) stateMap.get(CopilotConstants.STATE_ADDITIONAL_SYSTEM_PROMPT))
            .hasSize(CopilotConstants.ADDITIONAL_SYSTEM_PROMPT_MAX_LENGTH);
    }

    /**
     * Ticket 1051: containment for the {@code project_build} agent's tool set, whose
     * {@code ProjectTools.publishProject} takes a caller-supplied {@code projectId} and is guarded only by
     * {@code ProjectServiceImpl.publishProject}'s own {@code hasPermission(#id, 'Project', 'DEPLOYMENT_PUSH')} gate --
     * the gate {@code ConnectedUserResourceMembershipResolver.CATALOG_PROVISIONABLE_PROJECT_SCOPES} now answers GRANTED
     * for on a visible catalog project. If a connected user could drive that agent, they could publish a new version of
     * the tenant admin's catalog project.
     *
     * <p>
     * What this test proves, precisely: the ONE embedded connected-user copilot surface selects its agent SERVER-SIDE
     * and hardcoded ({@code Source.WORKFLOW_EDITOR} + {@code Mode.BUILD}), so no client state can steer it to another
     * agent, {@code project_build} included. Both other {@code LocalAgent} resolution sites are outside the embedded
     * surface: {@code CopilotWorkflowGeneratorImpl} hardcodes the same id, and {@code CopilotChatFacadeImpl} takes a
     * client-supplied {@code agentId} but is the platform copilot REST surface.
     *
     * <p>
     * What it deliberately does NOT prove: that a connected user reaching {@code CopilotChatFacadeImpl} would be
     * refused. It would not be refused there by a gate -- that facade lets any authenticated caller name an
     * {@code agentId}, and its {@code authorizeWorkflowAccess} no-ops when the state carries no {@code workflowId}.
     * What actually stops the tools today is that {@code CopilotChatFacadeImpl} never sets
     * {@code CopilotConstants.STATE_AUTHENTICATION}, so the agent's worker threads inherit no principal and RBAC denies
     * -- containment by WIRING, not by a gate. This test pins the half that is structural. A change to
     * {@code SecurityContextRehydrator}, or to who populates {@code STATE_AUTHENTICATION}, would not turn it red, so
     * that half still needs reading rather than trusting.
     */
    @Test
    @WithMockUser(username = "ext-user-1")
    public void testCopilotChatCannotBeSteeredToTheProjectBuildAgent() throws Exception {
        SseEmitter completedEmitter = new SseEmitter();

        when(connectedUserProjectFacade.prepareCopilotChat(
            eq("ext-user-1"), eq(WORKFLOW_UUID), eq(Environment.PRODUCTION)))
                .thenReturn(new CopilotChatContextDTO("wf-99", Set.of("slack")));

        when(agUiService.runAgent(any(LocalAgent.class), any(AgUiParameters.class)))
            .thenReturn(completedEmitter);

        // Every key a client might plausibly reach for to name a different agent, all in one request.
        mockMvc
            .perform(
                post("/v1/automation/workflows/{workflowUuid}/copilot/chat", WORKFLOW_UUID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"threadId\":\"thread-1\",\"state\":{\"agentId\":\"project_build\","
                            + "\"source\":\"PROJECT\",\"mode\":\"ASK\"}}")
                    .accept(MediaType.TEXT_EVENT_STREAM))
            .andExpect(status().isOk());

        ArgumentCaptor<LocalAgent> agentCaptor = ArgumentCaptor.forClass(LocalAgent.class);
        ArgumentCaptor<AgUiParameters> parametersCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(agentCaptor.capture(), parametersCaptor.capture());

        assertThat(agentCaptor.getValue()
            .getAgentId()).isEqualTo("workflow_editor_build");

        Map<String, Object> stateMap = parametersCaptor.getValue()
            .getState()
            .getState();

        // The mode is server-set too, so the client cannot even downgrade the run it gets.
        assertThat(stateMap).containsEntry("mode", "BUILD");
        assertThat(stateMap).doesNotContainKey("agentId");
        assertThat(stateMap).doesNotContainKey("source");
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

        /**
         * Registered so {@code testCopilotChatCannotBeSteeredToTheProjectBuildAgent} has something to steer TOWARDS.
         * Without it that test would pass on the agent simply being absent from the map, which proves nothing about how
         * the controller selects one.
         */
        @Bean
        public LocalAgent projectBuildAgent() {
            LocalAgent agent = mock(LocalAgent.class);

            when(agent.getAgentId()).thenReturn("project_build");

            return agent;
        }
    }
}
