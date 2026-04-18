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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_MODE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.SKIPPED_FAILURES_METADATA_KEY;

import com.bytechef.component.ai.agent.guardrails.GuardrailException;
import com.bytechef.component.ai.agent.guardrails.SanitizerExecutionFailureException;
import com.bytechef.component.ai.agent.guardrails.constant.FailMode;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMap;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

/**
 * Spring-AI advisor that runs after the chat model responds and chains sanitizers over the assistant message text.
 * Apply-style sanitizers see the text as rewritten by predecessors; rule-based entity collectors read the same pre-mask
 * text and contribute spans that are merged into a single length-sorted replacement pass applied after the PREFLIGHT
 * loop completes.
 *
 * <p>
 * Sanitizers run in two stages: PREFLIGHT (rule-based) sanitizers run first and mask their detected entities; LLM-based
 * sanitizers run afterwards and see the already-masked text.
 *
 * <p>
 * Failure policy is per-sanitizer via the {@code failMode} parameter:
 * <ul>
 * <li><b>FAIL_OPEN</b> — when a sanitizer throws, its span is skipped: the last-good intermediate text propagates to
 * the next sanitizer and ultimately to the caller, with a WARN logged. Partial masking is preserved.</li>
 * <li><b>FAIL_CLOSED</b> (default) — a thrown exception fails the entire pass. At end-of-pass, if any FAIL_CLOSED
 * sanitizer failed, {@link SanitizerExecutionFailureException} is thrown and {@link #adviseCall}/{@link #adviseStream}
 * replace the response with a placeholder rather than risk leaking unredacted text.</li>
 * </ul>
 * Upstream stream errors that occur before any sanitization can run — and malformed chat responses with null or empty
 * results — are also withheld via a placeholder, because in both cases the advisor cannot prove the response is safe to
 * pass through.
 *
 * <p>
 * <b>Telemetry:</b> fail-open sanitizer crashes surface on the response via {@code SKIPPED_FAILURES_METADATA_KEY} —
 * symmetric with {@code CheckForViolationsAdvisor} — so downstream alerting can detect "text was returned with a
 * silently degraded sanitizer" without having to grep log files. Each entry carries the sanitizer name and a stable
 * {@link com.bytechef.component.ai.agent.guardrails.GuardrailExceptionKind failureKind}; raw cause messages never reach
 * this channel (they can legitimately contain LLM output fragments).
 *
 * <p>
 * <b>Streaming caveat:</b> {@link #adviseStream} cannot recall chunks that have already been delivered to the caller.
 * When a sanitizer fails after one or more chunks have shipped, the remainder is swapped to a placeholder and an
 * ERROR-level {@code partial-leak} telemetry event is emitted — see that method's Javadoc for details and for the
 * recommended mitigation (use {@link #adviseCall} when full-text guarantees are required).
 *
 * @author Ivica Cardic
 */
