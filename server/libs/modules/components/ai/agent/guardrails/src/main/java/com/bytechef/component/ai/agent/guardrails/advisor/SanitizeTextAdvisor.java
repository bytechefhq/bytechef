/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agent.guardrails.advisor;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_VALIDATE_OUTPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SANITIZE_WITHHELD_KEY;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;

import com.bytechef.component.ai.agent.guardrails.SanitizerExecutionFailureException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallScope;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallTarget;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.HumanToolResponses;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring-AI advisor that runs sanitizers over the user message (input) and the assistant message (output). PREFLIGHT
 * (rule-based) sanitizers run first and mask their entities; LLM-based sanitizers run afterwards on the already-masked
 * text. Any exception escaping a sanitizer triggers a withheld-placeholder response. Input sanitizing also covers a
 * tool response that carries a human's answer (see {@code HumanToolResponses}), treated like a user message.
 *
 * <p>
 * Streaming: {@link #adviseStream} sanitizes per chunk — cross-chunk patterns are not masked and chunks already shipped
 * cannot be recalled. Use {@link #adviseCall} when full-text guarantees are required.
 *
 * @author Ivica Cardic
 */
public final class SanitizeTextAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String NAME = "SanitizeTextAdvisor";
    private static final String SANITIZER_FAILURE_PLACEHOLDER = "[sanitizer failed — response withheld]";
    private static final String SIDE_INPUT = "INPUT";
    private static final String SIDE_OUTPUT = "OUTPUT";
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final ClusterElementCallTarget callTarget;
    private final Context context;
    private final String frameComponentName;
    private final String frameWorkflowNodeName;
    private final List<SanitizerEntry> sanitizers;

    private SanitizeTextAdvisor(Builder builder, ClusterElementCallTarget callTarget) {
        this.callTarget = callTarget;
        this.context = Objects.requireNonNull(builder.context, "context");
        this.frameComponentName = builder.frameComponentName;
        this.frameWorkflowNodeName = builder.frameWorkflowNodeName;
        this.sanitizers = List.copyOf(builder.sanitizers);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrder() {
        return DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 1;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ClusterElementCallRef parentRef = ClusterElementCallScope.current();
        SanitizeTally inputTally = new SanitizeTally();
        ChatClientRequest sanitizedRequest;

        try {
            sanitizedRequest = sanitizeRequest(request, inputTally);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            recordWithheld(inputTally, SIDE_INPUT, parentRef);

            return withheldResponse(request);
        }

        recordRedactions(inputTally, SIDE_INPUT, parentRef);

        ChatClientResponse response = chain.nextCall(sanitizedRequest);

        SanitizeTally outputTally = new SanitizeTally();
        ChatClientResponse rewrittenResponse;

        try {
            rewrittenResponse = rewriteResponse(response, outputTally);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            recordWithheld(outputTally, SIDE_OUTPUT, parentRef);

            return withheldResponse(request);
        }

        recordRedactions(outputTally, SIDE_OUTPUT, parentRef);

        return rewrittenResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ClusterElementCallRef parentRef = ClusterElementCallScope.current();
        SanitizeTally inputTally = new SanitizeTally();
        ChatClientRequest sanitizedRequest;

        try {
            sanitizedRequest = sanitizeRequest(request, inputTally);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            recordWithheld(inputTally, SIDE_INPUT, parentRef);

            return Flux.just(withheldResponse(request));
        }

        recordRedactions(inputTally, SIDE_INPUT, parentRef);

        SanitizeTally outputTally = new SanitizeTally();

        return chain.nextStream(sanitizedRequest)
            .map(response -> safeRewriteResponse(request, response, outputTally, parentRef))
            .takeUntil(SanitizeTextAdvisor::isWithheldResponse)
            .doOnComplete(() -> recordStreamRedactions(outputTally, parentRef))
            .doOnError(throwable -> recordStreamRedactions(outputTally, parentRef));
    }

    private void recordStreamRedactions(SanitizeTally outputTally, ClusterElementCallRef parentRef) {
        if (!outputTally.withheld) {
            recordRedactions(outputTally, SIDE_OUTPUT, parentRef);
        }
    }

    private ChatClientResponse safeRewriteResponse(
        ChatClientRequest request, ChatClientResponse response, SanitizeTally outputTally,
        ClusterElementCallRef parentRef) {

        try {
            return rewriteResponse(response, outputTally);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            outputTally.withheld = true;

            recordWithheld(outputTally, SIDE_OUTPUT, parentRef);

            return withheldResponse(request);
        }
    }

    private static boolean isWithheldResponse(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();

        return chatResponse != null && chatResponse.getMetadata()
            .containsKey(SANITIZE_WITHHELD_KEY);
    }

    String sanitizeForTesting(String text) {
        return sanitize(text, false, new SanitizeTally());
    }

    private boolean canRecord() {
        return callTarget != null && frameWorkflowNodeName != null;
    }

    private void recordRedactions(SanitizeTally tally, String side, ClusterElementCallRef parentRef) {
        if (!canRecord() || tally.redactions.isEmpty()) {
            return;
        }

        Integer frameMaskCount = null;

        for (Redaction redaction : tally.redactions.values()) {
            if (redaction.maskCount != null) {
                frameMaskCount = (frameMaskCount == null ? 0 : frameMaskCount) + redaction.maskCount;
            }
        }

        ClusterElementCallRecorder recorder = callTarget.recorder();

        ClusterElementCallRef frameRef = recorder.recordOutcome(
            callTarget.jobId(), callTarget.taskExecutionId(), frameWorkflowNodeName, frameComponentName,
            parentRef == null ? null : parentRef.id(), ClusterElementCallRecord.Status.REDACTED,
            redactionDetails(side, frameMaskCount), null);

        for (SanitizerEntry entry : sanitizers) {
            Redaction redaction = tally.redactions.get(entry);

            if (redaction == null || entry.workflowNodeName() == null) {
                continue;
            }

            recorder.recordOutcome(
                callTarget.jobId(), callTarget.taskExecutionId(), entry.workflowNodeName(), entry.componentName(),
                frameRef.id(), ClusterElementCallRecord.Status.REDACTED,
                redactionDetails(side, redaction.maskCount), null);
        }
    }

    private void recordWithheld(SanitizeTally tally, String side, ClusterElementCallRef parentRef) {
        if (!canRecord()) {
            return;
        }

        ClusterElementCallRecorder recorder = callTarget.recorder();

        ClusterElementCallRef frameRef = recorder.recordOutcome(
            callTarget.jobId(), callTarget.taskExecutionId(), frameWorkflowNodeName, frameComponentName,
            parentRef == null ? null : parentRef.id(), ClusterElementCallRecord.Status.WITHHELD,
            redactionDetails(side, null), null);

        for (SanitizerEntry entry : sanitizers) {
            Throwable failure = tally.failures.get(entry);

            if (failure == null || entry.workflowNodeName() == null) {
                continue;
            }

            Class<? extends Throwable> failureClass = failure.getClass();

            recorder.recordOutcome(
                callTarget.jobId(), callTarget.taskExecutionId(), entry.workflowNodeName(), entry.componentName(),
                frameRef.id(), ClusterElementCallRecord.Status.FAILED, null, failureClass.getSimpleName());
        }
    }

    private static String redactionDetails(String side, Integer maskCount) {
        Map<String, Object> details = new LinkedHashMap<>();

        details.put("side", side);

        if (maskCount != null) {
            details.put("maskCount", maskCount);
        }

        return JSON_MAPPER.writeValueAsString(details);
    }

    private static ChatClientResponse withheldResponse(ChatClientRequest request) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(SANITIZER_FAILURE_PLACEHOLDER))))
            .metadata(SANITIZE_WITHHELD_KEY, Boolean.TRUE)
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();
    }

    private ChatClientResponse rewriteResponse(ChatClientResponse response, SanitizeTally tally) {
        if (sanitizers.isEmpty()) {
            return response;
        }

        boolean anyOutputSanitizer = sanitizers.stream()
            .anyMatch(entry -> entry.context.inputParameters()
                .getBoolean(VALIDATE_OUTPUT, DEFAULT_VALIDATE_OUTPUT));

        if (!anyOutputSanitizer) {
            return response;
        }

        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return response;
        }

        List<Generation> results = chatResponse.getResults();
        List<Generation> rewrittenGenerations = new ArrayList<>(results.size());

        for (Generation generation : results) {
            AssistantMessage original = generation.getOutput();

            if (original == null) {
                rewrittenGenerations.add(generation);

                continue;
            }

            String originalText = original.getText();

            if (originalText == null) {
                rewrittenGenerations.add(generation);

                continue;
            }

            String sanitized = sanitize(originalText, false, tally);

            AssistantMessage rewrittenAssistantMessage = AssistantMessage.builder()
                .content(sanitized)
                .properties(original.getMetadata())
                .toolCalls(original.getToolCalls())
                .media(original.getMedia())
                .build();

            rewrittenGenerations.add(new Generation(rewrittenAssistantMessage, generation.getMetadata()));
        }

        ChatResponse rewrittenChatResponse = ChatResponse.builder()
            .generations(rewrittenGenerations)
            .metadata(chatResponse.getMetadata())
            .build();

        return response.mutate()
            .chatResponse(rewrittenChatResponse)
            .build();
    }

    private String sanitize(String text, boolean isInput, SanitizeTally tally) {
        Map<String, Throwable> closedFailures = new LinkedHashMap<>();
        String intermediate = text;
        MaskEntityMapUtils maskEntities = new MaskEntityMapUtils(context);

        for (SanitizerEntry entry : sanitizers) {
            if (entry.function.stage() != GuardrailStage.PREFLIGHT || !shouldRun(entry, isInput)) {
                continue;
            }

            try {
                if (entry.function instanceof PreflightMasking masking) {
                    MaskResult maskResult = masking.mask(intermediate, entry.context);

                    switch (maskResult) {
                        case MaskResult.Entities entitiesResult -> {
                            Map<String, List<String>> entities = entitiesResult.entities();

                            maskEntities.merge(entities);

                            int maskCount = MaskEntityMapUtils.countMatches(intermediate, entities);

                            if (maskCount > 0) {
                                tally.redacted(entry, maskCount);
                            }
                        }
                        case MaskResult.Masked maskedResult -> {
                            String maskedText = maskedResult.text();

                            if (!maskedText.equals(intermediate)) {
                                tally.redacted(entry, null);
                            }

                            intermediate = maskedText;
                        }
                        case MaskResult.Unchanged ignored -> {
                        }
                    }
                } else {
                    String preflightResult = entry.function.apply(intermediate, entry.context);

                    if (preflightResult == null) {
                        IllegalStateException nullResult = new IllegalStateException("Sanitizer returned null");

                        closedFailures.put(entry.sanitizerName, nullResult);

                        tally.failed(entry, nullResult);
                    } else {
                        if (!preflightResult.equals(intermediate)) {
                            tally.redacted(entry, null);
                        }

                        intermediate = preflightResult;
                    }
                }
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

                closedFailures.put(entry.sanitizerName, throwable);

                tally.failed(entry, throwable);
            }
        }

        if (!maskEntities.isEmpty()) {
            intermediate = maskEntities.applyTo(intermediate);
        }

        for (SanitizerEntry entry : sanitizers) {
            if (entry.function.stage() != GuardrailStage.LLM || !shouldRun(entry, isInput)) {
                continue;
            }

            intermediate = collectingApply(entry, intermediate, closedFailures, tally);
        }

        if (!closedFailures.isEmpty()) {
            throw new SanitizerExecutionFailureException(closedFailures);
        }

        return intermediate;
    }

    private ChatClientRequest sanitizeRequest(ChatClientRequest request, SanitizeTally tally) {
        boolean anyInputSanitizer = sanitizers.stream()
            .anyMatch(entry -> entry.context.inputParameters()
                .getBoolean(VALIDATE_INPUT, DEFAULT_VALIDATE_INPUT));

        if (!anyInputSanitizer) {
            return request;
        }

        Prompt prompt = request.prompt();
        List<Message> messages = prompt.getInstructions();
        List<Message> sanitizedMessages = new ArrayList<>(messages.size());
        boolean modified = false;

        for (Message message : messages) {
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                String humanResponseText = HumanToolResponses.getHumanResponseText(toolResponseMessage);

                if (humanResponseText == null || humanResponseText.isEmpty()) {
                    sanitizedMessages.add(message);

                    continue;
                }

                String sanitizedHumanResponseText = sanitize(humanResponseText, true, tally);

                if (sanitizedHumanResponseText.equals(humanResponseText)) {
                    sanitizedMessages.add(message);
                } else {
                    sanitizedMessages.add(
                        HumanToolResponses.withHumanResponseText(toolResponseMessage, sanitizedHumanResponseText));

                    modified = true;
                }

                continue;
            }

            if (message.getMessageType() != MessageType.USER) {
                sanitizedMessages.add(message);

                continue;
            }

            String text = message.getText();

            if (text == null || text.isEmpty()) {
                sanitizedMessages.add(message);

                continue;
            }

            String sanitizedText = sanitize(text, true, tally);

            if (!sanitizedText.equals(text)) {
                sanitizedMessages.add(rewriteUserMessage(message, sanitizedText));

                modified = true;
            } else {
                sanitizedMessages.add(message);
            }
        }

        if (!modified) {
            return request;
        }

        Prompt newPrompt = new Prompt(sanitizedMessages, prompt.getOptions());

        return request.mutate()
            .prompt(newPrompt)
            .build();
    }

    private static UserMessage rewriteUserMessage(Message original, String sanitizedText) {
        UserMessage.Builder builder = UserMessage.builder()
            .text(sanitizedText);

        Map<String, Object> metadata = original.getMetadata();

        if (metadata != null && !metadata.isEmpty()) {
            builder.metadata(metadata);
        }

        if (original instanceof UserMessage userMessage) {
            List<Media> media = userMessage.getMedia();

            if (media != null && !media.isEmpty()) {
                builder.media(media);
            }
        }

        return builder.build();
    }

    private static boolean shouldRun(SanitizerEntry entry, boolean isInput) {
        Parameters parameters = entry.context.inputParameters();

        if (isInput) {
            return parameters.getBoolean(VALIDATE_INPUT, DEFAULT_VALIDATE_INPUT);
        }

        return parameters.getBoolean(VALIDATE_OUTPUT, DEFAULT_VALIDATE_OUTPUT);
    }

    private static String collectingApply(
        SanitizerEntry entry, String text, Map<String, Throwable> closedFailures, SanitizeTally tally) {

        try {
            String result = entry.function.apply(text, entry.context);

            if (result == null) {
                IllegalStateException nullResult = new IllegalStateException("Sanitizer returned null");

                closedFailures.put(entry.sanitizerName, nullResult);

                tally.failed(entry, nullResult);

                return text;
            }

            if (!result.equals(text)) {
                tally.redacted(entry, null);
            }

            return result;
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            closedFailures.put(entry.sanitizerName, throwable);

            tally.failed(entry, throwable);

            return text;
        }
    }

    public static final class Builder {

        private ClusterElementCallTarget callTarget;
        private Context context;
        private String frameComponentName;
        private String frameWorkflowNodeName;
        private final List<PendingSanitizer> pendingSanitizers = new ArrayList<>();
        private final List<SanitizerEntry> sanitizers = new ArrayList<>();

        public Builder callTarget(ClusterElementCallTarget value) {
            this.callTarget = value;

            return this;
        }

        public Builder clusterElement(String workflowNodeName, String componentName) {
            this.frameWorkflowNodeName = workflowNodeName;
            this.frameComponentName = componentName;

            return this;
        }

        public Builder context(Context value) {
            this.context = value;

            return this;
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters) {

            return add(sanitizerName, function, inputParameters, connectionParameters, null, null, Map.of(), null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, null, extensions,
                componentConnections, null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, null, extensions,
                componentConnections, chatClient);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, parentParameters, extensions,
                componentConnections, null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {

            pendingSanitizers.add(new PendingSanitizer(
                sanitizerName, null, null, function, inputParameters, connectionParameters, parentParameters,
                extensions, componentConnections, chatClient));

            return this;
        }

        public Builder add(
            ClusterElement element, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {

            pendingSanitizers.add(new PendingSanitizer(
                element.getClusterElementName(), element.getWorkflowNodeName(), element.getComponentName(), function,
                inputParameters, connectionParameters, parentParameters, extensions, componentConnections,
                chatClient));

            return this;
        }

        public SanitizeTextAdvisor build() {
            Objects.requireNonNull(context, "context");

            for (PendingSanitizer pending : pendingSanitizers) {
                GuardrailContext guardrailContext = GuardrailContext.builder()
                    .inputParameters(pending.inputParameters)
                    .connectionParameters(pending.connectionParameters)
                    .parentParameters(pending.parentParameters)
                    .extensions(pending.extensions)
                    .componentConnections(pending.componentConnections)
                    .chatClient(pending.chatClient)
                    .context(context)
                    .build();

                sanitizers.add(new SanitizerEntry(
                    pending.sanitizerName, pending.workflowNodeName, pending.componentName, pending.function,
                    guardrailContext));
            }

            ClusterElementCallTarget resolvedCallTarget = callTarget == null
                ? ClusterElementCallTarget.of(context)
                    .orElse(null)
                : callTarget;

            return new SanitizeTextAdvisor(this, resolvedCallTarget);
        }
    }

    private record PendingSanitizer(
        String sanitizerName, @Nullable String workflowNodeName, @Nullable String componentName,
        GuardrailSanitizerFunction function, Parameters inputParameters, Parameters connectionParameters,
        Parameters parentParameters, Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ChatClient chatClient) {
    }

    private record SanitizerEntry(
        String sanitizerName, @Nullable String workflowNodeName, @Nullable String componentName,
        GuardrailSanitizerFunction function, GuardrailContext context) {
    }

    private static final class Redaction {

        private Integer maskCount;
    }

    private static final class SanitizeTally {

        private final Map<SanitizerEntry, Throwable> failures = new IdentityHashMap<>();
        private final Map<SanitizerEntry, Redaction> redactions = new IdentityHashMap<>();
        private boolean withheld;

        private void failed(SanitizerEntry entry, Throwable throwable) {
            failures.putIfAbsent(entry, throwable);
        }

        private void redacted(SanitizerEntry entry, Integer maskCount) {
            Redaction redaction = redactions.computeIfAbsent(entry, key -> new Redaction());

            if (maskCount != null) {
                redaction.maskCount = (redaction.maskCount == null ? 0 : redaction.maskCount) + maskCount;
            }
        }
    }
}
