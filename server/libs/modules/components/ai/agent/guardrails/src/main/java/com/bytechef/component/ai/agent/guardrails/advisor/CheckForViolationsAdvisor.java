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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VIOLATIONS_METADATA_KEY;

import com.bytechef.component.ai.agent.guardrails.GuardrailException;
import com.bytechef.component.ai.agent.guardrails.GuardrailExceptionKind;
import com.bytechef.component.ai.agent.guardrails.GuardrailOutputParseException;
import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.constant.FailMode;
import com.bytechef.component.ai.agent.guardrails.util.MaskEntityMap;
import com.bytechef.component.ai.agent.guardrails.util.RegexParser;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightMasking;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

/**
 * Spring-AI advisor that intercepts the user prompt and runs every configured guardrail check. All violations from
 * every check are aggregated into a {@link List} so downstream consumers (and observability) see every guardrail that
 * fired, not only the first. When the aggregated list is non-empty the advisor returns a blocked response without
 * forwarding to the model.
 *
 * <p>
 * <b>Failure policy</b> is per-check via the {@code failMode} parameter (see {@link FailMode#parse}):
 * <ul>
 * <li><b>FAIL_CLOSED</b> (default) — any exception that escapes a check function becomes a fail-closed
 * {@link Violation#ofExecutionFailure(String, Throwable) execution-failure} violation, so a broken guardrail blocks
 * requests until an operator fixes it rather than silently disappearing from the security perimeter.</li>
 * <li><b>FAIL_OPEN</b> — transient exceptions (e.g. upstream LLM outage, network failure) are logged at WARN and the
 * check is skipped: the failure does not block the request and is not surfaced as a violation. Use sparingly — the
 * operator is trading availability for security and must monitor the WARN log to know the guardrail is degraded.</li>
 * </ul>
 *
 * <p>
 * <b>Configuration-error override:</b> regardless of {@code failMode}, the exception classes enumerated in
 * {@link #isConfigurationError(Throwable)} (invalid regex, missing MODEL child, programming bugs such as NPE/CCE, etc.)
 * always fail closed. These indicate operator bugs or programming errors rather than transient outages, so treating
 * them as FAIL_OPEN would let a tenant believe protection is in place while it is silently doing nothing.
 *
 * <p>
 * A failing check does not abort the loop; remaining checks still run, so a single broken guardrail does not mask
 * violations that other checks would have found.
 *
 * @author Ivica Cardic
 */