public final class SanitizeTextAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SanitizeTextAdvisor.class);
    private static final String NAME = "SanitizeTextAdvisor";
    private static final String SANITIZER_FAILURE_PLACEHOLDER = "[sanitizer failed — response withheld]";
    private final List<SanitizerEntry> sanitizers;

    private SanitizeTextAdvisor(Builder builder) {
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
        return LOWEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response;

        try {
            response = chain.nextCall(request);
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable t) {
            // Upstream call failure pre-sanitize is security-relevant: we cannot prove what the model produced was
            // safe, so withhold the response symmetric with the streaming path's onErrorResume behaviour rather than
            // letting the raw exception escape with any partial content. Restore the interrupt flag so cooperative
            // cancellation upstream still sees the thread as interrupted if the failure was a wrapped
            // InterruptedException from the reactor/HTTP pipeline.
            LlmClassifier.restoreInterruptIfWrapped(t);

            log.error("Upstream call errored before sanitization could run; withholding response", t);

            return withheldResponse(request);
        }

        try {
            return rewriteResponse(response);
        } catch (SanitizerExecutionFailureException e) {
            log.error(
                "Sanitization failed for synchronous call; withholding response to avoid leaking unredacted text", e);

            return withheldResponse(request);
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable t) {
            // Any other throwable (NPE, CCE, StackOverflowError from pathological input) must not leak the raw
            // model text upstream. Catch Throwable (minus OutOfMemoryError) so fail-closed coverage symmetry with
            // adviseStream holds even when sanitizers throw unchecked or Error subclasses. Restore the interrupt flag
            // if the failure wraps an InterruptedException.
            LlmClassifier.restoreInterruptIfWrapped(t);

            log.error(
                "Sanitization failed with unexpected throwable for synchronous call; withholding response", t);

            return withheldResponse(request);
        }
    }

    /**
     * Sanitizes each emitted {@link ChatClientResponse} independently as it arrives.
     *
     * <p>
     * <b>Known limitations of streaming sanitization:</b>
     * <ul>
     * <li><b>Cross-chunk regex gap:</b> sensitive patterns (PII, secrets) whose characters straddle two chunks are
     * <em>not</em> masked — e.g. the model emits {@code "alice@"} in one chunk and {@code "example.com"} in the next,
     * and neither chunk matches the e-mail regex in isolation.</li>
     * <li><b>Partial-leak on mid-stream failure:</b> when a sanitizer throws on chunk <i>N</i>, chunks 1..<i>N</i>-1
     * have already been delivered to the caller. Streaming fail-closed can only withhold the <em>remainder</em> (by
     * swapping to a placeholder via {@code onErrorResume}); it cannot recall previously-shipped chunks. A WARN-level
     * telemetry event is logged in that case — operators should alert on it.</li>
     * </ul>
     *
     * <p>
     * Buffering the full stream before sanitizing would close both gaps but defeats the streaming contract (no
     * incremental tokens for the caller). Callers that need full-text guarantees must use
     * {@link #adviseCall(ChatClientRequest, CallAdvisorChain)} instead, which sanitizes the complete assistant message.
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // Defer the subscription so each subscriber gets its own emittedBeforeError counter. Otherwise a shared counter
        // across subscribers would cross-contaminate the "how many chunks already shipped" signal used for the
        // partial-leak WARN below.
        return Flux.defer(() -> {
            AtomicInteger emittedBeforeError = new AtomicInteger();

            return chain.nextStream(request)
                .map(this::rewriteResponse)
                .doOnNext(emitted -> emittedBeforeError.incrementAndGet())
                .onErrorResume(error -> {
                    int alreadyShipped = emittedBeforeError.get();

                    if (error instanceof SanitizerExecutionFailureException) {
                        if (alreadyShipped > 0) {
                            // Partial-leak case: chunks 1..alreadyShipped already went to the caller before this
                            // failure. The streaming contract forbids recalling prior items; we can only swap the
                            // remainder to a placeholder. Log at ERROR with a distinct marker so operator alerts can
                            // page specifically on "streaming partial leak" (e.g. grep for "partial-leak" or match on
                            // the shipped-count) rather than collapsing it into the general "mid-stream failure"
                            // bucket. This is the streaming analogue of a fail-closed withhold — treat with the same
                            // urgency.
                            log.error(
                                "SanitizeTextAdvisor partial-leak: sanitization failed mid-stream after {} chunk(s) "
                                    + "already delivered to the caller; streaming fail-closed can only withhold the "
                                    + "remainder, not recall prior chunks. Withholding remainder.",
                                alreadyShipped, error);
                        } else {
                            log.error("Sanitization failed mid-stream; withholding response", error);
                        }
                    } else {
                        // Upstream stream failures pre-sanitize are equally security-relevant as sanitize-time failures
                        // — either way the response would have skipped masking, so log at ERROR for symmetric operator
                        // signal.
                        if (alreadyShipped > 0) {
                            log.error(
                                "SanitizeTextAdvisor partial-leak: upstream stream errored after {} chunk(s) had "
                                    + "already passed through; prior chunks reached the caller and cannot be recalled. "
                                    + "Withholding remainder.",
                                alreadyShipped, error);
                        } else {
                            log.error(
                                "Upstream stream errored before sanitization could run; withholding response to avoid "
                                    + "leaking unredacted text",
                                error);
                        }
                    }

                    return Flux.just(withheldResponse(request));
                });
        });
    }

    private static ChatClientResponse withheldResponse(ChatClientRequest request) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(SANITIZER_FAILURE_PLACEHOLDER))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();
    }

    private ChatClientResponse rewriteResponse(ChatClientResponse response) {
        if (sanitizers.isEmpty()) {
            return response;
        }

        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null || chatResponse.getResults() == null || chatResponse.getResults()
            .isEmpty()) {
            // Malformed/empty chat response: fail closed. The advisor cannot see what the model produced, so it cannot
            // prove the response is safe to pass through. Synthesising a SanitizerExecutionFailureException means the
            // outer adviseCall/adviseStream paths produce the withheldResponse placeholder symmetric with any other
            // sanitizer failure.
            throw new SanitizerExecutionFailureException(
                Map.of(NAME, new IllegalStateException(
                    "Received a null or empty chatResponse; expected a populated ChatResponse — check upstream "
                        + "advisor chain / model output shape.")));
        }

        List<SkippedSanitizerFailure> aggregatedSkipped = new ArrayList<>();

        List<Generation> rewrittenGenerations = chatResponse.getResults()
            .stream()
            .map(generation -> {
                AssistantMessage original = generation.getOutput();

                if (original == null) {
                    return generation;
                }

                String originalText = original.getText();

                if (originalText == null) {
                    // Null text means a tool-call-only or media-only generation. Log at ERROR so operators notice —
                    // tool-call payloads and media are NOT sanitized here; if sensitive data can flow through those
                    // channels in your workflow, add a dedicated sanitizer for them. The generation is passed through
                    // to preserve tool-using agent behaviour (withholding here would break every tool call).
                    log.error(
                        "SanitizeTextAdvisor skipped a generation with null text; tool calls and media payloads are " +
                            "NOT sanitized. If the response carries sensitive structured output, add a dedicated " +
                            "sanitizer for it.");

                    return generation;
                }

                SanitiseOutcome outcome = sanitise(originalText);

                aggregatedSkipped.addAll(outcome.skippedFailures());

                // Preserve tool calls, media, and message properties — downstream advisors (memory, logging,
                // structured-output converters) depend on them; dropping them via `new AssistantMessage(text)` would
                // silently break tool-using agents.
                AssistantMessage rewritten = AssistantMessage.builder()
                    .content(outcome.text())
                    .properties(original.getMetadata())
                    .toolCalls(original.getToolCalls())
                    .media(original.getMedia())
                    .build();

                return new Generation(rewritten, generation.getMetadata());
            })
            .toList();

        ChatResponse.Builder rewrittenChatResponseBuilder = ChatResponse.builder()
            .generations(rewrittenGenerations)
            .metadata(chatResponse.getMetadata());

        if (!aggregatedSkipped.isEmpty()) {
            // Symmetric with CheckForViolationsAdvisor: fail-open sanitizer crashes are log-visible but also reach
            // downstream telemetry via SKIPPED_FAILURES_METADATA_KEY so operator alerting can detect "text was
            // returned with a silently degraded sanitizer" without grepping logs. Each entry carries only the
            // sanitizer name + failureKind tag — raw cause messages never reach this channel (cause.getMessage() can
            // legitimately contain LLM output fragments).
            rewrittenChatResponseBuilder.metadata(SKIPPED_FAILURES_METADATA_KEY, aggregatedSkipped.stream()
                .map(SanitizeTextAdvisor::toPublicView)
                .toList());
        }

        return response.mutate()
            .chatResponse(rewrittenChatResponseBuilder.build())
            .build();
    }

    private static Map<String, Object> toPublicView(SkippedSanitizerFailure failure) {
        Map<String, Object> view = new LinkedHashMap<>();

        view.put("guardrail", failure.sanitizerName());
        view.put("executionFailed", true);
        view.put("failureKind", resolveFailureKind(failure.cause()));

        return view;
    }

    private static String resolveFailureKind(Throwable cause) {
        if (cause instanceof GuardrailException guardrailException) {
            return guardrailException.kind()
                .name();
        }

        return "UNKNOWN:" + cause.getClass()
            .getSimpleName();
    }

    /** Package-private seam for tests. */
    String sanitiseForTesting(String text) {
        return sanitise(text).text();
    }

    /** Package-private seam for tests — exposes the fail-open skipped-sanitizer track so assertions can pin it. */
    List<SkippedSanitizerFailure> sanitiseSkippedFailuresForTesting(String text) {
        return sanitise(text).skippedFailures();
    }

    private SanitiseOutcome sanitise(String text) {
        Map<String, Throwable> closedFailures = new LinkedHashMap<>();
        // Fail-open execution failures go here: they are surfaced to telemetry via SKIPPED_FAILURES_METADATA_KEY so
        // operators can distinguish "sanitizer cleanly passed" from "sanitizer crashed silently and text was
        // returned anyway". Parallel to CheckForViolationsAdvisor.skippedFailures.
        List<SkippedSanitizerFailure> skipped = new ArrayList<>();
        String intermediate = text;
        MaskEntityMap maskEntities = new MaskEntityMap();

        // Stage 1: PREFLIGHT (rule-based) sanitizers run first. Sanitizers that declare entities via
        // preflightMaskEntities participate in a single length-sorted global mask pass; apply()-based sanitizers mutate
        // text sequentially. Both reads operate on `intermediate` (the progressively-mutated text) — reading entity
        // spans off the original text while apply() reads the mutated text would produce ghost entities whose
        // substrings no longer appear in `intermediate` by the time maskEntities.applyTo runs.
        for (SanitizerEntry entry : sanitizers) {
            if (entry.function.stage() != GuardrailStage.PREFLIGHT) {
                continue;
            }

            try {
                // Masking is opt-in via PreflightMasking — LLM-stage sanitizers cannot declare this mixin, so the
                // "LLM sanitizers must not mask" invariant is compile-enforced. MaskResult is an exhaustive sealed
                // return type, so the advisor switches on exactly one of Entities / Masked / Unchanged. Unchanged
                // means "no work"; apply() is NOT called as a fallback — apply-style sanitizers do not implement
                // PreflightMasking (they are handled by the else-branch below), which preserves the "entity-only
                // sanitizer never double-detects" invariant that the previous usesMaskEntityMap flag encoded.
                if (entry.function instanceof PreflightMasking masking) {
                    MaskResult maskResult = masking.mask(intermediate, entry.context);

                    switch (maskResult) {
                        case MaskResult.Entities entitiesResult -> maskEntities.merge(entitiesResult.entities());
                        case MaskResult.Masked maskedResult -> intermediate = maskedResult.text();
                        case MaskResult.Unchanged ignored -> {
                            // no-op
                        }
                    }
                } else {
                    intermediate = entry.function.apply(intermediate, entry.context);
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();

                recordSanitizerFailure(entry, e, "interrupted", closedFailures, skipped);
            } catch (OutOfMemoryError e) {
                throw e;
            } catch (Throwable t) {
                // Catch Throwable (minus OOM) so StackOverflowError from pathological input, reactor-wrapped Errors,
                // and other Error subclasses are treated as fail-closed rather than escaping the advisor. OOM is
                // rethrown because the JVM is likely in an unstable state.
                recordSanitizerFailure(entry, t, "failed", closedFailures, skipped);
            }
        }

        if (!maskEntities.isEmpty()) {
            intermediate = maskEntities.applyTo(intermediate);
        }

        // Stage 2: LLM-based sanitizers see the already-masked text. Entity-based collection does not apply here —
        // LLM sanitizers transform text as a whole rather than emitting discrete spans.
        for (SanitizerEntry entry : sanitizers) {
            if (entry.function.stage() != GuardrailStage.LLM) {
                continue;
            }

            intermediate = collectingApply(entry, intermediate, closedFailures, skipped);
        }

        if (!closedFailures.isEmpty()) {
            throw new SanitizerExecutionFailureException(closedFailures);
        }

        return new SanitiseOutcome(intermediate, List.copyOf(skipped));
    }

    private static void recordSanitizerFailure(
        SanitizerEntry entry, Throwable cause, String verb, Map<String, Throwable> closedFailures,
        List<SkippedSanitizerFailure> skipped) {

        if (entry.failMode == FailMode.FAIL_OPEN) {
            log.warn(
                "Sanitizer '{}' {} (fail-open — skipping this sanitizer, recording as skipped-failure metadata, and "
                    + "continuing with the last-good text)",
                entry.sanitizerName, verb, cause);

            // Track the failure so it reaches the response metadata via SKIPPED_FAILURES_METADATA_KEY. Previously the
            // cause was log-only, which meant downstream telemetry could not distinguish "sanitizer cleanly passed"
            // from "sanitizer crashed silently under FAIL_OPEN and unsanitized text was returned".
            skipped.add(new SkippedSanitizerFailure(entry.sanitizerName, cause));

            return;
        }

        // Logged at ERROR because a fail-closed sanitizer means the caller's response will be replaced by the
        // withheld-placeholder — a production-visible event that operators need to alert on, consistent with
        // SanitizeTextAdvisor.adviseCall/adviseStream which also log withheld responses at ERROR.
        log.error("Sanitizer '{}' {} (failing closed — response will be withheld)", entry.sanitizerName, verb, cause);

        closedFailures.put(entry.sanitizerName, cause);
    }

    private String collectingApply(
        SanitizerEntry entry, String text, Map<String, Throwable> closedFailures,
        List<SkippedSanitizerFailure> skipped) {

        try {
            return entry.function.apply(text, entry.context);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();

            recordSanitizerFailure(entry, e, "interrupted", closedFailures, skipped);

            return text;
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable t) {
            recordSanitizerFailure(entry, t, "failed", closedFailures, skipped);

            return text;
        }
    }

    /**
     * Builder for {@link SanitizeTextAdvisor}.
     */
    public static final class Builder {

        private final List<SanitizerEntry> sanitizers = new ArrayList<>();

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters) {

            return add(sanitizerName, function, inputParameters, connectionParameters, null, Map.of());
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections) {

            return add(
                sanitizerName, function, inputParameters, connectionParameters, extensions, componentConnections, null);
        }

        public Builder add(
            String sanitizerName, GuardrailSanitizerFunction function, Parameters inputParameters,
            Parameters connectionParameters, Parameters extensions,
            Map<String, ComponentConnection> componentConnections, ChatClient chatClient) {

            GuardrailContext context = new GuardrailContext(
                inputParameters, connectionParameters, null, extensions, componentConnections, chatClient);

            FailMode failMode = resolveFailMode(inputParameters);

            sanitizers.add(new SanitizerEntry(sanitizerName, function, context, failMode));

            return this;
        }

        public SanitizeTextAdvisor build() {
            return new SanitizeTextAdvisor(this);
        }

        private static FailMode resolveFailMode(Parameters inputParameters) {
            if (inputParameters == null) {
                return FailMode.FAIL_CLOSED;
            }

            return FailMode.parse(inputParameters.getString(FAIL_MODE, FailMode.FAIL_CLOSED.name()), log);
        }
    }

    private record SanitizerEntry(
        String sanitizerName, GuardrailSanitizerFunction function, GuardrailContext context, FailMode failMode) {
    }

    /**
     * Bundle of the sanitize pass output: {@code text} is the rewritten text returned to the caller;
     * {@code skippedFailures} are fail-open sanitizer crashes that did not fail the pass but still need to reach
     * downstream telemetry via {@code SKIPPED_FAILURES_METADATA_KEY}.
     */
    record SanitiseOutcome(String text, List<SkippedSanitizerFailure> skippedFailures) {
    }

    /**
     * A single fail-open sanitizer failure captured during a sanitize pass. Exposed as package-private so tests can
     * assert on the skipped track via {@link #sanitiseSkippedFailuresForTesting}.
     */
    record SkippedSanitizerFailure(String sanitizerName, Throwable cause) {
    }
}
