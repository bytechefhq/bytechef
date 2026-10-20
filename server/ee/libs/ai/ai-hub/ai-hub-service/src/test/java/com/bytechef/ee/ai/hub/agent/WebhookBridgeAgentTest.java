/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.RunFinishedEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.AssistantMessage;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.spring.AgUiParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link WebhookBridgeAgent}: routing decisions, the request it hands to the workflow, the user-facing
 * error strings, and the Stop wiring.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookBridgeAgentTest {

    private static final String THREAD_ID = "00000000-0000-0000-0000-00000000004e";
    private static final String RUN_ID = "run-1";
    private static final long CHAT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_ID = 99L;
    private static final String FALLBACK_REPLY_FRAGMENT = "This workflow finished but produced no chat reply.";

    private WebhookWorkflowExecutor executor;
    private AiHubChatService chatService;
    private WebhookResumeRegistry resumeRegistry;
    private WorkflowChatGuard guard;
    private WorkflowChatMetrics metrics;
    private JobFacade jobFacade;
    private InFlightAiHubRunRegistry inFlightRunRegistry;
    private SessionRepository sessionRepository;
    private AgentSubscriber subscriber;

    @BeforeEach
    void setUp() {
        executor = mock(WebhookWorkflowExecutor.class);
        chatService = mock(AiHubChatService.class);
        resumeRegistry = new WebhookResumeRegistry(new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        guard = mock(WorkflowChatGuard.class);
        metrics = mock(WorkflowChatMetrics.class);
        jobFacade = mock(JobFacade.class);
        inFlightRunRegistry = new InFlightAiHubRunRegistry();
        sessionRepository = InMemorySessionRepository.builder()
            .build();
        subscriber = mock(AgentSubscriber.class);

        when(guard.tryAdmit(anyString())).thenReturn(WorkflowChatGuard.AdmissionResult.admit());
    }

    @Test
    void testEmitsErrorWhenChatNotFound() throws AGUIException {
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        assertThat(captureRunError()).contains("AiHubChat not found");
        verify(subscriber, timeout(2000)).onRunFinalized(any());
    }

    @Test
    void testEmitsRoutingMisrouteErrorForStandardChat() throws AGUIException {
        givenChat(newChat(AiHubChatKind.STANDARD, "title"));

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        assertThat(captureRunError()).contains("kind mismatch");
        verify(executor, never()).executeSync(any(), any());
    }

    @Test
    void testDisabledWorkflowEndsWithARunErrorAndFreesTheChat() throws AGUIException {
        givenChat(newChat(AiHubChatKind.WORKFLOW_CHAT, "My Helpful Bot"));

        when(executor.isWorkflowDisabled(any())).thenReturn(true);

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        assertThat(captureRunError())
            .contains("disabled")
            .contains("My Helpful Bot")
            .contains("Re-enable");
        verify(subscriber, timeout(2000)).onRunFinalized(any());
        verifyNoInteractions(jobFacade);
        verify(executor, never()).executeSync(any(), any());
        verify(executor, never()).stream(any(), any(), any());
        verify(metrics).recordUnreachable("disabled");
    }

    @Test
    void testEmitsFriendlyDeletedWorkflowMessageWhenLookupThrows() throws AGUIException {
        givenChat(newChat(AiHubChatKind.WORKFLOW_CHAT, "Stale Chat"));

        when(executor.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenThrow(new RuntimeException("workflow not found"));

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        assertThat(captureRunError())
            .contains("can no longer be reached")
            .contains("Stale Chat")
            .contains("Archive this chat");
        verify(metrics).recordUnreachable("deleted");
    }

    @Test
    void testUnresolvableWorkflowExecutionIdEndsWithARunError() throws AGUIException {
        AiHubChat chat = newChat(AiHubChatKind.WORKFLOW_CHAT, "Chat");

        chat.setWorkflowExecutionId("garbage");

        givenChat(chat);

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        assertThat(captureRunError()).contains("Failed to parse workflow execution id");
        verify(subscriber, timeout(2000)).onRunFinalized(any());
        verifyNoInteractions(executor, jobFacade, guard);
        verify(subscriber, never()).onNewMessage(any());
        verify(subscriber, never()).onRunFinishedEvent(any());
    }

    @Test
    void testRateLimitedTurnEndsWithTheGuardsMessage() throws AGUIException {
        givenChat(newChat(AiHubChatKind.WORKFLOW_CHAT, "Chat"));

        when(guard.tryAdmit(THREAD_ID))
            .thenReturn(WorkflowChatGuard.AdmissionResult.rateLimited(Duration.ofMillis(500)));

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        assertThat(captureRunError()).contains("too quickly");
        verifyNoInteractions(executor);
        verify(metrics).recordTurn("rate_limited");
        verify(metrics).recordTurnByWorkspace("rate_limited", 5L);
        verify(chatService, never()).getWorkspaceId(anyLong());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRequestBodyCarriesConversationIdAndChatId() throws AGUIException {
        givenSyncChat();

        when(executor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(Map.of("message", "ok"));

        newAgent().runAgent(parametersOf(buildInput("Hello")), subscriber);

        ArgumentCaptor<WebhookRequest> requestCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(executor, timeout(2000)).executeSync(any(WorkflowExecutionId.class), requestCaptor.capture());

        WebhookRequest captured = requestCaptor.getValue();

        WebhookRequest.WebhookBodyImpl body = captured.body();

        Map<String, Object> content = (Map<String, Object>) body.getContent();

        assertThat(content)
            .containsEntry("message", "Hello")
            .containsEntry("chatId", THREAD_ID)
            .containsEntry("conversationId", "workflow-chat:" + THREAD_ID)
            .containsEntry("attachments", List.of());
        assertThat(captured.parameters())
            .containsEntry("message", List.of("Hello"))
            .containsEntry("chatId", List.of(THREAD_ID))
            .containsEntry("conversationId", List.of("workflow-chat:" + THREAD_ID));
    }

    @Test
    void testTriggerInstanceNameDoesNotLogANonChatTriggerMessage() throws AGUIException {
        AiHubChat chat = newChat(AiHubChatKind.WORKFLOW_CHAT, "Chat");
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "uuid-123", "trigger_1");

        chat.setWorkflowExecutionId(workflowExecutionId.toString());

        givenChat(chat);

        when(executor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(executor.executeSync(any(), any())).thenReturn(Map.of("message", "done"));

        Logger bridgeLogger = (Logger) LoggerFactory.getLogger(WebhookBridgeAgent.class);
        Level originalLevel = bridgeLogger.getLevel();
        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        logAppender.start();
        bridgeLogger.addAppender(logAppender);
        bridgeLogger.setLevel(Level.DEBUG);

        try {
            newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

            assertThat(captureAssistantReply()).isEqualTo("done");
            verify(subscriber, timeout(2000)).onRunFinalized(any());
        } finally {
            bridgeLogger.setLevel(originalLevel);
            bridgeLogger.detachAppender(logAppender);
        }

        assertThat(logAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .noneMatch(message -> message.contains("non-chat trigger"));
    }

    @Test
    void testSyncPathCallsExecuteSyncDirectly() throws AGUIException {
        givenSyncChat();

        when(executor.executeSync(any(), any())).thenReturn(Map.of("message", "done"));

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureAssistantReply()).isEqualTo("done");
        verify(subscriber, timeout(2000)).onRunFinishedEvent(any(RunFinishedEvent.class));
        verify(subscriber, timeout(2000)).onRunFinalized(any());
        verify(subscriber, never()).onRunErrorEvent(any());
        verify(metrics).recordTurn("sync");
    }

    @Test
    void testSyncExecutionFailureEndsWithARunError() throws AGUIException {
        givenSyncChat();

        when(executor.executeSync(any(), any())).thenThrow(new IllegalStateException("Workflow blew up"));

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureRunError()).isEqualTo("Workflow blew up");
        verify(subscriber, timeout(2000)).onRunFinalized(any());
    }

    @Test
    void testWebhookResponseEnvelopeUnwrapsBodyMessage() throws AGUIException {
        givenSyncChat();

        Map<String, Object> webhookResponseEnvelope = Map.of(
            "type", "JSON",
            "body", Map.of("message", "Hello!", "attachments", List.of()),
            "headers", Map.of(),
            "statusCode", 200);

        when(executor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponseEnvelope));

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureAssistantReply()).isEqualTo("Hello!");
    }

    @Test
    void testWebhookResponseEnvelopeWithStringBody() throws AGUIException {
        givenSyncChat();

        Map<String, Object> webhookResponseEnvelope = Map.of(
            "type", "RAW",
            "body", "Plain text reply",
            "headers", Map.of(),
            "statusCode", 200);

        when(executor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponseEnvelope));

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureAssistantReply()).isEqualTo("Plain text reply");
    }

    @Test
    void testTopLevelMessageMapStillEmitsAssistantText() throws AGUIException {
        givenSyncChat();

        when(executor.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(Map.of("message", "Top-level reply"));

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureAssistantReply()).isEqualTo("Top-level reply");
    }

    @Test
    void testEmptyOutputCompletesWithTheFallbackReply() throws AGUIException {
        givenSyncChat();

        when(executor.executeSync(any(), any())).thenReturn(Map.of());

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        String reply = captureAssistantReply();

        assertThat(reply)
            .startsWith(FALLBACK_REPLY_FRAGMENT)
            .contains("Response to Chat Request");
        verify(subscriber, timeout(2000)).onRunFinalized(any());
        verify(subscriber, never()).onRunErrorEvent(any());
        verify(subscriber, never()).onRunFailed(any(), any());
        assertThat(sessionMessages())
            .extracting(Message::getMessageType, Message::getText)
            .containsExactly(tuple(MessageType.USER, "hi"), tuple(MessageType.ASSISTANT, reply));
    }

    @Test
    void testStreamingPathPersistsTheAccumulatedReplyOnFinalize() throws AGUIException {
        givenStreamingChat();

        ArgumentCaptor<SseStreamBridge> bridgeCaptor = ArgumentCaptor.forClass(SseStreamBridge.class);

        newAgent().runAgent(parametersOf(buildInput("stream please")), subscriber);

        verify(executor, timeout(2000)).stream(any(), any(), bridgeCaptor.capture());

        SseStreamBridge bridge = bridgeCaptor.getValue();

        bridge.onEvent("Hello ");
        bridge.onEvent("world");
        bridge.onComplete();

        verify(subscriber, timeout(2000)).onRunFinalized(any());
        verify(metrics).recordTurn("streaming");
        assertThat(sessionMessages())
            .extracting(Message::getText)
            .containsExactly("stream please", "Hello world");
    }

    @Test
    void testStreamStartFailureEndsWithARunError() throws AGUIException {
        givenStreamingChat();

        when(executor.stream(any(), any(), any())).thenThrow(new IllegalStateException("broker down"));

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureRunError()).isEqualTo("broker down");
        verify(subscriber, timeout(2000)).onRunFinalized(any());
    }

    @Test
    void testStopSignalReachesTheStopper() throws AGUIException {
        inFlightRunRegistry = mock(InFlightAiHubRunRegistry.class);

        Sinks.One<Void> stopSink = Sinks.one();

        when(inFlightRunRegistry.stopRequested(THREAD_ID)).thenReturn(stopSink.asMono());

        givenStreamingChat();

        ArgumentCaptor<SseStreamBridge> bridgeCaptor = ArgumentCaptor.forClass(SseStreamBridge.class);

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        verify(executor, timeout(2000)).stream(any(), any(), bridgeCaptor.capture());

        SseStreamBridge bridge = bridgeCaptor.getValue();

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 77L)));

        verify(jobFacade, never()).stopJob(anyLong());

        stopSink.tryEmitEmpty();

        verify(jobFacade).stopJob(77L);
    }

    @Test
    void testStopSignalStopsTheJobUnderTheTurnsTenant() throws AGUIException {
        inFlightRunRegistry = mock(InFlightAiHubRunRegistry.class);

        Sinks.One<Void> stopSink = Sinks.one();

        when(inFlightRunRegistry.stopRequested(THREAD_ID)).thenReturn(stopSink.asMono());

        AtomicReference<String> stopJobTenantId = new AtomicReference<>();

        doAnswer(invocation -> {
            stopJobTenantId.set(TenantContext.getCurrentTenantId());

            return null;
        }).when(jobFacade)
            .stopJob(77L);

        givenStreamingChat();

        ArgumentCaptor<SseStreamBridge> bridgeCaptor = ArgumentCaptor.forClass(SseStreamBridge.class);

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, "acme");

        newAgent().runAgent(parametersOf(buildInput("hi"), state), subscriber);

        verify(executor, timeout(2000)).stream(any(), any(), bridgeCaptor.capture());

        SseStreamBridge bridge = bridgeCaptor.getValue();

        TenantContext.resetCurrentTenantId();

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 77L)));
        stopSink.tryEmitEmpty();

        verify(jobFacade).stopJob(77L);
        assertThat(stopJobTenantId.get()).isEqualTo("acme");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testStreamErrorStopsOnlyTheTurnsOwnJob() throws AGUIException {
        givenStreamingChat();

        ArgumentCaptor<SseStreamBridge> bridgeCaptor = ArgumentCaptor.forClass(SseStreamBridge.class);

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        verify(executor, timeout(2000)).stream(any(), any(), bridgeCaptor.capture());

        SseStreamBridge bridge = bridgeCaptor.getValue();

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 77L)));
        bridge.onError(new IllegalStateException("workflow failed"));

        verify(jobFacade).stopJob(77L);
        verify(jobFacade, never()).stopJob(longThat(jobId -> jobId != 77L));
        assertThat(captureRunError()).isEqualTo("workflow failed");
    }

    @Test
    void testStreamErrorBeforeAnyJobIdStopsNothing() throws AGUIException {
        givenStreamingChat();

        ArgumentCaptor<SseStreamBridge> bridgeCaptor = ArgumentCaptor.forClass(SseStreamBridge.class);

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        verify(executor, timeout(2000)).stream(any(), any(), bridgeCaptor.capture());

        SseStreamBridge bridge = bridgeCaptor.getValue();

        bridge.onError(new IllegalStateException("workflow failed"));

        verifyNoInteractions(jobFacade);
    }

    @Test
    void testResumeLinkOfAnotherTenantsChatWithTheSameIdIsNotConsumed() throws AGUIException {
        givenSyncChat();

        when(executor.executeSync(any(), any())).thenReturn(Map.of("message", "fresh run"));

        String otherTenantThreadId = "a1b2c3d4-0000-4000-8000-0000000000ff";

        resumeRegistry.register(otherTenantThreadId, "https://tenant-a.example.com/resume");

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        assertThat(captureAssistantReply()).isEqualTo("fresh run");
        verify(executor).executeSync(any(), any());
        verify(metrics, never()).recordTurn("resume");
        assertThat(resumeRegistry.consume(otherTenantThreadId)).isEqualTo("https://tenant-a.example.com/resume");
    }

    @Test
    void testStopSubscriptionIsDisposedWhenTheTurnFinalizes() throws AGUIException {
        inFlightRunRegistry = mock(InFlightAiHubRunRegistry.class);

        Sinks.One<Void> stopSink = Sinks.one();

        when(inFlightRunRegistry.stopRequested(THREAD_ID)).thenReturn(stopSink.asMono());

        givenStreamingChat();

        ArgumentCaptor<SseStreamBridge> bridgeCaptor = ArgumentCaptor.forClass(SseStreamBridge.class);

        newAgent().runAgent(parametersOf(buildInput("hi")), subscriber);

        verify(executor, timeout(2000)).stream(any(), any(), bridgeCaptor.capture());

        SseStreamBridge bridge = bridgeCaptor.getValue();

        bridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", 77L)));
        bridge.onComplete();

        assertThat(stopSink.currentSubscriberCount()).isZero();

        stopSink.tryEmitEmpty();

        verify(jobFacade, never()).stopJob(anyLong());
    }

    @Test
    void testFinishedTurnReleasesTheThreadInTheRunRegistry() throws AGUIException, InterruptedException {
        givenSyncChat();

        when(executor.executeSync(any(), any())).thenReturn(Map.of("message", "done"));

        InFlightAiHubRunRegistry.InFlightRun run = inFlightRunRegistry.tryStart(THREAD_ID, RUN_ID)
            .orElseThrow();

        CountDownLatch releasedLatch = new CountDownLatch(1);

        inFlightRunRegistry.runWhenReleased(THREAD_ID, releasedLatch::countDown);

        AiHubChatStreamer streamer = new AiHubChatStreamer(inFlightRunRegistry, new JsonMapper());

        streamer.start(newAgent(), buildAgUiParameters("hi"), run);

        assertThat(releasedLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(inFlightRunRegistry.isInFlight(THREAD_ID)).isFalse();
    }

    @Test
    void testResumeRequestIsSentOverHttp11WithoutAnUpgradeHeader() throws Exception {
        givenSyncChat();

        AtomicReference<String> upgradeHeader = new AtomicReference<>();
        CountDownLatch requestLatch = new CountDownLatch(1);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/resume", exchange -> {
            upgradeHeader.set(
                exchange.getRequestHeaders()
                    .getFirst("Upgrade"));

            byte[] responseBody = "{\"message\":\"done\"}".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBody.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }

            requestLatch.countDown();
        });

        httpServer.start();

        try {
            int port = httpServer.getAddress()
                .getPort();

            resumeRegistry.register(THREAD_ID, "http://127.0.0.1:" + port + "/resume");

            newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

            assertThat(requestLatch.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(upgradeHeader.get()).isNull();
        } finally {
            httpServer.stop(0);
        }
    }

    private WebhookBridgeAgent newAgent() throws AGUIException {
        return new WebhookBridgeAgent(
            executor, chatService, resumeRegistry, JsonMapper.builder()
                .build(),
            metrics, new AiHubSessionMemory(sessionRepository, null), guard, jobFacade,
            inFlightRunRegistry);
    }

    private void givenChat(AiHubChat chat) {
        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));
    }

    private void givenSyncChat() {
        givenChat(newChat(AiHubChatKind.WORKFLOW_CHAT, "Chat"));

        when(executor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
    }

    private void givenStreamingChat() {
        givenChat(newChat(AiHubChatKind.WORKFLOW_CHAT, "Chat"));

        when(executor.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(executor.hasStreamingTask(any(WorkflowExecutionId.class))).thenReturn(true);
        when(executor.stream(any(), any(), any())).thenReturn(new CompletableFuture<>());
    }

    private String captureRunError() {
        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(2000)).onRunErrorEvent(errorCaptor.capture());

        RunErrorEvent runErrorEvent = errorCaptor.getValue();

        return runErrorEvent.getError();
    }

    private String captureAssistantReply() {
        ArgumentCaptor<AssistantMessage> assistantCaptor = ArgumentCaptor.forClass(AssistantMessage.class);

        verify(subscriber, timeout(2000)).onNewMessage(assistantCaptor.capture());

        AssistantMessage assistantMessage = assistantCaptor.getValue();

        return assistantMessage.getContent();
    }

    private List<Message> sessionMessages() {
        return sessionRepository.findEvents(THREAD_ID, EventFilter.all())
            .stream()
            .map(SessionEvent::getMessage)
            .toList();
    }

    private static AiHubChat newChat(AiHubChatKind kind, String title) {
        AiHubChat chat = new AiHubChat(3L);

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setKind(kind);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setTitle(title);
        chat.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);
        chat.setWorkspaceId(5L);

        if (kind == AiHubChatKind.WORKFLOW_CHAT) {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, 1L, "uuid-123", "newChatRequest");

            chat.setWorkflowExecutionId(workflowExecutionId.toString());
        }

        return chat;
    }

    private static RunAgentInput buildInput(String userMessageText) {
        UserMessage userMessage = new UserMessage();

        userMessage.setContent(userMessageText);

        return new RunAgentInput(
            THREAD_ID, RUN_ID, new State(), List.of((BaseMessage) userMessage), List.of(), List.of(), null);
    }

    private static AgUiParameters buildAgUiParameters(String userMessageText) {
        AgUiParameters agUiParameters = new AgUiParameters();
        RunAgentInput input = buildInput(userMessageText);

        agUiParameters.setThreadId(THREAD_ID);
        agUiParameters.setRunId(RUN_ID);
        agUiParameters.setState(new State());
        agUiParameters.setMessages(input.messages());

        return agUiParameters;
    }

    private static RunAgentParameters parametersOf(RunAgentInput input) {
        return parametersOf(input, new State());
    }

    private static RunAgentParameters parametersOf(RunAgentInput input, State state) {
        return RunAgentParameters.builder()
            .threadId(input.threadId())
            .state(state)
            .runId(input.runId())
            .messages(input.messages())
            .forwardedProps(input.forwardedProps())
            .build();
    }
}
