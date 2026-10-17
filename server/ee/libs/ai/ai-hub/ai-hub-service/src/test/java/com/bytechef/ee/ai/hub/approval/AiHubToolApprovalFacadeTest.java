/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.AutomationToolInvocationContext;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.ForbiddenException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubToolApprovalFacadeTest {

    private static final long WORKSPACE_ID = 7L;
    private static final long CHAT_OWNER_ID = 3L;
    private static final long CHAT_ID = 10L;
    private static final long STRANGER_ID = 4L;
    private static final String OWNER_LOGIN = "owner";
    private static final String STRANGER_LOGIN = "stranger";
    private static final String THREAD_ID = "thread-1";
    private static final String TOOL_NAME = "deleteProjectDeployment";
    private static final String RUN_ID = "run-1";

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC);

    private AiHubToolApprovalService approvalService;
    private AiHubChatService chatService;
    private UserService userService;
    private AiHubPinnedToolCallbacks pinnedToolCallbacks;
    private AiHubContinuationStarter continuationStarter;
    private SecurityContextRehydrator securityContextRehydrator;
    private PlatformTransactionManager transactionManager;
    private ToolCallback gatedDelegate;
    private AiHubChat chat;
    private AiHubToolApprovalFacadeImpl facade;

    @BeforeEach
    void setUp() {
        approvalService = mock(AiHubToolApprovalService.class);
        chatService = mock(AiHubChatService.class);
        userService = mock(UserService.class);
        pinnedToolCallbacks = mock(AiHubPinnedToolCallbacks.class);
        continuationStarter = mock(AiHubContinuationStarter.class);

        gatedDelegate = callback(TOOL_NAME);

        when(pinnedToolCallbacks.pinnedToolCallbacks(Mode.BUILD)).thenReturn(List.of(gatedDelegate));
        when(pinnedToolCallbacks.pinnedToolCallbacks(Mode.ASK)).thenReturn(List.of());

        chat = new AiHubChat(CHAT_OWNER_ID);

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setWorkspaceId(WORKSPACE_ID);

        when(chatService.getManageable(CHAT_ID, WORKSPACE_ID, CHAT_OWNER_ID)).thenReturn(chat);
        when(chatService.getManageable(CHAT_ID, WORKSPACE_ID, STRANGER_ID))
            .thenThrow(new NotFoundException("AiHubChat not found"));
        when(approvalService.save(any(AiHubToolApproval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(continuationStarter.startContinuation(any(), any(), anyLong(), anyString()))
            .thenReturn(Optional.of(RUN_ID));

        User owner = user(CHAT_OWNER_ID, OWNER_LOGIN);

        when(userService.getCurrentUser()).thenReturn(owner);
        when(userService.fetchUser(CHAT_OWNER_ID)).thenReturn(Optional.of(owner));

        securityContextRehydrator = new SecurityContextRehydrator(userService, mock(AuthorityService.class));
        transactionManager = noopTransactionManager();

        facade = newFacade(pinnedToolCallbacks, continuationStarter);

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(OWNER_LOGIN, "n/a", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testStrangerCannotResolve() {
        pendingApproval(TOOL_NAME, "BUILD");

        User stranger = user(STRANGER_ID, STRANGER_LOGIN);

        when(userService.getCurrentUser()).thenReturn(stranger);

        assertThatThrownBy(() -> facade.resolve(WORKSPACE_ID, 99L, true, null))
            .isInstanceOf(NotFoundException.class);

        verify(approvalService, never()).save(any());
        verify(gatedDelegate, never()).call(anyString(), any());
        verify(continuationStarter, never()).startContinuation(any(), any(), anyLong(), anyString());
    }

    @Test
    void testTenantAdminWhoIsNotTheOwnerCannotResolve() {
        pendingApproval(TOOL_NAME, "BUILD");

        User admin = user(STRANGER_ID, STRANGER_LOGIN);

        when(userService.getCurrentUser()).thenReturn(admin);
        doReturn(chat).when(chatService)
            .getManageable(CHAT_ID, WORKSPACE_ID, STRANGER_ID);

        assertThatThrownBy(() -> facade.resolve(WORKSPACE_ID, 99L, true, null))
            .isInstanceOf(ForbiddenException.class);

        verify(approvalService, never()).save(any());
        verify(gatedDelegate, never()).call(anyString(), any());
        verify(continuationStarter, never()).startContinuation(any(), any(), anyLong(), anyString());
    }

    @Test
    void testResolveInAnotherWorkspaceIsNotFound() {
        pendingApproval(TOOL_NAME, "BUILD");

        when(chatService.getManageable(CHAT_ID, 8L, CHAT_OWNER_ID))
            .thenThrow(new NotFoundException("AiHubChat not found"));

        assertThatThrownBy(() -> facade.resolve(8L, 99L, true, null)).isInstanceOf(NotFoundException.class);

        verify(gatedDelegate, never()).call(anyString(), any());
    }

    @Test
    void testExpiredApprovalNeverExecutes() {
        AiHubToolApproval approval = pendingApproval(TOOL_NAME, "BUILD");

        approval.setExpiresAt(Instant.now(clock)
            .minusSeconds(1));

        assertThatThrownBy(() -> facade.resolve(WORKSPACE_ID, 99L, true, null))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("expired");

        ArgumentCaptor<AiHubToolApproval> savedCaptor = ArgumentCaptor.forClass(AiHubToolApproval.class);

        verify(approvalService).save(savedCaptor.capture());

        assertThat(savedCaptor.getValue()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.EXPIRED);

        verify(transactionManager, never()).getTransaction(any());
        verify(gatedDelegate, never()).call(anyString(), any());
        verify(continuationStarter, never()).startContinuation(any(), any(), anyLong(), anyString());
    }

    @Test
    void testResolvingTwiceExecutesTheToolOnce() {
        AiHubToolApproval approval = pendingApproval(TOOL_NAME, "BUILD");

        when(gatedDelegate.call(eq(approval.getArguments()), any())).thenReturn("{\"deleted\":true}");

        facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThatThrownBy(() -> facade.resolve(WORKSPACE_ID, 99L, true, null))
            .isInstanceOf(ConflictException.class);

        verify(gatedDelegate, times(1)).call(anyString(), any());
    }

    @Test
    void testConcurrentResolveTranslatesOptimisticLockFailureToNotFound() {
        pendingApproval(TOOL_NAME, "BUILD");

        when(approvalService.save(any(AiHubToolApproval.class)))
            .thenThrow(new OptimisticLockingFailureException("stale version"));

        assertThatThrownBy(() -> facade.resolve(WORKSPACE_ID, 99L, true, null))
            .isInstanceOf(NotFoundException.class);

        verify(gatedDelegate, never()).call(anyString(), any());
        verify(continuationStarter, never()).startContinuation(any(), any(), anyLong(), anyString());
    }

    @Test
    void testRejectRecordsDecisionAndStartsContinuationWithoutExecuting() {
        pendingApproval(TOOL_NAME, "BUILD");

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, false, "not now");

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.REJECTED);
        assertThat(resolution.approval()
            .getDecidedByUserId()).isEqualTo(CHAT_OWNER_ID);
        assertThat(resolution.approval()
            .getDecidedAt()).isEqualTo(Instant.now(clock));
        assertThat(resolution.approval()
            .getComment()).isEqualTo("not now");
        assertThat(resolution.continuationStarted()).isTrue();
        assertThat(resolution.runId()).isEqualTo(RUN_ID);

        verify(gatedDelegate, never()).call(anyString(), any());

        String message = continuationMessage();

        assertThat(message).startsWith(AiHubContinuationStarter.CONTINUATION_PREFIX + "99 rejected]");
        assertThat(message).contains("Comment: not now");
    }

    @Test
    void testApprovePinnedToolActuallyExecutesWithTheStoredArguments() {
        AiHubToolApproval approval = pendingApproval(TOOL_NAME, "BUILD");

        when(gatedDelegate.call(eq(approval.getArguments()), any())).thenReturn("{\"deleted\":true}");

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.APPROVED);
        assertThat(resolution.approval()
            .getExecutionError()).isNull();

        verify(gatedDelegate).call(eq(approval.getArguments()), any());

        assertThat(continuationMessage()).contains("approved and executed")
            .contains("{\"deleted\":true}");
    }

    @Test
    void testApproveLooksUpThePinnedToolsOfTheApprovalsMode() {
        ToolCallback askTool = callback("deleteSomething");

        when(pinnedToolCallbacks.pinnedToolCallbacks(Mode.ASK)).thenReturn(List.of(askTool));
        when(askTool.call(anyString(), any())).thenReturn("{}");

        pendingApproval("deleteSomething", "ASK");

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.APPROVED);

        verify(askTool).call(anyString(), any());
    }

    @Test
    void testApprovedExecutionRunsAsTheRequesterWithTheChatsToolContext() {
        AiHubToolApproval approval = pendingApproval(TOOL_NAME, "BUILD");

        List<String> observedLogins = new ArrayList<>();

        when(gatedDelegate.call(eq(approval.getArguments()), any())).thenAnswer(invocation -> {
            observedLogins.add(SecurityUtils.getCurrentUserLogin());

            return "{\"deleted\":true}";
        });

        facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThat(observedLogins).containsExactly(OWNER_LOGIN);

        ArgumentCaptor<ToolContext> toolContextCaptor = ArgumentCaptor.forClass(ToolContext.class);

        verify(gatedDelegate).call(eq(approval.getArguments()), toolContextCaptor.capture());

        Map<String, Object> toolContext = toolContextCaptor.getValue()
            .getContext();

        assertThat(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY)).isEqualTo(CHAT_OWNER_ID);
        assertThat(toolContext.get(AgentToolInvocationContext.TOOL_CONTEXT_CONVERSATION_ID_KEY)).isEqualTo(THREAD_ID);
        assertThat(toolContext.get(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY))
            .isEqualTo(WORKSPACE_ID);
        assertThat(toolContext.get(AutomationToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY)).isEqualTo(THREAD_ID);
        assertThat(toolContext.get(AiHubApprovalGateToolCallback.TOOL_CONTEXT_MODE_KEY)).isEqualTo("BUILD");
    }

    @Test
    void testApproveWhenTheToolThrowsMarksFailed() {
        AiHubToolApproval approval = pendingApproval(TOOL_NAME, "BUILD");

        when(gatedDelegate.call(eq(approval.getArguments()), any())).thenThrow(new RuntimeException("boom"));

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.FAILED);
        assertThat(resolution.approval()
            .getExecutionError()).isEqualTo("boom");
        assertThat(resolution.continuationStarted()).isTrue();
        assertThat(continuationMessage()).contains("approved but the tool failed")
            .contains("Error: boom");
    }

    @Test
    void testApproveOfAToolNoLongerPinnedMarksFailed() {
        pendingApproval("deleteWorkflow", "BUILD");

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.FAILED);
        assertThat(resolution.approval()
            .getExecutionError()).contains("no longer available");
    }

    @Test
    void testApproveWithoutAnyPinnedToolSourceMarksFailed() {
        pendingApproval(TOOL_NAME, "BUILD");

        AiHubToolApprovalFacade.Resolution resolution = newFacade(null, continuationStarter).resolve(
            WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.FAILED);

        verify(gatedDelegate, never()).call(anyString(), any());
    }

    @Test
    void testNoContinuationStarterStillRecordsAndExecutes() {
        AiHubToolApproval approval = pendingApproval(TOOL_NAME, "BUILD");

        when(gatedDelegate.call(eq(approval.getArguments()), any())).thenReturn("{\"deleted\":true}");

        AiHubToolApprovalFacade.Resolution resolution = newFacade(pinnedToolCallbacks, null).resolve(
            WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.APPROVED);
        assertThat(resolution.continuationStarted()).isFalse();
        assertThat(resolution.runId()).isNull();

        verify(gatedDelegate).call(eq(approval.getArguments()), any());
    }

    @Test
    void testContinuationSkippedWhenTheStarterDeclines() {
        pendingApproval(TOOL_NAME, "BUILD");

        when(continuationStarter.startContinuation(any(), any(), anyLong(), anyString())).thenReturn(Optional.empty());

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, false, null);

        assertThat(resolution.continuationStarted()).isFalse();
        assertThat(resolution.runId()).isNull();
        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.REJECTED);
    }

    @Test
    void testContinuationTurnIsDispatchedAsTheRequester() {
        pendingApproval(TOOL_NAME, "BUILD");

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("someone-else", "n/a", List.of()));

        List<String> dispatchLogins = new ArrayList<>();

        when(continuationStarter.startContinuation(any(), any(), anyLong(), anyString())).thenAnswer(invocation -> {
            dispatchLogins.add(SecurityUtils.getCurrentUserLogin());

            return Optional.of(RUN_ID);
        });

        facade.resolve(WORKSPACE_ID, 99L, false, null);

        assertThat(dispatchLogins).containsExactly(OWNER_LOGIN);
    }

    @Test
    void testRequesterThatCannotBeResolvedFailsClosed() {
        pendingApproval(TOOL_NAME, "BUILD");

        when(userService.fetchUser(CHAT_OWNER_ID)).thenReturn(Optional.empty());

        AiHubToolApprovalFacade.Resolution resolution = facade.resolve(WORKSPACE_ID, 99L, true, null);

        assertThat(resolution.approval()
            .getStatus()).isEqualTo(AiHubToolApproval.Status.FAILED);
        assertThat(resolution.continuationStarted()).isFalse();

        verify(gatedDelegate, never()).call(anyString(), any());
        verify(continuationStarter, never()).startContinuation(any(), any(), anyLong(), anyString());
    }

    @Test
    void testListRequiresAViewableChat() {
        AiHubToolApproval approval = new AiHubToolApproval();

        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, CHAT_OWNER_ID)).thenReturn(chat);
        when(approvalService.list(CHAT_ID)).thenReturn(List.of(approval));

        assertThat(facade.list(WORKSPACE_ID, CHAT_ID)).containsExactly(approval);

        when(chatService.getViewable(CHAT_ID, 8L, CHAT_OWNER_ID))
            .thenThrow(new NotFoundException("AiHubChat not found"));

        assertThatThrownBy(() -> facade.list(8L, CHAT_ID)).isInstanceOf(NotFoundException.class);
    }

    private AiHubToolApprovalFacadeImpl newFacade(
        AiHubPinnedToolCallbacks pinnedToolCallbacksSource, AiHubContinuationStarter continuationStarterSource) {

        return new AiHubToolApprovalFacadeImpl(
            approvalService, chatService, userService, provider(pinnedToolCallbacksSource),
            provider(continuationStarterSource), securityContextRehydrator, transactionManager, clock);
    }

    private AiHubToolApproval pendingApproval(String toolName, String mode) {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setId(99L);
        approval.setChatId(CHAT_ID);
        approval.setThreadId(THREAD_ID);
        approval.setRequestedByUserId(CHAT_OWNER_ID);
        approval.setToolKind(AiHubToolApproval.ToolKind.CATALOG);
        approval.setToolName(toolName);
        approval.setArguments("{\"projectDeploymentId\":5}");
        approval.setStatus(AiHubToolApproval.Status.PENDING);
        approval.setMode(mode);
        approval.setEnvironment(0);
        approval.setExpiresAt(Instant.now(clock)
            .plusSeconds(3600));

        when(approvalService.get(99L)).thenReturn(approval);

        return approval;
    }

    private String continuationMessage() {
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(continuationStarter).startContinuation(any(), eq(chat), eq(WORKSPACE_ID), messageCaptor.capture());

        return messageCaptor.getValue();
    }

    private static ToolCallback callback(String name) {
        ToolDefinition toolDefinition = mock(ToolDefinition.class);

        when(toolDefinition.name()).thenReturn(name);

        ToolCallback callback = mock(ToolCallback.class);

        when(callback.getToolDefinition()).thenReturn(toolDefinition);

        return callback;
    }

    private static User user(long id, String login) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn(login);
        when(user.getAuthorityIds()).thenReturn(List.of());

        return user;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);

        return objectProvider;
    }

    private static PlatformTransactionManager noopTransactionManager() {
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        return manager;
    }
}
