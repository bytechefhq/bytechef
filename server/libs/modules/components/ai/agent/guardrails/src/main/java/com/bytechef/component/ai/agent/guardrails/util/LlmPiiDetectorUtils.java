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
import com.bytechef.component.definition.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.core.JacksonException;

/**
 * LLM-assisted PII detection. Spans whose value is not an exact substring of the input are dropped as hallucinations.
 *
 * @author Ivica Cardic
 */
public final class LlmPiiDetectorUtils {

    private static final String SYSTEM_MESSAGE =
        "You are a PII detector. Return every span of personally identifiable information in the input. " +
            "Only include types from the 'Requested types' list. Return an empty list when nothing matches. " +
            "Do not invent or guess values — each span's 'value' MUST be an exact substring of the input.";

    private LlmPiiDetectorUtils() {
    }

    public static List<Span> detect(
        String guardrailName, ChatClient chatClient, String text, List<String> requestedTypes, Context context) {

        Objects.requireNonNull(context, "context");

        if (text == null || text.isEmpty()) {
            return List.of();
        }

        if (requestedTypes == null || requestedTypes.isEmpty()) {
            throw new IllegalArgumentException("'" + guardrailName + "' requires at least one entity type");
        }

        String instructions = "Requested types: " + String.join(", ", requestedTypes);
        String userPrompt = LlmClassifierUtils.fenceUserInput(instructions, text);

        Response response;

        try {
            response = chatClient.prompt()
                .system(SYSTEM_MESSAGE)
                .user(userPrompt)
                .call()
                .entity(Response.class);
        } catch (JacksonException exception) {
            throw new GuardrailOutputParseException(
                guardrailName, "Failed to parse LLM PII output: " + exception.getMessage(), exception);
        } catch (OutOfMemoryError error) {
            throw error;
        } catch (Throwable throwable) {
            LlmClassifierUtils.restoreInterruptIfWrapped(throwable);

            throw new GuardrailUnavailableException(
                guardrailName, "LLM PII detection failed: " + throwable.getMessage(), throwable);
        }

        if (response == null) {
            throw new GuardrailUnavailableException(guardrailName, "LLM PII returned a null-parsed response");
        }

        List<Span> spans = response.spans();

        if (spans == null) {
            throw new GuardrailUnavailableException(
                guardrailName, "LLM PII returned a null spans field (expected empty list for 'no PII')");
        }

        List<Span> verified = new ArrayList<>();
        List<String> droppedTypes = new ArrayList<>();

        for (Span span : spans) {
            String value = span == null ? null : span.value();

            // Whitespace-only spans (e.g. a single space, "\n") technically appear in most input but masking them
            // corrupts every space in the response — reject as a hallucination.
            if (value != null && !value.isBlank() && text.contains(value)) {
                verified.add(span);
            } else if (span != null && span.type() != null) {
                droppedTypes.add(span.type());
            } else {
                droppedTypes.add("<null>");
            }
        }

        int returned = spans.size();

        if (returned > 0 && verified.isEmpty()) {
            throw new GuardrailUnavailableException(
                guardrailName,
                "LLM PII returned " + returned + " span(s) but none matched the input (100% hallucination); "
                    + "dropped types=" + droppedTypes);
        }

        if (returned > 0 && droppedTypes.size() * 2 > returned) {
            throw new GuardrailUnavailableException(
                guardrailName,
                "LLM PII dropped " + droppedTypes.size() + " of " + returned
                    + " returned span(s) as hallucinations (>50%); model unreliable, failing closed. "
                    + "Dropped types=" + droppedTypes);
        }

        if (returned > verified.size()) {
            int dropped = returned - verified.size();
            List<String> droppedTypesCopy = List.copyOf(droppedTypes);

            context.log(contextLog -> contextLog.error(
                "Guardrail '" + guardrailName + "' LLM PII dropped " + dropped + " of " + returned
                    + " returned span(s) as hallucinations; droppedTypes=" + droppedTypesCopy));
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
