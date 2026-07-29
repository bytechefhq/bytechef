/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentParameters;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.event.TextMessageContentEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.task.AiHubTaskStatus;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for the HTTP resume path in {@link WebhookBridgeAgent#run}. The resume path is the most fragile codepath in the
 * bridge — JSON parsing, HTTP timeouts, response shape variants — and the JDK's {@link HttpServer} gives us a loopback
 * test target with no extra dependencies. These tests pin:
 *
 * <ul>
 * <li>A successful resume with a JSON body shaped like {@code {message: "..."}} streams the message back through the
 * bridge as a text-message-content event.</li>
 * <li>A non-2xx response surfaces as a {@code RUN_ERROR} naming the status code (so ops can correlate with logs).</li>
 * <li>A non-JSON response body falls through to the raw-body path rather than failing the whole turn.</li>
 * </ul>
 *
 * <p>
 * Resume URLs are <em>consumed</em> from the registry (atomic remove-and-return) BEFORE the HTTP call fires, so a
 * failure on one of these paths still clears the slot — verified explicitly in
 * {@link #testResumeUrlIsConsumedBeforeHttpCall}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookBridgeAgentResumeTest {

    private static final String THREAD_ID = "00000000-0000-0000-0000-00000000004e";
    private static final long TASK_ID = 11L;

    private WebhookWorkflowExecutor webhookFacade;
    private AiHubTaskService taskService;
    private WebhookResumeRegistry resumeRegistry;
    private JsonMapper jsonMapper;
    private AgentSubscriber subscriber;
    private AssetFileFacade assetFileFacade;

    private HttpServer httpServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        webhookFacade = mock(WebhookWorkflowExecutor.class);
        taskService = mock(AiHubTaskService.class);
        resumeRegistry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        jsonMapper = JsonMapper.builder()
            .build();
        subscriber = mock(AgentSubscriber.class);
        assetFileFacade = mock(AssetFileFacade.class);

        // Bind to ephemeral port on the loopback so concurrent test runs don't collide. Using
        // InetAddress.getLoopbackAddress() rather than a hardcoded "127.0.0.1" string keeps PMD's
        // AvoidUsingHardCodedIP rule happy and works on IPv6-only test environments without translation.
        InetAddress loopback = InetAddress.getLoopbackAddress();

        httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);

        httpServer.start();

        baseUrl = "http://" + loopback.getHostAddress() + ":" + httpServer.getAddress()
            .getPort();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            // stop(0) terminates immediately even if there are in-flight requests — test cleanup, no graceful drain.
            httpServer.stop(0);
        }
    }

    @Test
    void testResumeWithSuccessfulJsonResponseStreamsAssistantMessage() throws AGUIException {
        AtomicReference<String> capturedRequestBody = new AtomicReference<>();

        httpServer.createContext("/resume", exchange -> {
            capturedRequestBody.set(readBody(exchange));

            byte[] response = "{\"message\": \"Got your answer.\"}".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubTask task = newWorkflowChatTask();

        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        // Pre-register the resume URL so the bridge picks the resume path on this turn.
        resumeRegistry.register(TASK_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("My answer is 42")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber, timeout(5000)).onTextMessageContentEvent(contentCaptor.capture());

        assertThat(contentCaptor.getValue()
            .getDelta()).isEqualTo("Got your answer.");
        assertThat(capturedRequestBody.get())
            .as("Resume body must include the user's message and the task's threadId")
            .contains("My answer is 42")
            .contains(THREAD_ID);
    }

    @Test
    void testResumeWithNon2xxStatusEmitsRunError() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "internal error".getBytes(StandardCharsets.UTF_8);

            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubTask task = newWorkflowChatTask();

        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        resumeRegistry.register(TASK_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(5000)).onRunErrorEvent(errorCaptor.capture());

        // The error message must include the status code so an ops investigation can correlate with the receiving
        // server's logs. Without "500" in the message the user sees a generic "Workflow resume failed" with no
        // actionable signal.
        assertThat(errorCaptor.getValue()
            .getError())
                .contains("Workflow resume failed")
                .contains("500");
    }

    @Test
    void testResumeWithNonJsonBodyFallsBackToRawText() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            // Plain text response — not JSON. The bridge's parseResponseBody falls back to the raw string and
            // handleSyncOutputs surfaces it via Objects.toString. Without this fallback, a slightly-misshapen
            // workflow would lose every reply.
            byte[] response = "plain text reply".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubTask task = newWorkflowChatTask();

        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        resumeRegistry.register(TASK_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber, timeout(5000)).onTextMessageContentEvent(contentCaptor.capture());

        assertThat(contentCaptor.getValue()
            .getDelta()).isEqualTo("plain text reply");
    }

    @Test
    void testStreamingResumeEmitsTextChunksAsTheyArrive() throws AGUIException {
        // WC #8: when the resume endpoint advertises text/event-stream, the bridge surfaces each SSE data
        // event through onEvent as it arrives — rather than buffering the whole body and treating it as one
        // sync result. Verify by emitting two distinct chunks and asserting two separate
        // onTextMessageContentEvent calls (one per chunk) instead of one concatenated event.
        httpServer.createContext("/resume", exchange -> {
            // SSE response shape: each event is a "data: ..." line followed by a blank line. The bridge's
            // streamResumeBody parses each line and feeds the data payload to bridge.onEvent.
            byte[] response = "data: First chunk\n\ndata: Second chunk\n\n".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubTask task = newWorkflowChatTask();

        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        resumeRegistry.register(TASK_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        // Two separate content events — pin both. If the bridge regressed to buffering, we'd see one event
        // with concatenated text and the test would fail.
        verify(subscriber, timeout(5000).times(2)).onTextMessageContentEvent(contentCaptor.capture());

        List<String> deltas = contentCaptor.getAllValues()
            .stream()
            .map(TextMessageContentEvent::getDelta)
            .toList();

        assertThat(deltas).containsExactly("First chunk", "Second chunk");
    }

    @Test
    void testStreamingResumeWithJsonDataParsesAsMapEvent() throws AGUIException {
        // SSE data lines that parse as JSON go through bridge.onEvent as Map — same handler the streaming
        // executor's ask_user_question / AI-agent-event branches use. This pin guards the contract: a workflow
        // emitting structured events on resume gets the same routing as one that streamed them on a fresh run.
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "data: {\"event-type\":\"thinking\",\"text\":\"hmm\"}\n\n"
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubTask task = newWorkflowChatTask();

        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        resumeRegistry.register(TASK_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        // The Map-shaped event with event-type=thinking goes through AgUiStreamBridge's routing which emits
        // a CustomEvent (NOT a TextMessageContent). Verify by waiting for any custom event.
        verify(subscriber, timeout(5000)).onCustomEvent(any(com.agui.core.event.CustomEvent.class));
    }

    @Test
    void testResumeUrlIsConsumedBeforeHttpCall() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "{\"message\": \"ok\"}".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubTask task = newWorkflowChatTask();

        when(taskService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(task));

        resumeRegistry.register(TASK_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        // Wait for the turn to complete so we know the bridge has progressed past the consume() call.
        verify(subscriber, timeout(5000)).onTextMessageContentEvent(any(TextMessageContentEvent.class));

        // The atomic consume contract: a duplicate-delivered turn cannot fire the resume POST twice. After the
        // first turn fires the registry slot is empty, so a re-register would have to happen for resume to
        // trigger again.
        assertThat(resumeRegistry.consume(TASK_ID))
            .as("Resume URL must be consumed before the HTTP call so duplicates start fresh executions")
            .isNull();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private WebhookBridgeAgent newAgent() throws AGUIException {
        // Stub the guard to admit every turn — these tests focus on the resume / sync / streaming dispatch logic,
        // not the rate-limit + concurrency gate. The dedicated WorkflowChatGuardTest covers admission outcomes.
        WorkflowChatGuard guard = mock(WorkflowChatGuard.class);

        when(guard.tryAdmit(anyLong())).thenReturn(WorkflowChatGuard.AdmissionResult.admit());

        return new WebhookBridgeAgent(
            webhookFacade, taskService, resumeRegistry, jsonMapper, assetFileFacade,
            mock(com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics.class),
            mock(WorkflowChatJobRegistry.class),
            new com.bytechef.ee.ai.hub.memory.AiHubSessionMemory(
                org.springframework.ai.session.InMemorySessionRepository.builder()
                    .build(),
                null),
            guard, null);
    }

    private static AiHubTask newWorkflowChatTask() {
        AiHubTask task = new AiHubTask(3L);

        task.setId(TASK_ID);
        task.setThreadId(THREAD_ID);
        task.setKind(AiHubTaskKind.WORKFLOW_CHAT);
        task.setStatus(AiHubTaskStatus.ACTIVE);
        task.setTitle("Resume Test");
        task.setProjectDeploymentId(99L);
        task.setWorkflowExecutionId(
            WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "uuid-123", "newChatRequest")
                .toString());

        return task;
    }

    private static com.agui.core.agent.RunAgentInput buildInput(String userMessageText) {
        UserMessage userMessage = new UserMessage();

        userMessage.setContent(userMessageText);

        return new com.agui.core.agent.RunAgentInput(
            THREAD_ID, "run-1", new State(), List.<BaseMessage>of(userMessage), List.of(), List.of(), null);
    }

    private static RunAgentParameters parametersOf(com.agui.core.agent.RunAgentInput input) {
        return RunAgentParameters.builder()
            .threadId(input.threadId())
            .runId(input.runId())
            .messages(input.messages())
            .forwardedProps(input.forwardedProps())
            .build();
    }
}