public final class CheckForViolationsAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(CheckForViolationsAdvisor.class);
    private static final String NAME = "CheckForViolationsAdvisor";

    private final String blockedMessage;
    private final List<CheckEntry> checks;

    private CheckForViolationsAdvisor(Builder builder) {
        this.blockedMessage = builder.blockedMessage;
        this.checks = List.copyOf(builder.checks);
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
        CheckOutcome outcome = runChecks(request);

        if (!outcome.violations.isEmpty()) {
            return blockedResponse(request, outcome);
        }

        ChatClientResponse response = chain.nextCall(request);

        // Fail-closed when upstream produced a malformed response AND we have fail-open skipped failures to surface:
        // silently forwarding would erase the "guardrail X crashed under FAIL_OPEN" telemetry signal (metadata has
        // nowhere to attach). Returning a blocked response keeps the skipped-failures track reachable for alerting
        // and makes the double-failure ("fail-open crash + malformed upstream") visible.
        if (!outcome.skippedFailures.isEmpty() && (response == null || response.chatResponse() == null)) {
            log.warn(
                "Upstream returned {} while {} fail-open guardrail(s) had crashed; failing closed to preserve "
                    + "skipped-failures telemetry",
                response == null ? "a null response" : "a response with null chatResponse",
                outcome.skippedFailures.size());

            return blockedResponse(request, outcome);
        }

        return attachSkippedFailures(response, outcome);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        CheckOutcome outcome = runChecks(request);

        if (!outcome.violations.isEmpty()) {
            return Flux.just(blockedResponse(request, outcome));
        }

        return chain.nextStream(request)
            .map(response -> attachSkippedFailures(response, outcome));
    }

    /** Package-private seam for tests. */
    List<Violation> runChecksForTesting(ChatClientRequest request) {
        return runChecks(request).violations;
    }

    /** Package-private seam for tests — exposes the fail-open skipped-failures track so assertions can pin it. */
    List<Violation> runChecksSkippedFailuresForTesting(ChatClientRequest request) {
        return runChecks(request).skippedFailures;
    }

    private CheckOutcome runChecks(ChatClientRequest request) {
        String userText = extractUserText(request);

        if (userText.isEmpty()) {
            // Fail-closed: refuse to forward to the model when no user text was found. A tool-only, media-only, or
            // malformed multi-part request has no user prompt for us to validate; passing it through would let the
            // model process inputs that bypassed every configured guardrail.
            log.warn(
                "CheckForViolationsAdvisor found no USER message with non-empty text; blocking request (fail-closed) "
                    + "with {} configured check(s). Verify the agent is being invoked with a user prompt.",
                checks.size());

            return new CheckOutcome(
                List.of(Violation.ofExecutionFailure(
                    "CheckForViolationsAdvisor",
                    new IllegalStateException(
                        "No USER message with non-empty text found; cannot run configured guardrails"))),
                List.of());
        }

        List<Violation> aggregated = new ArrayList<>();
        // Fail-open execution failures go here: they are surfaced to telemetry via SKIPPED_FAILURES_METADATA_KEY so
        // operators can distinguish "guardrail cleanly passed" from "guardrail crashed silently and request was
        // forwarded anyway", without blocking the request.
        List<Violation> skippedFailures = new ArrayList<>();
        String textForLlm = userText;
        MaskEntityMap maskEntities = new MaskEntityMap();

        // Stage 1: PREFLIGHT (rule-based) — runs against the progressively-mutated user text and may mask.
        for (CheckEntry entry : checks) {
            if (entry.function.stage() != GuardrailStage.PREFLIGHT) {
                continue;
            }

            try {
                // Feed the progressively-mutated `textForLlm` to apply() AND preflightMaskEntities uniformly.
                // Earlier, apply() saw the raw `userText` while preflightMaskEntities saw `textForLlm`: when a
                // prior apply()-based check mutated `textForLlm`, a later entity-based check would still detect
                // against the raw text, report entities that no longer existed in the mutated text, and silently
                // fail to mask them when maskEntities.applyTo(textForLlm) ran below. Mirrors
                // SanitizeTextAdvisor.sanitise, which has always fed `intermediate` uniformly.
                Optional<Violation> result = entry.function.apply(textForLlm, entry.context);

                result.ifPresent(aggregated::add);

                // Masking is opt-in via PreflightMasking — LLM-stage guardrails cannot declare this mixin, so the
                // "LLM guardrails must not mask" invariant is compile-enforced. The MaskResult sealed return type
                // makes the dispatch exhaustive at compile time: a guardrail returns exactly one of Entities /
                // Masked / Unchanged, and the advisor switches on it without any flag-based dispatch.
                if (entry.function instanceof PreflightMasking masking) {
                    MaskResult maskResult = masking.mask(textForLlm, entry.context);

                    switch (maskResult) {
                        case MaskResult.Entities entitiesResult -> maskEntities.merge(entitiesResult.entities());
                        case MaskResult.Masked maskedResult -> textForLlm = maskedResult.text();
                        case MaskResult.Unchanged ignored -> {
                            // no-op
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();

                recordFailure(entry, e, aggregated, skippedFailures, "interrupted");
            } catch (OutOfMemoryError e) {
                throw e;
            } catch (Throwable t) {
                // Catch Throwable (minus OOM) so StackOverflowError from pathological input and other Error subclasses
                // are treated as fail-closed rather than escaping the advisor chain.
                recordFailure(entry, t, aggregated, skippedFailures, "failed");
            }
        }

        if (!maskEntities.isEmpty()) {
            textForLlm = maskEntities.applyTo(textForLlm);
        }

        // Stage 2: LLM — runs against masked text.
        for (CheckEntry entry : checks) {
            if (entry.function.stage() != GuardrailStage.LLM) {
                continue;
            }

            try {
                Optional<Violation> result = entry.function.apply(textForLlm, entry.context);

                result.ifPresent(aggregated::add);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();

                recordFailure(entry, e, aggregated, skippedFailures, "interrupted");
            } catch (OutOfMemoryError e) {
                throw e;
            } catch (Throwable t) {
                recordFailure(entry, t, aggregated, skippedFailures, "failed");
            }
        }

        logViolations(request, aggregated, false);
        logViolations(request, skippedFailures, true);

        return new CheckOutcome(aggregated, skippedFailures);
    }

    private static void logViolations(ChatClientRequest request, List<Violation> violations, boolean skipped) {
        if (violations.isEmpty()) {
            return;
        }

        String correlation = resolveCorrelation(request);

        for (Violation violation : violations) {
            // Render confidenceScore as a plain value or "-" so logs don't show a placeholder for pattern and
            // execution-failure variants (which never carry a score). Pattern-match on the sealed hierarchy — each
            // variant exposes its own state on its record accessor.
            String confidenceScore = switch (violation) {
                case Violation.ClassifiedViolation classified -> Double.toString(classified.confidenceScore());
                case Violation.PatternViolation ignored -> "-";
                case Violation.ExecutionFailureViolation ignored -> "-";
            };

            boolean executionFailed = violation instanceof Violation.ExecutionFailureViolation;

            if (skipped) {
                log.warn(
                    "Guardrail skipped (fail-open): guardrail={}, executionFailed={}, correlation={}",
                    violation.guardrail(), executionFailed, correlation);
            } else {
                log.warn(
                    "Guardrail violation detected: guardrail={}, confidenceScore={}, executionFailed={}, "
                        + "correlation={}",
                    violation.guardrail(), confidenceScore, executionFailed, correlation);
            }
        }
    }

    /**
     * Resolve a correlation token for the blocked/skipped log lines so operators can map "guardrail fired at 14:02"
     * back to a specific agent invocation. Pulls from {@code request.context()} using a small set of commonly-used
     * trace keys — if none are present, falls back to "-". Never throws.
     */
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

    // Common correlation keys (Spring AI conversation id, OpenTelemetry trace/span, MDC). Check in priority order —
    // the first present key wins. Keep this list short: every extra key is one more lookup per violation.
    private static final List<String> CORRELATION_KEYS = List.of(
        "conversationId", "traceId", "spanId", "requestId", "correlationId");

    private static void recordFailure(
        CheckEntry entry, Throwable cause, List<Violation> aggregated, List<Violation> skippedFailures, String verb) {

        if (entry.failMode == FailMode.FAIL_OPEN && !isConfigurationError(cause)) {
            log.warn(
                "Guardrail check '{}' {} (fail-open — recording as skipped-failure metadata; request not blocked)",
                entry.guardrailName, verb, cause);

            // Track the failure so it still reaches the metadata channel (under SKIPPED_FAILURES_METADATA_KEY).
            // Previously the cause was log-only, which meant downstream telemetry could not distinguish "guardrail
            // cleanly passed" from "guardrail crashed silently under FAIL_OPEN".
            skippedFailures.add(Violation.ofExecutionFailure(entry.guardrailName, cause));

            return;
        }

        // Logged at ERROR because a fail-closed path means a production request is about to be blocked — operators
        // watching for ERROR-level alerts need to see "every request blocked because guardrail X is
        // down/misconfigured".
        log.error("Guardrail check '{}' {} (failing closed — request will be blocked)",
            entry.guardrailName, verb, cause);

        aggregated.add(Violation.ofExecutionFailure(entry.guardrailName, cause));
    }

    /**
     * Exceptions that must always fail closed regardless of {@code failMode}. These are operator bugs or programming
     * errors, not transient upstream outages — FAIL_OPEN on them would let the user believe protection is in place
     * while silently doing nothing.
     * <ul>
     * <li>{@link IllegalArgumentException}, {@link java.util.regex.PatternSyntaxException} — invalid regex / malformed
     * settings.</li>
     * <li>{@link IllegalStateException} — detector invariant violation (e.g. unexpected internal state). Kept in
     * lockstep with {@code LlmClassifier.isUnrecoverable}, which treats the same type as non-transient: asymmetry
     * between the two would let a FAIL_OPEN tenant silently forward on an invariant bug while the WARN-vs-ERROR log
     * level still reported it as permanent.</li>
     * <li>{@link MissingModelChildException} — LLM guardrail wired without a MODEL child cluster element.</li>
     * <li>{@link GuardrailOutputParseException} — schema drift or prompt bug on our side, not an upstream outage.</li>
     * <li>{@link RegexParser.RegexExecutionLimitException} — operator-supplied pathological regex hit the DoS bound;
     * the guardrail is effectively inert and must block, not forward.</li>
     * <li>{@link NullPointerException}, {@link ClassCastException} — programming bugs that escape the detector code.
     * Treating them as transient would let FAIL_OPEN mask real defects.</li>
     * </ul>
     */
    private static boolean isConfigurationError(Throwable cause) {
        return cause instanceof IllegalArgumentException
            || cause instanceof java.util.regex.PatternSyntaxException
            || cause instanceof IllegalStateException
            || cause instanceof MissingModelChildException
            || cause instanceof GuardrailOutputParseException
            || cause instanceof RegexParser.RegexExecutionLimitException
            || cause instanceof NullPointerException
            || cause instanceof ClassCastException;
    }

    private ChatClientResponse blockedResponse(ChatClientRequest request, CheckOutcome outcome) {
        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(blockedMessage))))
            // Project each Violation through toPublicView() so preflight-internal keys (e.g. maskEntities) never reach
            // downstream workflow consumers via the blocked response metadata. The raw Violation objects stay inside
            // the advisor for local observability.
            .metadata(VIOLATIONS_METADATA_KEY, outcome.violations.stream()
                .map(CheckForViolationsAdvisor::toPublicView)
                .toList());

        if (!outcome.skippedFailures.isEmpty()) {
            // Even on a blocked response, surface the fail-open skipped failures so telemetry can see that other
            // guardrails degraded in the same request — otherwise a blocked request would hide any co-occurring
            // silent-degradation events.
            chatResponseBuilder.metadata(SKIPPED_FAILURES_METADATA_KEY, outcome.skippedFailures.stream()
                .map(CheckForViolationsAdvisor::toPublicView)
                .toList());
        }

        return ChatClientResponse.builder()
            .chatResponse(chatResponseBuilder.build())
            .context(request.context())
            .build();
    }

    /**
     * Attach fail-open skipped-failure metadata to an unblocked response. Used by the pass-through paths in
     * {@link #adviseCall} and {@link #adviseStream} so downstream telemetry can distinguish "guardrail cleanly passed"
     * from "guardrail crashed silently under FAIL_OPEN and request was forwarded anyway". Returns the original response
     * unchanged when there are no skipped failures, so the fast path is free of allocation.
     */
    private static ChatClientResponse attachSkippedFailures(ChatClientResponse response, CheckOutcome outcome) {
        if (outcome.skippedFailures.isEmpty() || response == null) {
            return response;
        }

        ChatResponse chatResponse = response.chatResponse();

        if (chatResponse == null) {
            // Reached only from adviseStream — adviseCall short-circuits to blockedResponse for the null-chatResponse
            // branch. A malformed streaming chunk is forwarded unchanged (swapping to a blocked chunk mid-stream
            // would confuse the consumer), but we log at WARN so the "fail-open guardrail crashed + malformed
            // upstream chunk" double-failure is not invisible to operators.
            log.warn(
                "Streaming chunk has null chatResponse; cannot attach SKIPPED_FAILURES_METADATA_KEY for {} "
                    + "skipped-failure(s): {}",
                outcome.skippedFailures.size(),
                outcome.skippedFailures.stream()
                    .map(Violation::guardrail)
                    .toList());

            return response;
        }

        List<Map<String, Object>> views = outcome.skippedFailures.stream()
            .map(CheckForViolationsAdvisor::toPublicView)
            .toList();

        ChatResponse mutatedChatResponse = ChatResponse.builder()
            .from(chatResponse)
            .metadata(SKIPPED_FAILURES_METADATA_KEY, views)
            .build();

        return response.mutate()
            .chatResponse(mutatedChatResponse)
            .build();
    }

    /**
     * Public view of a violation emitted on the blocked-response metadata. Raw matched substrings (which for PII,
     * secret-key, and URL detectors are the sensitive values themselves) are deliberately NOT included — only a
     * {@code matchCount} so downstream consumers can see that a guardrail fired without the raw values leaking through
     * the metadata channel. The advisor still has the full List<Violation> for local observability and server logs.
     */
    private static Map<String, Object> toPublicView(Violation violation) {
        Map<String, Object> view = new LinkedHashMap<>();

        view.put("guardrail", violation.guardrail());

        // matchCount defaults to 0 for variants without concrete substrings; each variant contributes its own state
        // via the sealed switch so the shape stays stable across variants while the internals are pattern-specific.
        int matchCount = switch (violation) {
            case Violation.PatternViolation pattern -> pattern.matchedSubstrings()
                .size();
            case Violation.ClassifiedViolation ignored -> 0;
            case Violation.ExecutionFailureViolation ignored -> 0;
        };

        view.put("matchCount", matchCount);
        view.put("executionFailed", violation instanceof Violation.ExecutionFailureViolation);
        view.put("info", violation.info());

        switch (violation) {
            case Violation.ExecutionFailureViolation failure -> view.put("failureKind",
                resolveFailureKind(failure.exception()));
            case Violation.ClassifiedViolation classified -> view.put("confidenceScore", classified.confidenceScore());
            case Violation.PatternViolation ignored -> {
                // no variant-specific fields beyond matchCount/info
            }
        }

        return view;
    }

    /**
     * Project a cause onto the stable {@link GuardrailExceptionKind} enum so downstream consumers see a version-stable
     * tag rather than a runtime class name that would drift on rename. Configuration errors (operator-supplied bad
     * regex, programming bugs escaping detector code) surface as {@code CONFIGURATION:<class>} so alerting pipelines
     * can page on them rather than treat them as transient upstream outages. Truly unknown causes surface as
     * {@code UNKNOWN:<class>} with the simple class name as a fallback so operators still get a human-readable hint.
     */
    private static String resolveFailureKind(Throwable cause) {
        if (cause instanceof GuardrailException guardrailException) {
            return guardrailException.kind()
                .name();
        }

        if (isConfigurationError(cause)) {
            return GuardrailExceptionKind.CONFIGURATION.name() + ":" + cause.getClass()
                .getSimpleName();
        }

        return "UNKNOWN:" + cause.getClass()
            .getSimpleName();
    }

    /**
     * Extract text from every USER message in turn order, joined by newlines. Scanning the full USER history rather
     * than only the last turn closes the multi-turn bypass where an attacker buries PII / jailbreak content in an
     * earlier turn. Tool-call and media-only messages contribute empty text; the advisor blocks-closed when no user
     * text is found — see {@link #runChecks(ChatClientRequest)}.
     */
    private static String extractUserText(ChatClientRequest request) {
        List<Message> messages = request.prompt()
            .getInstructions();

        StringBuilder combined = new StringBuilder();

        for (Message message : messages) {
            if (message.getMessageType() != MessageType.USER) {
                continue;
            }

            String text = message.getText();

            if (text == null || text.isEmpty()) {
                continue;
            }

            if (!combined.isEmpty()) {
                combined.append('\n');
            }

            combined.append(text);
        }

        return combined.toString();
    }

    /**
     * Builder for {@link CheckForViolationsAdvisor}.
     */
    public static final class Builder {

        private String blockedMessage = "";
        private final List<CheckEntry> checks = new ArrayList<>();

        public Builder blockedMessage(String value) {
            this.blockedMessage = value;

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

            GuardrailContext context = new GuardrailContext(
                inputParameters, connectionParameters, parentParameters, extensions, componentConnections, chatClient);

            FailMode failMode = resolveFailMode(inputParameters);

            checks.add(new CheckEntry(guardrailName, function, context, failMode));

            return this;
        }

        public CheckForViolationsAdvisor build() {
            return new CheckForViolationsAdvisor(this);
        }

        private static FailMode resolveFailMode(Parameters inputParameters) {
            if (inputParameters == null) {
                return FailMode.FAIL_CLOSED;
            }

            return FailMode.parse(inputParameters.getString(FAIL_MODE, FailMode.FAIL_CLOSED.name()), log);
        }
    }

    private record CheckEntry(
        String guardrailName, GuardrailCheckFunction function, GuardrailContext context, FailMode failMode) {
    }

    /**
     * Bundle of the two violation tracks produced by a single {@code runChecks} pass: {@code violations} are the
     * blocking findings (cause a blocked response); {@code skippedFailures} are fail-open execution failures that did
     * not block but still need to reach downstream telemetry so "silently degraded guardrail" is observable from the
     * response shape alone.
     */
    private record CheckOutcome(List<Violation> violations, List<Violation> skippedFailures) {
    }
}
