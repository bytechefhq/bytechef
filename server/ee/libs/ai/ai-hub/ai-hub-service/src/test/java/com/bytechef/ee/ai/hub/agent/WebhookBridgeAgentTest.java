/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.event.RunErrorEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.AssistantMessage;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link WebhookBridgeAgent} focused on routing decisions and error-message ergonomics. The HTTP resume
 * path is exercised by an integration test (out of scope here) — these tests cover the synchronous decision tree and
 * the user-facing error strings.
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

    private WebhookWorkflowExecutor webhookFacade;
    private AiHubChatService chatService;
    private WebhookResumeRegistry resumeRegistry;
    private JsonMapper jsonMapper;
    private AgentSubscriber subscriber;
    private AssetFileFacade assetFileFacade;

    @BeforeEach
    void setUp() {
        webhookFacade = mock(WebhookWorkflowExecutor.class);
        chatService = mock(AiHubChatService.class);
        resumeRegistry = new WebhookResumeRegistry(
            new ConcurrentMapCacheManager(WebhookResumeRegistry.CACHE_NAME));
        jsonMapper = JsonMapper.builder()
            .build();
        subscriber = mock(AgentSubscriber.class);
        assetFileFacade = mock(AssetFileFacade.class);

        // Default createFromUpload stub: return an AssetFile shaped like the production happy path. Tests that
        // need to assert specific upload arguments override this with a captor.
        when(
            assetFileFacade.createFromUpload(
                anyLong(), anyInt(), anyString(), anyString(), any(java.io.InputStream.class)))
                    .thenAnswer(invocation -> {
                        String filename = invocation.getArgument(2);
                        String contentType = invocation.getArgument(3);

                        AssetFile assetFile = new AssetFile();

                        assetFile.setName(filename);
                        assetFile.setMimeType(contentType);
                        assetFile.setFile(new FileEntry(filename, "file:///workspace/" + filename));

                        return assetFile;
                    });
    }

    @Test
    void testEmitsErrorWhenChatNotFound() throws AGUIException {
        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.empty());

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hello")), subscriber);

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(2000)).onRunErrorEvent(errorCaptor.capture());

        assertThat(errorCaptor.getValue()
            .getError()).contains("AiHubChat not found");
    }

    @Test
    void testEmitsRoutingMisrouteErrorForStandardChat() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.STANDARD, AiHubChatStatus.ACTIVE, "title");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hello")), subscriber);

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(2000)).onRunErrorEvent(errorCaptor.capture());

        assertThat(errorCaptor.getValue()
            .getError()).contains("kind mismatch");
        verify(webhookFacade, never()).executeSync(any(), any());
    }

    @Test
    void testEmitsFriendlyDisabledMessageWithChatTitle() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "My Helpful Bot");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(true);

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hello")), subscriber);

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(2000)).onRunErrorEvent(errorCaptor.capture());

        String errorMessage = errorCaptor.getValue()
            .getError();

        assertThat(errorMessage)
            .contains("disabled")
            .contains("My Helpful Bot")
            .contains("Re-enable");
        verify(webhookFacade, never()).executeSync(any(), any());
    }

    @Test
    void testEmitsFriendlyDeletedWorkflowMessageWhenLookupThrows() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Stale Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenThrow(new RuntimeException("workflow not found"));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hello")), subscriber);

        ArgumentCaptor<RunErrorEvent> errorCaptor = ArgumentCaptor.forClass(RunErrorEvent.class);

        verify(subscriber, timeout(2000)).onRunErrorEvent(errorCaptor.capture());

        String errorMessage = errorCaptor.getValue()
            .getError();

        // The message should distinguish "deleted" from "disabled" so users know the recovery action differs.
        assertThat(errorMessage)
            .contains("can no longer be reached")
            .contains("Stale Chat")
            .contains("Archive this chat");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWebhookRequestBodyIncludesMessageAndAttachments() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(false);
        when(webhookFacade.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(webhookFacade.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Map.of("message", "ok")));

        Map<String, Object> attachment = Map.of(
            "name", "report.pdf",
            "contentType", "application/pdf",
            "base64", "JVBERi0=");
        Map<String, Object> forwardedProps = Map.of("attachments", List.of(attachment));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("Hello", forwardedProps)), subscriber);

        ArgumentCaptor<WebhookRequest> requestCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(webhookFacade, timeout(2000)).executeSync(any(WorkflowExecutionId.class), requestCaptor.capture());

        Map<String, Object> bodyContent = (Map<String, Object>) requestCaptor.getValue()
            .body()
            .getContent();

        assertThat(bodyContent)
            .containsEntry("message", "Hello")
            .containsEntry("chatId", THREAD_ID);

        List<?> attachments = (List<?>) bodyContent.get("attachments");

        assertThat(attachments).hasSize(1);
        // The promotion contract: each attachment must be an SDK FileEntry instance, not a raw map. This is
        // exactly the discriminator ChatNewRequestTrigger.checkMap uses (`list.getFirst() instanceof FileEntry`)
        // to decide whether to surface the list as files or as a single value.
        assertThat(attachments.getFirst())
            .isInstanceOf(com.bytechef.component.definition.FileEntry.class);

        com.bytechef.component.definition.FileEntry fileEntry =
            (com.bytechef.component.definition.FileEntry) attachments.getFirst();

        assertThat(fileEntry.getName()).isEqualTo("report.pdf");
        assertThat(fileEntry.getMimeType()).isEqualTo("application/pdf");
        assertThat(fileEntry.getUrl()).isEqualTo("file:///workspace/report.pdf");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAttachmentsWithoutBase64AreDropped() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(false);
        when(webhookFacade.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(webhookFacade.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Map.of("message", "ok")));

        // Metadata-only attachment with no base64 payload — the bridge should drop it (the workflow can't act
        // on a stub, and forwarding it would put a half-built FileEntry into the trigger). Pin: the trigger
        // sees an empty list, NOT a list with a missing-payload entry that would crash on .getUrl().
        Map<String, Object> stubAttachment = Map.of("name", "metadata-only.pdf", "contentType", "application/pdf");
        Map<String, Object> forwardedProps = Map.of("attachments", List.of(stubAttachment));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("Hi", forwardedProps)), subscriber);

        ArgumentCaptor<WebhookRequest> requestCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(webhookFacade, timeout(2000)).executeSync(any(WorkflowExecutionId.class), requestCaptor.capture());

        Map<String, Object> bodyContent = (Map<String, Object>) requestCaptor.getValue()
            .body()
            .getContent();

        assertThat((List<?>) bodyContent.get("attachments")).isEmpty();
    }

    /**
     * Pins the canonical chat workflow output shape: the {@code chat/responseToRequest} action emits a
     * {@code WebhookResponse}, which the executor stores under {@link MetadataConstants#WEBHOOK_RESPONSE}. The
     * serialized envelope carries {@code {body, headers, statusCode, type}}; for JSON type the body is the user's
     * response map (typically {@code {message, attachments}}). The bridge MUST unwrap this envelope and extract
     * {@code body.message} — otherwise the user sees a blank chat reply even though the workflow ran successfully.
     *
     * <p>
     * Regression context: when the bridge's {@code extractMessage} only checked for a top-level {@code message} field,
     * every chat workflow using the canonical {@code chat/responseToRequest} action surfaced as an empty assistant
     * message. The HTTP controller has always unwrapped {@code __webhookResponse} via {@code processWebhookResponse};
     * the bridge needed the same logic to render the same workflow output.
     * </p>
     */
    @Test
    void testWebhookResponseEnvelopeUnwrapsBodyMessage() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(false);
        when(webhookFacade.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));

        // Production output shape from WebhookWorkflowExecutorImpl.executeSync after a chat/responseToRequest
        // step: the action's WebhookResponse is collected into __webhookResponse, then read back as a map.
        Map<String, Object> webhookResponseEnvelope = Map.of(
            "type", "JSON",
            "body", Map.of("message", "Hello!", "attachments", List.of()),
            "headers", Map.of(),
            "statusCode", 200);
        Map<String, Object> outputs = Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponseEnvelope);

        when(webhookFacade.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(outputs));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hi")), subscriber);

        ArgumentCaptor<AssistantMessage> assistantCaptor = ArgumentCaptor.forClass(AssistantMessage.class);

        verify(subscriber, timeout(2000)).onNewMessage(assistantCaptor.capture());

        assertThat(assistantCaptor.getValue()
            .getContent()).isEqualTo("Hello!");
    }

    /**
     * Companion to {@link #testWebhookResponseEnvelopeUnwrapsBodyMessage}: workflows using {@code WebhookResponse.raw}
     * (plain text reply) wrap a String in the body. Confirms the bridge surfaces the bare String as the assistant reply
     * rather than dropping it.
     */
    @Test
    void testWebhookResponseEnvelopeWithStringBody() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(false);
        when(webhookFacade.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));

        Map<String, Object> webhookResponseEnvelope = Map.of(
            "type", "RAW",
            "body", "Plain text reply",
            "headers", Map.of(),
            "statusCode", 200);
        Map<String, Object> outputs = Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponseEnvelope);

        when(webhookFacade.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(outputs));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hi")), subscriber);

        ArgumentCaptor<AssistantMessage> assistantCaptor = ArgumentCaptor.forClass(AssistantMessage.class);

        verify(subscriber, timeout(2000)).onNewMessage(assistantCaptor.capture());

        assertThat(assistantCaptor.getValue()
            .getContent()).isEqualTo("Plain text reply");
    }

    /**
     * Pins backwards compatibility with workflows that bypass {@code WebhookResponse} and emit a plain map directly
     * (older / minimal workflows, plus the existing mock-shape used by other tests in this file). The bridge must keep
     * the top-level {@code message} branch alongside the new envelope unwrap so neither path regresses the other.
     */
    @Test
    void testTopLevelMessageMapStillEmitsAssistantText() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(false);
        when(webhookFacade.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(webhookFacade.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Map.of("message", "Top-level reply")));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hi")), subscriber);

        ArgumentCaptor<AssistantMessage> assistantCaptor = ArgumentCaptor.forClass(AssistantMessage.class);

        verify(subscriber, timeout(2000)).onNewMessage(assistantCaptor.capture());

        assertThat(assistantCaptor.getValue()
            .getContent()).isEqualTo("Top-level reply");
    }

    /**
     * Pins the empty-outputs fallback path: when a workflow runs to completion with no extractable chat reply (most
     * common cause: no `chat/responseToRequest` step), the bridge MUST surface a clear, actionable hint rather than
     * completing silently. Before this fix, users saw an empty assistant bubble with no diagnostic trail —
     * operationally indistinguishable from a network failure or a stalled SSE stream.
     */
    @Test
    void testEmptyOutputsEmitsActionableFallbackMessage() throws AGUIException {
        AiHubChat chat =
            newChat(AiHubChatKind.WORKFLOW_CHAT, AiHubChatStatus.ACTIVE,
                "Chat");

        when(chatService.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));
        when(webhookFacade.isWorkflowDisabled(any(WorkflowExecutionId.class)))
            .thenReturn(false);
        when(webhookFacade.getWebhookTriggerFlags(any(WorkflowExecutionId.class)))
            .thenReturn(new WebhookTriggerFlags(false, true, false, false));
        when(webhookFacade.executeSync(any(WorkflowExecutionId.class), any(WebhookRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(Map.of()));

        WebhookBridgeAgent agent = newAgent();

        agent.runAgent(parametersOf(buildInput("hi")), subscriber);

        ArgumentCaptor<AssistantMessage> assistantCaptor = ArgumentCaptor.forClass(AssistantMessage.class);

        verify(subscriber, timeout(2000)).onNewMessage(assistantCaptor.capture());

        // Pin the actionable wording — the user should know what to fix without digging into server logs. The
        // exact phrasing can evolve, but the test guards the load-bearing references: "no chat reply" + the
        // canonical step name `Response to Chat Request` so a copy-edit doesn't accidentally drop the
        // remediation pointer.
        String content = assistantCaptor.getValue()
            .getContent();

        assertThat(content)
            .contains("no chat reply")
            .contains("Response to Chat Request");
    }

    private WebhookBridgeAgent newAgent() throws AGUIException {
        // WorkflowChatMetrics records counters via Micrometer when a MeterRegistry is on the classpath. Tests
        // don't need a real registry — a mock satisfies the dependency and Mockito stubs every method to a
        // no-op, which is exactly the "no actuator on this app variant" production fallback the metrics class
        // is designed for. AiHubChatArtifactService and WorkflowChatJobRegistry likewise — the bridge
        // calls
        // them best-effort and tests don't assert on either. WorkflowChatGuard is stubbed to admit every turn so
        // the existing dispatch / sync / streaming assertions don't trip on the rate-limit + concurrency gate;
        // the gate has its own dedicated WorkflowChatGuardTest.
        WorkflowChatGuard guard = mock(WorkflowChatGuard.class);

        when(guard.tryAdmit(anyLong())).thenReturn(WorkflowChatGuard.AdmissionResult.admit());

        return new WebhookBridgeAgent(
            webhookFacade, chatService, resumeRegistry, jsonMapper, assetFileFacade,
            mock(com.bytechef.ee.ai.hub.metric.WorkflowChatMetrics.class),
            mock(WorkflowChatJobRegistry.class),
            new com.bytechef.ee.ai.hub.memory.AiHubSessionMemory(
                org.springframework.ai.session.InMemorySessionRepository.builder()
                    .build(),
                null),
            guard, null);
    }

    private static AiHubChat
        newChat(AiHubChatKind kind, AiHubChatStatus status, String title) {
        AiHubChat chat = new AiHubChat(3L);

        chat.setId(CHAT_ID);
        chat.setThreadId(THREAD_ID);
        chat.setKind(kind);
        chat.setStatus(status);
        chat.setTitle(title);
        chat.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        if (kind == AiHubChatKind.WORKFLOW_CHAT) {
            // Build a real WorkflowExecutionId so parse() succeeds in the bridge — using a hand-rolled string
            // would force us to recreate the encoding logic inline and silently rot when the format changes.
            chat.setWorkflowExecutionId(
                WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "uuid-123", "newChatRequest")
                    .toString());
        }

        return chat;
    }

    private static RunAgentInput buildInput(String userMessageText) {
        return buildInput(userMessageText, null);
    }

    private static RunAgentInput buildInput(String userMessageText, Object forwardedProps) {
        UserMessage userMessage = new UserMessage();

        // UserMessage.getRole() returns Role.user implicitly — no setter needed.
        userMessage.setContent(userMessageText);

        return new RunAgentInput(
            THREAD_ID, RUN_ID, new State(), List.of((BaseMessage) userMessage), List.of(), List.of(), forwardedProps);
    }

    /**
     * The bridge's superclass {@link com.agui.server.LocalAgent#runAgent} expects {@code RunAgentParameters}, not
     * {@code RunAgentInput} directly. Wrap the input via the parameters builder so the test exercises the same runAgent
     * → run dispatch path the production controller goes through.
     */
    private static com.agui.core.agent.RunAgentParameters parametersOf(RunAgentInput input) {
        return com.agui.core.agent.RunAgentParameters.builder()
            .threadId(input.threadId())
            .runId(input.runId())
            .messages(input.messages())
            .forwardedProps(input.forwardedProps())
            .build();
    }
}
