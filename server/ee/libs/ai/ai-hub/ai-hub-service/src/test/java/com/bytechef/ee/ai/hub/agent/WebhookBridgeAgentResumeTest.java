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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
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
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * Tests for the HTTP resume path in {@link WebhookBridgeAgent#run}. The resume path is the most fragile codepath in the
 * bridge (JSON parsing, HTTP timeouts, response shape variants), and the JDK's {@link HttpServer} gives us a loopback
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
 * failure on one of these paths still clears the slot, verified explicitly in
 * {@link #testResumeUrlIsConsumedBeforeHttpCall}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WebhookBridgeAgentResumeTest {

    private static final String THREAD_ID = "00000000-0000-0000-0000-00000000004e";
    private static final long CHAT_ID = 11L;

    private WebhookWorkflowExecutor webhookFacade;
    private AiHubChatService chatService;
    private WebhookResumeRegistry resumeRegistry;
    private JsonMapper jsonMapper;
    private AgentSubscriber subscriber;
    private SessionRepository sessionRepository;
    private JobFacade jobFacade;
    private InFlightAiHubRunRegistry inFlightRunRegistry;

    private HttpServer httpServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        webhookFacade = mock(WebhookWorkflowExecutor.class);
        chatService = mock(AiHubChatService.class);
        resumeRegistry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        jsonMapper = JsonMapper.builder()
            .build();
        subscriber = mock(AgentSubscriber.class);
        sessionRepository = InMemorySessionRepository.builder()
            .build();
        jobFacade = mock(JobFacade.class);
        inFlightRunRegistry = new InFlightAiHubRunRegistry();

        InetAddress loopback = InetAddress.getLoopbackAddress();

        httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);

        httpServer.start();

        baseUrl = "http://" + loopback.getHostAddress() + ":" + httpServer.getAddress()
            .getPort();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
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

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("My answer is 42")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber, timeout(5000)).onTextMessageContentEvent(contentCaptor.capture());

        assertThat(contentCaptor.getValue()
            .getDelta()).isEqualTo("Got your answer.");
        assertThat(capturedRequestBody.get())
            .as("Resume body must include the user's message, the chat's threadId and its conversation id")
            .contains("My answer is 42")
            .contains("\"chatId\":\"" + THREAD_ID + "\"")
            .contains("\"conversationId\":\"workflow-chat:" + THREAD_ID + "\"");
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

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(5000)).onRunErrorEvent(errorCaptor.capture());

        assertThat(errorCaptor.getValue()
            .getError())
                .contains("Workflow resume failed")
                .contains("500");
    }

    @Test
    void testResumeWithNonJsonBodyFallsBackToRawText() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "plain text reply".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber, timeout(5000)).onTextMessageContentEvent(contentCaptor.capture());

        assertThat(contentCaptor.getValue()
            .getDelta()).isEqualTo("plain text reply");
    }

    @Test
    void testStreamingResumeEmitsTextChunksAsTheyArrive() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "data: First chunk\n\ndata: Second chunk\n\n".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber, timeout(5000).times(2)).onTextMessageContentEvent(contentCaptor.capture());

        List<String> deltas = contentCaptor.getAllValues()
            .stream()
            .map(TextMessageContentEvent::getDelta)
            .toList();

        assertThat(deltas).containsExactly("First chunk", "Second chunk");
    }

    @Test
    void testStreamingResumeDecodesJsonEncodedTextChunks() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "data:\"Great\"\n\ndata:\" choice, Lis\"\n\ndata:\"bon\"\n\ndata:42\n\n"
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("Lisbon")), subscriber);

        ArgumentCaptor<TextMessageContentEvent> contentCaptor = ArgumentCaptor.forClass(TextMessageContentEvent.class);

        verify(subscriber, timeout(5000).times(4)).onTextMessageContentEvent(contentCaptor.capture());

        String text = contentCaptor.getAllValues()
            .stream()
            .map(TextMessageContentEvent::getDelta)
            .collect(Collectors.joining());

        assertThat(text).isEqualTo("Great choice, Lisbon42");
    }

    @Test
    void testStreamingResumeReplyIsPersistedWithTheUserMessage() throws AGUIException {
        httpServer.createContext("/resume", exchange -> {
            byte[] response = "data: First chunk\n\ndata: Second chunk\n\n".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("My answer is 42")), subscriber);

        verify(subscriber, timeout(5000)).onRunFinalized(any());

        List<Message> sessionMessages = sessionRepository.findEvents(THREAD_ID, EventFilter.all())
            .stream()
            .map(SessionEvent::getMessage)
            .toList();

        assertThat(sessionMessages)
            .extracting(Message::getMessageType, Message::getText)
            .containsExactly(
                tuple(MessageType.USER, "My answer is 42"), tuple(MessageType.ASSISTANT, "First chunkSecond chunk"));
    }

    @Test
    void testStreamingResumeWithJsonDataParsesAsMapEvent() throws AGUIException {
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

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

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

        AiHubChat chat = newWorkflowChat();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("ping")), subscriber);

        verify(subscriber, timeout(5000)).onTextMessageContentEvent(any(TextMessageContentEvent.class));

        assertThat(resumeRegistry.consume(THREAD_ID))
            .as("Resume URL must be consumed before the HTTP call so duplicates start fresh executions")
            .isNull();
    }

    @Test
    void testResumeRequestIsSentOverHttp11WithoutAnUpgradeHeader() throws AGUIException {
        AtomicReference<String> upgradeHeader = new AtomicReference<>();

        httpServer.createContext("/resume", exchange -> {
            upgradeHeader.set(
                exchange.getRequestHeaders()
                    .getFirst("Upgrade"));

            byte[] response = "{\"message\":\"done\"}".getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody()
                .write(response);
            exchange.close();
        });

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(newWorkflowChat()));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        newAgent().runAgent(parametersOf(buildInput("hello")), subscriber);

        verify(subscriber, timeout(5000)).onRunFinalized(any());
        assertThat(upgradeHeader.get()).isNull();
    }

    @Test
    void testStopBeforeTheResumedJobIdArrivesStopsTheJobOnce() throws AGUIException {
        Sinks.Empty<Void> stopSink = givenStopSink();

        stopSink.tryEmitEmpty();

        CountDownLatch streamReleaseLatch = new CountDownLatch(1);

        createStreamingResumeContext(streamReleaseLatch);

        streamReleaseLatch.countDown();

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(newWorkflowChat()));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        newAgent().runAgent(parametersOf(buildInput("ping")), subscriber);

        verify(subscriber, timeout(5000)).onRunFinalized(any());
        verify(jobFacade, times(1)).stopJob(77L);
    }

    @Test
    void testStopAfterTheResumedJobIdArrivedStopsTheJobOnce() throws AGUIException {
        Sinks.Empty<Void> stopSink = givenStopSink();
        CountDownLatch streamReleaseLatch = new CountDownLatch(1);

        createStreamingResumeContext(streamReleaseLatch);

        when(chatService.findByThreadId(THREAD_ID)).thenReturn(Optional.of(newWorkflowChat()));

        resumeRegistry.register(THREAD_ID, baseUrl + "/resume");

        newAgent().runAgent(parametersOf(buildInput("ping")), subscriber);

        verify(subscriber, timeout(5000)).onTextMessageContentEvent(any(TextMessageContentEvent.class));
        verify(jobFacade, never()).stopJob(anyLong());

        stopSink.tryEmitEmpty();

        verify(jobFacade).stopJob(77L);

        streamReleaseLatch.countDown();

        verify(subscriber, timeout(5000)).onRunFinalized(any());
        verify(jobFacade, times(1)).stopJob(77L);
    }

    private Sinks.Empty<Void> givenStopSink() {
        Sinks.Empty<Void> stopSink = Sinks.empty();

        inFlightRunRegistry = mock(InFlightAiHubRunRegistry.class);

        when(inFlightRunRegistry.stopRequested(THREAD_ID)).thenReturn(stopSink.asMono());

        return stopSink;
    }

    /**
     * Serves an SSE resume response that reports job 77 and a first chunk, then holds the stream open until
     * {@code streamReleaseLatch} opens.
     */
    private void createStreamingResumeContext(CountDownLatch streamReleaseLatch) {
        httpServer.createContext("/resume", exchange -> {
            exchange.getResponseHeaders()
                .set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(
                    "data: {\"event\":\"start\",\"payload\":{\"jobId\":77}}\n\ndata: First chunk\n\n"
                        .getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                if (!streamReleaseLatch.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("Stream release latch timed out");
                }

                outputStream.write("data: Last chunk\n\n".getBytes(StandardCharsets.UTF_8));
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();
            }
        });
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private WebhookBridgeAgent newAgent() throws AGUIException {
        WorkflowChatGuard guard = mock(WorkflowChatGuard.class);

        when(guard.tryAdmit(anyString())).thenReturn(WorkflowChatGuard.AdmissionResult.admit());

        return new WebhookBridgeAgent(
            webhookFacade, chatService, resumeRegistry, jsonMapper, mock(WorkflowChatMetrics.class),
            new AiHubSessionMemory(sessionRepository, null),
            guard, jobFacade, inFlightRunRegistry);
    }

    private static AiHubChat newWorkflowChat() {
        AiHubChat chat = new AiHubChat(3L);

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setKind(AiHubChatKind.WORKFLOW_CHAT);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setTitle("Resume Test");
        chat.setProjectDeploymentId(99L);
        chat.setWorkflowExecutionId(
            WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "uuid-123", "newChatRequest")
                .toString());

        return chat;
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
