/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.state.State;
import com.agui.core.tool.Tool;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.ai.hub.agent.AiHubChatStreamer;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link AiHubApiController}. Collaborators are Mockito mocks except the in-flight registry and the
 * streamer, which are real so the one-turn gate and the parameters the agent receives are exercised end to end.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubApiControllerTest {

    private static final long OWNER_USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long WORKSPACE_ID = 7L;
    private static final long OTHER_WORKSPACE_ID = 8L;
    private static final long CHAT_ID = 100L;
    private static final String THREAD_ID = "thread-1";

    private final AiHubChatAccessPolicy accessPolicy = mock(AiHubChatAccessPolicy.class);
    private final LocalAgent askAgent = mock(LocalAgent.class);
    private final LocalAgent buildAgent = mock(LocalAgent.class);
    private final AiHubChatService chatService = mock(AiHubChatService.class);
    private final InFlightAiHubRunRegistry inFlightRunRegistry = new InFlightAiHubRunRegistry();
    private final PermissionService permissionService = mock(PermissionService.class);
    private final AiHubToolApprovalService toolApprovalService = mock(AiHubToolApprovalService.class);
    private final UserService userService = mock(UserService.class);

    private AiHubApiController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(askAgent.getAgentId()).thenReturn("ai_hub_ask");
        when(buildAgent.getAgentId()).thenReturn("ai_hub_build");

        ObjectProvider<PermissionService> permissionServiceProvider = mock(ObjectProvider.class);

        lenient().when(permissionServiceProvider.getIfAvailable())
            .thenReturn(permissionService);

        User currentUser = mock(User.class);

        lenient().when(currentUser.getId())
            .thenReturn(OWNER_USER_ID);
        lenient().when(userService.getCurrentUser())
            .thenReturn(currentUser);

        lenient().when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER"))
            .thenReturn(true);
        lenient().when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR"))
            .thenReturn(true);

        controller = new AiHubApiController(
            new AiHubChatStreamer(inFlightRunRegistry, new JsonMapper()), inFlightRunRegistry,
            List.of(askAgent, buildAgent), chatService,
            accessPolicy, userService, permissionServiceProvider, toolApprovalService);
    }

    @Test
    void testTurnOnABrandNewThreadPersistsTheChatBeforeTheAgentSeesTheVerifiedKeys() {
        AiHubChat createdChat = chat(OWNER_USER_ID, WORKSPACE_ID);

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());
        when(chatService.create(WORKSPACE_ID, OWNER_USER_ID, 0, THREAD_ID)).thenReturn(createdChat);

        AgUiParameters agUiParameters = agUiParameters("ASK", "run-1");
        State state = agUiParameters.getState();

        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, 999L);
        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 999L);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, "forged-thread");

        controller.chat(agUiParameters);

        InOrder order = inOrder(chatService, askAgent);

        order.verify(chatService)
            .create(WORKSPACE_ID, OWNER_USER_ID, 0, THREAD_ID);
        order.verify(askAgent)
            .runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));

        Map<String, Object> stateMap = capturedParameters(askAgent).getState()
            .getState();

        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_THREAD_ID)).isEqualTo(THREAD_ID);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID)).isEqualTo(WORKSPACE_ID);
        assertThat(stateMap.get(AiHubStateKeys.AUTHENTICATED_USER_ID)).isEqualTo(OWNER_USER_ID);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_OWNER_USER_ID)).isEqualTo(OWNER_USER_ID);
    }

    @Test
    void testTurnOnAnExistingChatDoesNotCreateAnother() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        controller.chat(agUiParameters("ASK", "run-1"));

        verify(chatService, never()).create(anyLong(), anyLong(), anyInt(), anyString());
        verify(askAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testAgentThreadIdEqualsTheVerifiedThreadIdWhenTheStateCarriesAnotherOne() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        AgUiParameters agUiParameters = agUiParameters("ASK", "run-1");

        agUiParameters.getState()
            .set(AiHubStateKeys.THREAD_ID, "victim-thread");

        controller.chat(agUiParameters);

        RunAgentParameters parameters = capturedParameters(askAgent);
        Map<String, Object> stateMap = parameters.getState()
            .getState();

        assertThat(parameters.getThreadId()).isEqualTo(THREAD_ID);
        assertThat(stateMap.get(AiHubStateKeys.VERIFIED_THREAD_ID)).isEqualTo(THREAD_ID);
        assertThat(stateMap.get(AiHubStateKeys.THREAD_ID)).isEqualTo(THREAD_ID);
        verify(chatService, never()).findByThreadId("victim-thread");
    }

    @Test
    void testThreadIdFallsBackToTheStateAndBecomesTheAgentThreadId() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        AgUiParameters agUiParameters = agUiParameters("ASK", "run-1");

        agUiParameters.setThreadId(null);
        agUiParameters.getState()
            .set(AiHubStateKeys.THREAD_ID, THREAD_ID);

        controller.chat(agUiParameters);

        assertThat(capturedParameters(askAgent).getThreadId()).isEqualTo(THREAD_ID);
    }

    @Test
    void testTurnWithoutAThreadIdIsRejected() {
        AgUiParameters agUiParameters = agUiParameters("ASK", "run-1");

        agUiParameters.setThreadId(" ");

        assertStatus(() -> controller.chat(agUiParameters), HttpStatus.BAD_REQUEST);

        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testClientSuppliedToolsNeverReachTheAgent() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        AgUiParameters agUiParameters = agUiParameters("ASK", "run-1");

        agUiParameters.setTools(List.of(new Tool("clientTool", "client tool", null)));

        controller.chat(agUiParameters);

        assertThat(capturedParameters(askAgent).getTools()).isEmpty();
    }

    @Test
    void testSecondConcurrentTurnOnTheSameChatIs409() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        controller.chat(agUiParameters("ASK", "run-1"));

        assertThatThrownBy(() -> controller.chat(agUiParameters("ASK", "run-2")))
            .isInstanceOf(TurnInFlightException.class);

        ResponseEntity<Map<String, Object>> response = controller.handleTurnInFlight(new TurnInFlightException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "TURN_IN_FLIGHT");

        verify(askAgent, times(1)).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(toolApprovalService, times(1)).supersedePending(CHAT_ID);
    }

    @Test
    void testTurnOnAnotherUsersChatIsForbidden() {
        AiHubChat chat = chat(OTHER_USER_ID, WORKSPACE_ID);

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));
        when(accessPolicy.canParticipate(chat, OWNER_USER_ID)).thenReturn(false);

        assertStatus(() -> controller.chat(agUiParameters("ASK", "run-1")), HttpStatus.FORBIDDEN);

        verify(chatService, never()).create(anyLong(), anyLong(), anyInt(), anyString());
        verify(toolApprovalService, never()).supersedePending(anyLong());
        verify(askAgent, never()).runAgent(any(), any());
        assertThat(inFlightRunRegistry.isInFlight(THREAD_ID)).isFalse();
    }

    @Test
    void testTurnOnAChatOfAnotherWorkspaceIsForbidden() {
        AiHubChat chat = chat(OWNER_USER_ID, OTHER_WORKSPACE_ID);

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));
        when(accessPolicy.canParticipate(chat, OWNER_USER_ID)).thenReturn(true);

        assertStatus(() -> controller.chat(agUiParameters("ASK", "run-1")), HttpStatus.FORBIDDEN);

        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testTurnIsForbiddenWhenAnotherUserCreatesTheThreadConcurrently() {
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());
        when(chatService.create(WORKSPACE_ID, OWNER_USER_ID, 0, THREAD_ID))
            .thenThrow(new ConflictException("AiHubChat thread-1 already exists"));

        assertStatus(() -> controller.chat(agUiParameters("ASK", "run-1")), HttpStatus.FORBIDDEN);

        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testBuildModeRequiresEditorRole() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "EDITOR")).thenReturn(false);

        assertStatus(() -> controller.chat(agUiParameters("BUILD", "run-1")), HttpStatus.FORBIDDEN);

        verify(buildAgent, never()).runAgent(any(), any());
        verify(chatService, never()).findByThreadId(anyString());

        controller.chat(agUiParameters("ASK", "run-2"));

        verify(permissionService).hasWorkspaceRole(WORKSPACE_ID, "VIEWER");
        verify(askAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testBuildModeWithEditorRoleRunsTheBuildAgent() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        controller.chat(agUiParameters("BUILD", "run-1"));

        verify(permissionService).hasWorkspaceRole(WORKSPACE_ID, "EDITOR");
        verify(buildAgent).runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testAskModeWithoutViewerRoleIsForbidden() {
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(false);

        assertStatus(() -> controller.chat(agUiParameters("ASK", "run-1")), HttpStatus.FORBIDDEN);

        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testTurnWithoutAWorkspaceIdIsRejected() {
        AgUiParameters agUiParameters = agUiParameters("ASK", "run-1");

        agUiParameters.getState()
            .getState()
            .remove(AiHubStateKeys.WORKSPACE_ID);

        assertStatus(() -> controller.chat(agUiParameters), HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUnknownModeIsRejected() {
        assertStatus(() -> controller.chat(agUiParameters("DESTROY", "run-1")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void testUnregisteredAgentVariantIs404AndLeavesNoRunBehind() {
        ObjectProvider<PermissionService> permissionServiceProvider = permissionServiceProvider();

        AiHubApiController askOnlyController = new AiHubApiController(
            new AiHubChatStreamer(inFlightRunRegistry, new JsonMapper()), inFlightRunRegistry, List.of(askAgent),
            chatService,
            accessPolicy, userService, permissionServiceProvider, toolApprovalService);

        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        assertStatus(() -> askOnlyController.chat(agUiParameters("BUILD", "run-1")), HttpStatus.NOT_FOUND);

        verify(toolApprovalService, never()).supersedePending(anyLong());
        assertThat(inFlightRunRegistry.isInFlight(THREAD_ID)).isFalse();
    }

    @Test
    void testNewTurnSupersedesPendingApprovals() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        controller.chat(agUiParameters("ASK", "run-1"));

        InOrder order = inOrder(toolApprovalService, askAgent);

        order.verify(toolApprovalService)
            .supersedePending(CHAT_ID);
        order.verify(askAgent)
            .runAgent(any(RunAgentParameters.class), any(AgentSubscriber.class));
    }

    @Test
    void testFailureBeforeTheAgentStartsReleasesTheChat() {
        stubExistingChat(chat(OWNER_USER_ID, WORKSPACE_ID));

        doThrow(new IllegalStateException("database down")).when(toolApprovalService)
            .supersedePending(CHAT_ID);

        assertThatThrownBy(() -> controller.chat(agUiParameters("ASK", "run-1")))
            .isInstanceOf(IllegalStateException.class);

        assertThat(inFlightRunRegistry.isInFlight(THREAD_ID)).isFalse();
        verify(askAgent, never()).runAgent(any(), any());
    }

    @Test
    void testAttachAnswersAnUnknownAndANonViewableThreadIdentically() {
        when(chatService.findByThreadIdViewable("unknown-thread", OWNER_USER_ID)).thenReturn(Optional.empty());
        when(chatService.findByThreadIdViewable(THREAD_ID, OWNER_USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException unknownThreadException = catchResponseStatusException(
            () -> controller.attach("unknown-thread"));

        inFlightRunRegistry.tryStart(THREAD_ID, "run-1");

        ResponseStatusException nonViewableThreadException = catchResponseStatusException(
            () -> controller.attach(THREAD_ID));

        assertThat(unknownThreadException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(nonViewableThreadException.getStatusCode()).isEqualTo(unknownThreadException.getStatusCode());
        assertThat(nonViewableThreadException.getReason()).isEqualTo(unknownThreadException.getReason());
    }

    @Test
    void testAttachReturnsAnEmitterForAViewableThreadWithARun() {
        when(chatService.findByThreadIdViewable(THREAD_ID, OWNER_USER_ID))
            .thenReturn(Optional.of(chat(OWNER_USER_ID, WORKSPACE_ID)));

        inFlightRunRegistry.tryStart(THREAD_ID, "run-1");

        assertThat(controller.attach(THREAD_ID)).isInstanceOf(SseEmitter.class);
    }

    @Test
    void testAttachAnswersAnOwnerRemovedFromTheWorkspaceAsAnUnknownThread() {
        when(chatService.findByThreadIdViewable(THREAD_ID, OWNER_USER_ID))
            .thenReturn(Optional.of(chat(OWNER_USER_ID, WORKSPACE_ID)));
        when(permissionService.hasWorkspaceRole(WORKSPACE_ID, "VIEWER")).thenReturn(false);

        inFlightRunRegistry.tryStart(THREAD_ID, "run-1");

        ResponseStatusException exception = catchResponseStatusException(() -> controller.attach(THREAD_ID));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Unknown thread");
    }

    @Test
    void testAttachToAWorkflowChatRequiresTheWorkflowViewScope() {
        when(chatService.findByThreadIdViewable(THREAD_ID, OWNER_USER_ID))
            .thenReturn(Optional.of(workflowChat()));
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "WORKFLOW_VIEW")).thenReturn(false);

        inFlightRunRegistry.tryStart(THREAD_ID, "run-1");

        assertStatus(() -> controller.attach(THREAD_ID), HttpStatus.NOT_FOUND);
    }

    @Test
    void testAttachIs404WhenNoRunIsRegistered() {
        when(chatService.findByThreadIdViewable(THREAD_ID, OWNER_USER_ID))
            .thenReturn(Optional.of(chat(OWNER_USER_ID, WORKSPACE_ID)));

        assertStatus(() -> controller.attach(THREAD_ID), HttpStatus.NOT_FOUND);
    }

    private void stubExistingChat(AiHubChat chat) {
        lenient().when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        lenient().when(accessPolicy.canParticipate(chat, OWNER_USER_ID))
            .thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PermissionService> permissionServiceProvider() {
        ObjectProvider<PermissionService> permissionServiceProvider = mock(ObjectProvider.class);

        when(permissionServiceProvider.getIfAvailable()).thenReturn(permissionService);

        return permissionServiceProvider;
    }

    private static RunAgentParameters capturedParameters(LocalAgent agent) {
        ArgumentCaptor<RunAgentParameters> parametersCaptor = ArgumentCaptor.forClass(RunAgentParameters.class);

        verify(agent).runAgent(parametersCaptor.capture(), any(AgentSubscriber.class));

        return parametersCaptor.getValue();
    }

    private static AgUiParameters agUiParameters(String mode, String runId) {
        AgUiParameters agUiParameters = new AgUiParameters();
        State state = new State();

        state.set(AiHubStateKeys.WORKSPACE_ID, WORKSPACE_ID);
        state.set("mode", mode);

        agUiParameters.setState(state);
        agUiParameters.setThreadId(THREAD_ID);
        agUiParameters.setRunId(runId);

        return agUiParameters;
    }

    private static AiHubChat chat(long ownerUserId, long workspaceId) {
        AiHubChat chat = new AiHubChat(ownerUserId);

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setWorkspaceId(workspaceId);

        return chat;
    }

    private static void assertStatus(Runnable runnable, HttpStatusCode expectedStatus) {
        assertThat(catchResponseStatusException(runnable).getStatusCode()).isEqualTo(expectedStatus);
    }

    private static ResponseStatusException catchResponseStatusException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ResponseStatusException exception) {
            return exception;
        }

        throw new AssertionError("Expected a ResponseStatusException, but none was thrown");
    }
}
