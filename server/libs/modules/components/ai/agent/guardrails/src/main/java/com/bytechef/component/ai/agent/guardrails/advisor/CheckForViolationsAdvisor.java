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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VIOLATIONS_METADATA_KEY;

import com.bytechef.component.ai.agent.guardrails.GuardrailException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMapUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Spring-AI advisor that runs every configured guardrail check over the user prompt (gated by {@code validateInput})
 * and the model response (gated by {@code validateOutput}). Aggregates violations; returns a blocked response when any
 * check fires. Any exception escaping a check becomes a fail-closed execution-failure violation.
 *
 * <p>
 * Streaming: {@link #adviseStream} runs only PREFLIGHT (rule-based) output checks per chunk; chunks already shipped
 * cannot be recalled. Callers that need LLM-stage output validation must use {@link #adviseCall}.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("DB_DUPLICATE_BRANCHES")
public final class CheckForViolationsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final List<String> CORRELATION_KEYS = List.of(
        "conversationId", "traceId", "spanId", "requestId", "correlationId");
    private static final String NAME = "CheckForViolationsAdvisor";

    private static final Set<String> INTERNAL_INFO_KEYS = Set.of("maskEntities");

    private final String blockedMessage;
    private final List<CheckEntry> checkEntries;
    private final Context context;
    private final List<Message> conversationHistoryMessages;

    private CheckForViolationsAdvisor(Builder builder) {
        this.blockedMessage = builder.blockedMessage;
        this.checkEntries = List.copyOf(builder.checkEntries);
        this.context = Objects.requireNonNull(builder.context, "context");
        this.conversationHistoryMessages =
            builder.conversationHistoryMessages == null ? List.of() : List.copyOf(builder.conversationHistoryMessages);
    }

    private void contextLog(Context.ContextConsumer<Context.Log> logConsumer) {
        context.log(logConsumer);
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
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        CheckOutcome inputOutcome = safeRunChecks(request);

        if (!inputOutcome.violations.isEmpty()) {
            return blockedResponse(request, inputOutcome);
        }

        ChatClientResponse response;
        CheckOutcome outputOutcome;

        try {
            response = chain.nextCall(request);

            outputOutcome = runOutputChecks(request, response, true);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            return failClosedResponse(request, throwable);
        }

        if (!outputOutcome.violations.isEmpty()) {
            return blockedResponse(request, outputOutcome);
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        CheckOutcome inputOutcome = safeRunChecks(request);

        if (!inputOutcome.violations.isEmpty()) {
            return Flux.just(blockedResponse(request, inputOutcome));
        }

        return chain.nextStream(request)
            .concatMap(response -> {
                CheckOutcome outputOutcome;

                try {
                    outputOutcome = runOutputChecks(request, response, false);
                } catch (OutOfMemoryError error) {
                    throw error;
                } catch (Throwable throwable) {
                    return Flux.just(failClosedResponse(request, throwable));
                }

                if (!outputOutcome.violations.isEmpty()) {
                    return Flux.just(blockedResponse(request, outputOutcome));
                }

                return Flux.just(response);
            })
            .takeUntil(CheckForViolationsAdvisor::isBlockedResponse)
            .onErrorResume(error -> Flux.just(failClosedResponse(request, error)));
    }

    private CheckOutcome safeRunChecks(ChatClientRequest request) {
        try {
            return runChecks(request);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            return CheckOutcome.violations(List.of(Violation.ofExecutionFailure(NAME, throwable)));
        }
    }

    private ChatClientResponse failClosedResponse(ChatClientRequest request, Throwable throwable) {
        LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

        CheckOutcome failure = CheckOutcome.violations(List.of(Violation.ofExecutionFailure(NAME, throwable)));

        return blockedResponse(request, failure);
    }

    List<Violation> runChecksForTesting(ChatClientRequest request) {
        return runChecks(request).violations;
    }

    private CheckOutcome runChecks(ChatClientRequest request) {
        String textForLlm = extractUserText(request);
        List<Violation> aggregated = new ArrayList<>();
        MaskEntityMapUtils maskEntities = new MaskEntityMapUtils(context);

        // Stage 1: PREFLIGHT (rule-based) — runs against the progressively-mutated user text and may mask.
        for (CheckEntry entry : checkEntries) {
            if (entry.function.stage() != GuardrailStage.PREFLIGHT || !shouldRun(entry, VALIDATE_INPUT)) {
                continue;
            }

            try {
                List<Violation> results = entry.function.applyAll(textForLlm, entry.context);

                aggregated.addAll(Objects.requireNonNull(results));

                if (entry.function instanceof PreflightMasking masking) {
                    MaskResult maskResult = masking.mask(textForLlm, entry.context);

                    switch (maskResult) {
                        case MaskResult.Entities entitiesResult -> maskEntities.merge(entitiesResult.entities());
                        case MaskResult.Masked maskedResult -> textForLlm = maskedResult.text();
                        case MaskResult.Unchanged ignored -> {
                        }
                    }
                }
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

                aggregated.add(Violation.ofExecutionFailure(entry.guardrailName, throwable));
            }
        }

        if (!maskEntities.isEmpty()) {
            textForLlm = maskEntities.applyTo(textForLlm);
        }

        runStage(GuardrailStage.LLM, VALIDATE_INPUT, textForLlm, aggregated);

        logViolations(request, aggregated);

        return CheckOutcome.violations(aggregated);
    }

    private CheckOutcome runOutputChecks(ChatClientRequest request, ChatClientResponse response, boolean runLlmChecks) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null || chatResponse.getResults() == null) {
            return CheckOutcome.violations(
                List.of(Violation.ofExecutionFailure(NAME, new IllegalStateException("ChatResponse is null"))));
        }

        String responseText = extractAssistantText(response);
        List<Violation> aggregated = new ArrayList<>();

        runStage(GuardrailStage.PREFLIGHT, VALIDATE_OUTPUT, responseText, aggregated);

        if (runLlmChecks) {
            runStage(GuardrailStage.LLM, VALIDATE_OUTPUT, responseText, aggregated);
        }

        logViolations(request, aggregated);

        return CheckOutcome.violations(aggregated);
    }

    private void runStage(GuardrailStage stage, String validateKey, String text, List<Violation> aggregated) {
        for (CheckEntry entry : checkEntries) {
            if (entry.function.stage() != stage || !shouldRun(entry, validateKey)) {
                continue;
            }

            GuardrailContext effectiveContext =
                (conversationHistoryMessages.isEmpty() || !VALIDATE_INPUT.equals(validateKey))
                    ? entry.context
                    : entry.context.withConversationHistoryMessages(conversationHistoryMessages);

            try {
                List<Violation> results = entry.function.applyAll(text, effectiveContext);

                aggregated.addAll(Objects.requireNonNull(results));
            } catch (OutOfMemoryError error) {
                throw error;
            } catch (Throwable throwable) {
                LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

                aggregated.add(Violation.ofExecutionFailure(entry.guardrailName, throwable));
            }
        }
    }

    private static boolean shouldRun(CheckEntry entry, String validateKey) {
        boolean defaultValue = VALIDATE_INPUT.equals(validateKey) ? DEFAULT_VALIDATE_INPUT : DEFAULT_VALIDATE_OUTPUT;

        return entry.context.inputParameters()
            .getBoolean(validateKey, defaultValue);
    }

    private static String extractAssistantText(ChatClientResponse response) {
        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null || chatResponse.getResults() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (Generation generation : chatResponse.getResults()) {
            AssistantMessage output = generation.getOutput();

            if (output == null) {
                continue;
            }

            String text = output.getText();

            if (text == null || text.isEmpty()) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append('\n');
            }

            sb.append(text);
        }

        return sb.toString();
    }

    private void logViolations(ChatClientRequest request, List<Violation> violations) {
        if (violations.isEmpty()) {
            return;
        }

        String correlation = resolveCorrelation(request);

        for (Violation violation : violations) {
            String confidenceScore = switch (violation) {
                case Violation.ClassifiedViolation classified -> Double.toString(classified.confidenceScore());
                case Violation.PatternViolation ignored -> "-";
                case Violation.ExecutionFailureViolation ignored -> "-";
            };

            boolean executionFailed = violation instanceof Violation.ExecutionFailureViolation;

            contextLog(log -> log.warn(
                "Guardrail violation detected: guardrail={}, confidenceScore={}, executionFailed={}, correlation={}",
                violation.guardrail(), confidenceScore, executionFailed, correlation));
        }
    }

    private static String resolveCorrelation(ChatClientRequest request) {
        if (request == null) {
            return "-";
        }

        Map<String, Object> context = request.context();

        if (context == null || context.isEmpty()) {
            return "-";
        }

        for (String key : CORRELATION_KEYS) {
            Object value = context.get(key);

            if (value != null) {
                return key + "=" + value;
            }
        }

        return "-";
    }

    private static boolean isBlockedResponse(ChatClientResponse response) {
        if (response == null) {
            return false;
        }

        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            return false;
        }

        return chatResponse.getMetadata()
            .containsKey(VIOLATIONS_METADATA_KEY);
    }

    private ChatClientResponse blockedResponse(ChatClientRequest request, CheckOutcome outcome) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(blockedMessage))))
            .metadata(
                VIOLATIONS_METADATA_KEY,
                outcome.violations.stream()
                    .map(CheckForViolationsAdvisor::toPublicView)
                    .toList())
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();
    }

    private static Map<String, Serializable> scrubInternalKeys(Map<String, Serializable> info) {
        if (info == null || info.isEmpty()) {
            return Map.of();
        }

        boolean containsInternal = false;

        for (String key : INTERNAL_INFO_KEYS) {
            if (info.containsKey(key)) {
                containsInternal = true;

                break;
            }
        }

        if (!containsInternal) {
            return info;
        }

        Map<String, Serializable> filtered = new LinkedHashMap<>(info);

        INTERNAL_INFO_KEYS.forEach(filtered::remove);

        return Map.copyOf(filtered);
    }

    private static Map<String, Object> toPublicView(Violation violation) {
        Map<String, Object> view = new LinkedHashMap<>();

        view.put("guardrail", violation.guardrail());

        int matchCount = switch (violation) {
            case Violation.PatternViolation pattern -> {
                List<String> matchedSubstrings = pattern.matchedSubstrings();

                yield matchedSubstrings.size();
            }
            case Violation.ClassifiedViolation ignored -> 0;
            case Violation.ExecutionFailureViolation ignored -> 0;
        };

        view.put("matchCount", matchCount);
        view.put("executionFailed", violation instanceof Violation.ExecutionFailureViolation);
        view.put("info", scrubInternalKeys(violation.info()));

        switch (violation) {
            case Violation.ExecutionFailureViolation failure -> view.put("failureKind",
                resolveFailureKind(failure.exception()));
            case Violation.ClassifiedViolation classified -> view.put("confidenceScore", classified.confidenceScore());
            case Violation.PatternViolation ignored -> {
            }
        }

        return view;
    }

    private static String resolveFailureKind(Throwable cause) {
        if (cause instanceof GuardrailException guardrailException) {
            return guardrailException.kind()
                .name();
        }

        return cause.getClass()
            .getSimpleName();
    }

    private static String extractUserText(ChatClientRequest request) {
        Prompt prompt = request.prompt();

        List<Message> messages = prompt.getInstructions();

        StringBuilder sb = new StringBuilder();

        for (Message message : messages) {
            if (message.getMessageType() != MessageType.USER) {
                continue;
            }

            String text = message.getText();

            if (text == null || text.isEmpty()) {
                continue;
            }

            if (!sb.isEmpty()) {
                sb.append('\n');
            }

            sb.append(text);
        }

        return sb.toString();
    }

    public static final class Builder {

        private String blockedMessage = "";
        private final List<PendingCheck> pendingChecks = new ArrayList<>();
        private final List<CheckEntry> checkEntries = new ArrayList<>();
        private Context context;
        private List<Message> conversationHistoryMessages;

        public Builder blockedMessage(String value) {
            this.blockedMessage = value;

            return this;
        }

        public Builder context(Context value) {
            this.context = value;

            return this;
        }

        public Builder conversationHistoryMessages(List<Message> value) {
            this.conversationHistoryMessages = List.copyOf(value);

            return this;
        }

        public Builder add(
            String guardrailName, GuardrailCheckFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters) {

            return add(
                guardrailName, function, inputParameters, connectionParameters, parentParameters, null, Map.of());
        }

        public Builder add(
            String guardrailName, GuardrailCheckFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters,
            Parameters extensions, Map<String, ComponentConnection> componentConnections) {

            return add(
                guardrailName, function, inputParameters, connectionParameters, parentParameters,
                extensions, componentConnections, null);
        }

        public Builder add(
            String guardrailName, GuardrailCheckFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters parentParameters,
            Parameters extensions, Map<String, ComponentConnection> componentConnections,
            ChatClient chatClient) {

            pendingChecks.add(new PendingCheck(
                guardrailName, function, inputParameters, connectionParameters, parentParameters,
                extensions, componentConnections, chatClient));

            return this;
        }

        public CheckForViolationsAdvisor build() {
            Objects.requireNonNull(context, "context");

            for (PendingCheck pending : pendingChecks) {
                GuardrailContext guardrailContext = GuardrailContext.builder()
                    .inputParameters(pending.inputParameters)
                    .connectionParameters(pending.connectionParameters)
                    .parentParameters(pending.parentParameters)
                    .extensions(pending.extensions)
                    .componentConnections(pending.componentConnections)
                    .chatClient(pending.chatClient)
                    .context(context)
                    .build();

                checkEntries.add(new CheckEntry(pending.guardrailName, pending.function, guardrailContext));
            }

            return new CheckForViolationsAdvisor(this);
        }
    }

    private record PendingCheck(
        String guardrailName, GuardrailCheckFunction function, Parameters inputParameters,
        Parameters connectionParameters, Parameters parentParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {
    }

    private record CheckEntry(
        String guardrailName, GuardrailCheckFunction function, GuardrailContext context) {
    }

    private record CheckOutcome(List<Violation> violations) {

        private static CheckOutcome violations(List<Violation> violations) {
            return new CheckOutcome(violations);
        }
    }
}
