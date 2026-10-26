/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails.GuardrailCheckResult;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails.TokenSessionHandle;
import com.bytechef.ee.platform.ai.guardrails.StreamingResponseRedactor;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolationAction;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.violation.AiGuardrailViolationRecorder;
import com.bytechef.platform.ai.guardrails.ConversationScope;
import com.bytechef.platform.ai.guardrails.GuardrailAdvisorOrder;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.ai.guardrails.PublishedInputSpans;
import com.bytechef.platform.ai.guardrails.RestorationDestination;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSessionToolContext;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicy;
import com.bytechef.platform.ai.sensitivedata.tokenization.SensitiveDataPolicyToolContext;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGuardrailsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME = "AiGuardrailsAdvisor";

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailsAdvisor.class);

    private final AiGuardrails aiGuardrails;
    private final AiGuardrailMetrics metrics;
    private final @Nullable AiGuardrailViolationRecorder aiGuardrailViolationRecorder;
    private final AiGuardrailsSettingsTarget target;
    private final RestorationDestination destination;
    private final String surface;

    public AiGuardrailsAdvisor(
        AiGuardrails aiGuardrails, AiGuardrailsSettingsTarget target, AiGuardrailMetrics metrics, String surface,
        RestorationDestination destination) {

        this(aiGuardrails, target, metrics, surface, null, destination);
    }

    @SuppressFBWarnings("EI2")
    public AiGuardrailsAdvisor(
        AiGuardrails aiGuardrails, AiGuardrailsSettingsTarget target, AiGuardrailMetrics metrics, String surface,
        @Nullable AiGuardrailViolationRecorder aiGuardrailViolationRecorder, RestorationDestination destination) {

        this.aiGuardrails = Objects.requireNonNull(aiGuardrails, "aiGuardrails");
        this.target = Objects.requireNonNull(target, "target");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.surface = Objects.requireNonNull(surface, "surface");
        this.aiGuardrailViolationRecorder = aiGuardrailViolationRecorder;
        this.destination = Objects.requireNonNull(destination, "destination");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return GuardrailAdvisorOrder.WORKSPACE_FLOOR;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ConversationScope.@Nullable Key conversationKey = trustedConversationKey(chatClientRequest);
        TokenSessionHandle tokenSessionHandle = newTokenSession(conversationKey);

        PiiTokenSession session = tokenSessionHandle.session();

        try {
            ChatClientRequest guardedRequest = applyInputGuardrails(chatClientRequest, session);

            boolean workflowSurface = GuardrailSurface.AI_AGENT.equals(surface);
            boolean restoreIntoWorkflowOutput = true;

            if (workflowSurface || destination == RestorationDestination.WORKFLOW_OUTPUT) {
                restoreIntoWorkflowOutput = aiGuardrails.isRestoreIntoWorkflowOutput(target);
            }

            guardedRequest = withSessionInToolContext(guardedRequest, session, restoreIntoWorkflowOutput);

            ChatClientResponse response = callAdvisorChain.nextCall(guardedRequest);

            return applyResponseGuardrails(response, session, restoreIntoWorkflowOutput);
        } finally {
            releaseSession(conversationKey, tokenSessionHandle);
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        ConversationScope.@Nullable Key conversationKey = trustedConversationKey(chatClientRequest);
        TokenSessionHandle tokenSessionHandle = newTokenSession(conversationKey);

        PiiTokenSession session = tokenSessionHandle.session();
        ChatClientRequest guardedRequest;

        try {
            guardedRequest = applyInputGuardrails(chatClientRequest, session);

            boolean workflowSurface = GuardrailSurface.AI_AGENT.equals(surface);
            boolean restoreIntoWorkflowOutput = true;

            if (workflowSurface) {
                restoreIntoWorkflowOutput = aiGuardrails.isRestoreIntoWorkflowOutput(target);
            }

            guardedRequest = withSessionInToolContext(guardedRequest, session, restoreIntoWorkflowOutput);
        } catch (AiGuardrailViolationException exception) {
            releaseSession(conversationKey, tokenSessionHandle);

            return Flux.error(exception);
        }

        StreamingResponseRedactor redactor = aiGuardrails.newStreamingResponseRedactor(target, metrics, session);
        Flux<ChatClientResponse> stream = streamAdvisorChain.nextStream(guardedRequest);

        if (redactor == null) {
            return stream.doFinally(signalType -> releaseSession(conversationKey, tokenSessionHandle));
        }

        ChatClientRequest finalGuardedRequest = guardedRequest;

        return stream.map(response -> redactStreamChunk(response, redactor))
            .concatWith(Flux.defer(() -> flushStreamTail(redactor, finalGuardedRequest)))
            .doFinally(signalType -> releaseSession(conversationKey, tokenSessionHandle));
    }

    private ConversationScope.@Nullable Key trustedConversationKey(ChatClientRequest chatClientRequest) {
        return ConversationScope.trustedKey(chatClientRequest.context(), target.workspaceId())
            .orElse(null);
    }

    private TokenSessionHandle newTokenSession(ConversationScope.@Nullable Key conversationKey) {
        if (conversationKey == null) {
            return new TokenSessionHandle(aiGuardrails.newTokenSession(), true);
        }

        return aiGuardrails.newTokenSession(conversationKey);
    }

    private void releaseSession(
        ConversationScope.@Nullable Key conversationKey, TokenSessionHandle tokenSessionHandle) {

        try {
            if (conversationKey != null) {
                aiGuardrails.saveTokenSession(conversationKey, tokenSessionHandle);
            }
        } catch (RuntimeException exception) {
            log.warn("Could not save the conversation's token session; continuing request-scoped", exception);
        } finally {
            PiiTokenSession session = tokenSessionHandle.session();

            session.close();
        }
    }

    private ChatClientRequest applyInputGuardrails(
        ChatClientRequest chatClientRequest, @Nullable PiiTokenSession session) {

        Prompt prompt = chatClientRequest.prompt();
        List<Message> instructions = prompt.getInstructions();

        List<Integer> guardedIndexes = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (int index = 0; index < instructions.size(); index++) {
            Message message = instructions.get(index);
            MessageType messageType = message.getMessageType();

            if (messageType != MessageType.USER && messageType != MessageType.SYSTEM) {
                continue;
            }

            String text = message.getText();

            if (text == null || text.isEmpty()) {
                continue;
            }

            guardedIndexes.add(index);
            texts.add(text);
        }

        if (texts.isEmpty()) {
            return chatClientRequest;
        }

        List<GuardrailCheckResult> results = session == null
            ? aiGuardrails.checkInputs(texts, target, metrics)
            : aiGuardrails.tokenizeInputs(texts, target, session, metrics);
        boolean anyBlocked = results.stream()
            .anyMatch(GuardrailCheckResult::blocked);

        BlockingMode blockingMode = anyBlocked ? aiGuardrails.resolveBlockingMode(target) : null;

        if (blockingMode == BlockingMode.BLOCK) {
            submitViolationRecords(results, blockingMode, target);

            String category = results.stream()
                .filter(GuardrailCheckResult::blocked)
                .findFirst()
                .orElseThrow()
                .category();

            throw new AiGuardrailViolationException(category);
        }

        submitViolationRecords(results, blockingMode, target);

        List<SensitiveSpan> publishedSpans = userMessageSpans(instructions, guardedIndexes, results);

        List<Message> patched = new ArrayList<>(instructions);
        boolean changed = false;

        for (int i = 0; i < guardedIndexes.size(); i++) {
            GuardrailCheckResult result = results.get(i);

            boolean allowed = result.blocked() && blockingMode == BlockingMode.ALLOW;

            if (result.blocked()) {
                metrics.record(allowed ? "guardrail_allowed" : "blocking_downgraded");
            }

            int index = guardedIndexes.get(i);
            Message original = instructions.get(index);
            String newText = allowed ? result.unmaskedText() : result.text();

            if (!Objects.equals(newText, original.getText())) {
                patched.set(index, withText(original, newText));

                changed = true;
            }
        }

        if (!changed && publishedSpans.isEmpty()) {
            return chatClientRequest;
        }

        ChatClientRequest.Builder builder = chatClientRequest.mutate();

        if (changed) {
            builder.prompt(new Prompt(patched, prompt.getOptions()));
        }

        if (!publishedSpans.isEmpty()) {
            builder.context(PublishedInputSpans.CONTEXT_KEY, publishedSpans);
        }

        return builder.build();
    }

    private ChatClientRequest withSessionInToolContext(
        ChatClientRequest chatClientRequest, PiiTokenSession session, boolean restoreIntoWorkflowOutput) {

        Prompt prompt = chatClientRequest.prompt();
        ChatOptions chatOptions = prompt.getOptions();

        if (!(chatOptions instanceof ToolCallingChatOptions toolCallingChatOptions)) {
            return chatClientRequest;
        }

        Map<String, Object> existingToolContext = toolCallingChatOptions.getToolContext();
        Map<String, Object> toolContextWithSession = PiiTokenSessionToolContext.into(
            existingToolContext == null ? Map.of() : existingToolContext, session);

        boolean workflowSurface = GuardrailSurface.AI_AGENT.equals(surface);
        SensitiveDataPolicy basePolicy = aiGuardrails.resolveToolBoundaryPolicy(target);
        SensitiveDataPolicy policy = new SensitiveDataPolicy(
            basePolicy.kinds(), basePolicy.minConfidence(), !workflowSurface || restoreIntoWorkflowOutput);
        Map<String, Object> mergedToolContext = SensitiveDataPolicyToolContext.into(toolContextWithSession, policy);

        ChatOptions mergedChatOptions = toolCallingChatOptions.mutate()
            .toolContext(mergedToolContext)
            .build();

        Prompt mergedPrompt = new Prompt(prompt.getInstructions(), mergedChatOptions);

        return chatClientRequest.mutate()
            .prompt(mergedPrompt)
            .build();
    }

    private ChatClientResponse applyResponseGuardrails(
        ChatClientResponse response, PiiTokenSession session, boolean restoreIntoWorkflowOutput) {

        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return response;
        }

        boolean restoring = destination == RestorationDestination.CONVERSATION || restoreIntoWorkflowOutput;

        List<Generation> generations = chatResponse.getResults();
        List<Generation> rewrittenGenerations = new ArrayList<>(generations.size());
        boolean responseRedacted = false;
        boolean piiRestored = false;
        boolean restoreSuppressed = false;

        for (Generation generation : generations) {
            AssistantMessage original = generation.getOutput();
            String text = original == null ? null : original.getText();

            if (text == null) {
                rewrittenGenerations.add(generation);

                continue;
            }

            String scanned = aiGuardrails.scanResponseText(text, target, metrics);
            String restored;

            if (restoring) {
                restored = aiGuardrails.restoreResponseText(scanned, session, metrics);
            } else {
                restored = scanned;

                PiiTokenSession.RestoreResult wouldHaveRestored = session.restoreWithUnresolvedCount(scanned);

                if (!Objects.equals(wouldHaveRestored.text(), scanned)) {
                    restoreSuppressed = true;
                }
            }

            boolean generationRedacted = !scanned.equals(text);
            boolean generationRestored = !Objects.equals(restored, scanned);

            if (!generationRedacted && !generationRestored) {
                rewrittenGenerations.add(generation);

                continue;
            }

            responseRedacted = responseRedacted || generationRedacted;
            piiRestored = piiRestored || generationRestored;

            AssistantMessage rewritten = AssistantMessage.builder()
                .content(restored)
                .properties(original.getMetadata())
                .toolCalls(original.getToolCalls())
                .media(original.getMedia())
                .build();

            rewrittenGenerations.add(new Generation(rewritten, generation.getMetadata()));
        }

        if (restoreSuppressed) {
            metrics.recordRestoreSuppressed();
        }

        if (!responseRedacted && !piiRestored) {
            return response;
        }

        if (responseRedacted) {
            metrics.record("response_redacted");
        }

        ChatResponse rewrittenChatResponse = ChatResponse.builder()
            .generations(rewrittenGenerations)
            .metadata(chatResponse.getMetadata())
            .build();

        return response.mutate()
            .chatResponse(rewrittenChatResponse)
            .build();
    }

    private ChatClientResponse redactStreamChunk(ChatClientResponse response, StreamingResponseRedactor redactor) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return response;
        }

        Generation generation = chatResponse.getResult();

        if (generation == null) {
            return response;
        }

        AssistantMessage original = generation.getOutput();
        String chunkText = original == null ? null : original.getText();
        String deltaText = redactor.push(chunkText);

        AssistantMessage redactedMessage = AssistantMessage.builder()
            .content(deltaText)
            .properties(original == null ? Map.of() : original.getMetadata())
            .toolCalls(original == null ? List.of() : original.getToolCalls())
            .media(original == null ? List.of() : original.getMedia())
            .build();

        Generation redactedGeneration = new Generation(redactedMessage, generation.getMetadata());

        ChatResponse redactedChatResponse = ChatResponse.builder()
            .generations(List.of(redactedGeneration))
            .metadata(chatResponse.getMetadata())
            .build();

        return response.mutate()
            .chatResponse(redactedChatResponse)
            .build();
    }

    private Flux<ChatClientResponse> flushStreamTail(StreamingResponseRedactor redactor, ChatClientRequest request) {
        String tail = redactor.flush();

        if (redactor.isRedacted()) {
            metrics.record("response_redacted");
        }

        if (tail.isEmpty()) {
            return Flux.empty();
        }

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(tail))))
            .build();

        ChatClientResponse tailResponse = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();

        return Flux.just(tailResponse);
    }

    private static Message withText(Message message, String text) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.mutate()
                .text(text)
                .build();
        }

        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.mutate()
                .text(text)
                .build();
        }

        return message;
    }

    private void submitViolationRecords(
        List<GuardrailCheckResult> results, @Nullable BlockingMode blockingMode, AiGuardrailsSettingsTarget target) {

        if (aiGuardrailViolationRecorder == null) {
            return;
        }

        boolean enabled = aiGuardrails.isViolationRecordingEnabled(target);

        if (!enabled) {
            return;
        }

        AiGuardrailViolationAction action = actionOf(blockingMode);

        for (GuardrailCheckResult result : results) {
            aiGuardrailViolationRecorder.submit(
                result.spans(), true, action, metrics.getSurface(), target.workspaceId(), null,
                SecurityUtils.fetchCurrentUserLogin()
                    .orElse(null),
                metrics::record);
        }
    }

    private static AiGuardrailViolationAction actionOf(@Nullable BlockingMode blockingMode) {
        if (blockingMode == BlockingMode.BLOCK) {
            return AiGuardrailViolationAction.BLOCKED;
        }

        if (blockingMode == BlockingMode.ALLOW) {
            return AiGuardrailViolationAction.ALLOWED;
        }

        return AiGuardrailViolationAction.REDACTED;
    }

    private static List<SensitiveSpan> userMessageSpans(
        List<Message> instructions, List<Integer> guardedIndexes, List<GuardrailCheckResult> results) {

        List<SensitiveSpan> spans = new ArrayList<>();

        for (int i = 0; i < guardedIndexes.size(); i++) {
            Message message = instructions.get(guardedIndexes.get(i));

            if (message.getMessageType() != MessageType.USER) {
                continue;
            }

            spans.addAll(results.get(i)
                .spans());
        }

        return List.copyOf(spans);
    }
}
