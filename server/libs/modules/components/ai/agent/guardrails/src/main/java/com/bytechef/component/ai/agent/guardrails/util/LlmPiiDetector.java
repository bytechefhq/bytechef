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

package com.bytechef.component.ai.agent.guardrails.util;

import com.bytechef.component.ai.agent.guardrails.GuardrailOutputParseException;
import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.core.JacksonException;

/**
 * LLM-assisted PII detection. Uses Spring AI's {@code ChatClient.entity(Class)} terminal — the model is instructed to
 * return a structured list of {@link Span} records with {@code type} and {@code value}, and Spring AI's
 * {@code BeanOutputConverter} generates schema format instructions and parses the result.
 *
 * <p>
 * Hallucination guard: spans whose {@code value} is not an exact substring of the input are dropped.
 *
 * <p>
 * Failures (LLM outage, schema drift) surface as {@link GuardrailUnavailableException} so callers can fail closed.
 *
 * @author Ivica Cardic
 */
public final class LlmPiiDetector {

    private static final Logger log = LoggerFactory.getLogger(LlmPiiDetector.class);

    private static final String SYSTEM_MESSAGE =
        "You are a PII detector. Return every span of personally identifiable information in the input. " +
            "Only include types from the 'Requested types' list. Return an empty list when nothing matches. " +
            "Do not invent or guess values — each span's 'value' MUST be an exact substring of the input.";

    private LlmPiiDetector() {
    }

    public static List<Span> detect(
        String guardrailName, ChatClient chatClient, String text, List<String> requestedTypes) {

        if (text == null || text.isEmpty()) {
            return List.of();
        }

        // Empty requestedTypes would forward "Requested types: " (blank list) to the model; the system prompt says
        // "Only include types from the list", so the model returns no spans and the detector silently no-ops —
        // every PII value flows through unmasked with no error signal. Routing this as IllegalArgumentException
        // lets the advisor's isConfigurationError classifier turn it into a fail-closed violation (operator bug,
        // not transient outage) so the guardrail reveals itself instead of degrading silently. Cluster elements
        // should validate this higher up; this guard is the last line of defence.
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            throw new IllegalArgumentException(
                "LLM PII detector for guardrail '" + guardrailName + "' was invoked with no requested entity types. "
                    + "Select at least one entity type on the guardrail configuration.");
        }

        String userPrompt = "Requested types: " + String.join(", ", requestedTypes) + "\n\nInput:\n" + text;

        Response response;

        try {
            response = chatClient.prompt()
                .system(SYSTEM_MESSAGE)
                .user(userPrompt)
                .call()
                .entity(Response.class);
        } catch (JacksonException e) {
            // Schema drift / malformed JSON — distinct from a network/auth failure because the root cause is a prompt
            // or schema bug on our side. Mirrors LlmClassifier so the OUTPUT_PARSE kind is preserved downstream.
            log.error(
                "Guardrail '{}' LLM PII output parse failed (schema drift or prompt bug, not an upstream outage)",
                guardrailName, e);

            throw new GuardrailOutputParseException(
                guardrailName, "Failed to parse LLM PII output: " + e.getMessage(), e);
        } catch (OutOfMemoryError e) {
            // OOM is non-recoverable at this scope and must propagate unchanged so the JVM-level handler runs.
            throw e;
        } catch (Throwable e) {
            // Mirror LlmClassifier.classify — catch Throwable (minus OOM) so StackOverflowError on pathological
            // input, LinkageError from classpath drift, and other Error subclasses from inside Spring AI's
            // serializer are structured as GuardrailUnavailableException rather than escaping to the advisor as
            // opaque failures tagged "UNKNOWN".
            LlmClassifier.restoreInterruptIfWrapped(e);

            log.warn("Guardrail '{}' LLM PII detection failed (fail-closed)", guardrailName, e);

            throw new GuardrailUnavailableException(
                guardrailName, "LLM PII detection failed: " + e.getMessage(), e);
        }

        if (response == null) {
            // A null-parsed Response is an upstream bug — either the LLM returned nothing structured or Spring AI
            // could not deserialize to our record. Treat identically to the LlmClassifier.classify null-path so
            // callers can fail closed instead of silently reporting "no PII found".
            log.warn("Guardrail '{}' LLM PII returned a null-parsed response (failing closed)", guardrailName);

            throw new GuardrailUnavailableException(
                guardrailName, "LLM PII returned a null-parsed response");
        }

        if (response.spans() == null) {
            // Legitimately "model said nothing matched".
            return List.of();
        }

        List<Span> verified = new ArrayList<>();

        for (Span span : response.spans()) {
            if (span != null && span.value() != null && text.contains(span.value())) {
                verified.add(span);
            }
        }

        int returned = response.spans()
            .size();

        // When the model returns spans but every one fails the substring guard, the model is hallucinating. A 100%
        // hallucination ratio is treated as a degraded-model event and surfaced as GuardrailUnavailableException so
        // the caller fails closed — silently returning "no PII found" would let the prompt through unmasked while
        // signalling a security gap only via a WARN log.
        if (returned > 0 && verified.isEmpty()) {
            log.error(
                "Guardrail '{}' LLM PII returned {} span(s) but none were substrings of the input (100% "
                    + "hallucination, failing closed)",
                guardrailName, returned);

            throw new GuardrailUnavailableException(
                guardrailName,
                "LLM PII returned " + returned + " span(s) but none matched the input (100% hallucination)");
        }

        if (returned > verified.size()) {
            log.warn(
                "Guardrail '{}' LLM PII dropped {} of {} returned span(s) as hallucinations",
                guardrailName, returned - verified.size(), returned);
        }

        return List.copyOf(verified);
    }

    public record Span(String type, String value) {
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record Response(List<Span> spans) {
        public Response {
            spans = spans == null ? null : List.copyOf(spans);
        }
    }
}
