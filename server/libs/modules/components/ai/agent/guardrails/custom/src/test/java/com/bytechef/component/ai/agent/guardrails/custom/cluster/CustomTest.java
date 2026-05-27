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

package com.bytechef.component.ai.agent.guardrails.custom.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.DEFAULT_SYSTEM_MESSAGE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.NAME;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.PROMPT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.THRESHOLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifierUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.core.ParameterizedTypeReference;

@ExtendWith(ObjectMapperSetupExtension.class)
class CustomTest {

    @Test
    void testFlaggedAboveThresholdProducesViolationWithConfiguredName() {
        ChatClient client = mockClient(new LlmClassifierUtils.Response(0.9, true));

        Optional<Violation> violation = Custom.classifyWith(
            client, "my-custom-guardrail", "Detect references to competitor products.", 0.7, DEFAULT_SYSTEM_MESSAGE,
            "Have you tried using CompetitorApp instead?");

        assertThat(violation).isPresent();

        Violation violation1 = violation.get();

        assertThat(violation1.guardrail()).isEqualTo("my-custom-guardrail");

        assertThat(((Violation.ClassifiedViolation) violation1).confidenceScore()).isEqualTo(0.9);
    }

    @Test
    void testBelowThresholdNoViolation() {
        ChatClient client = mockClient(new LlmClassifierUtils.Response(0.5, false));

        Optional<Violation> violation = Custom.classifyWith(
            client, "my-guardrail", "Detect competitor mentions.", 0.7, DEFAULT_SYSTEM_MESSAGE, "clean text");

        assertThat(violation).isEmpty();
    }

    @Test
    void testExceptionFailsClosed() {
        ChatClient client = mock(ChatClient.class);

        when(client.prompt()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(
            () -> Custom.classifyWith(
                client, "my-guardrail", "Detect competitor mentions.", 0.7, DEFAULT_SYSTEM_MESSAGE, "text"))
                    .isInstanceOf(GuardrailUnavailableException.class)
                    .hasMessageContaining("my-guardrail");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCustomGuardrailWithResponseSchemaSurfacesExtrasInViolationInfo() throws Exception {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        when(chatClient.prompt()
            .system(anyString())
            .messages(anyList())
            .user(anyString())
            .call()
            .entity(any(ParameterizedTypeReference.class)))
                .thenReturn(Map.of(
                    "flagged", true,
                    "confidenceScore", 0.85,
                    "reason", "spam"));

        GuardrailCheckFunction check = Custom.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of(
            "guardrails", java.util.List.of(Map.of(
                NAME, "spamCheck",
                PROMPT, "Is this spam?",
                RESPONSE_SCHEMA, "{}",
                THRESHOLD, 0.7))));

        GuardrailContext context = new GuardrailContext(
            inputParameters, ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), chatClient, mock(Context.class));

        Optional<Violation> result = check.apply("hello", context);

        assertThat(result).isPresent();

        Violation violation = result.get();

        assertThat(((Violation.ClassifiedViolation) violation).confidenceScore()).isEqualTo(0.85);
        assertThat(violation.info()).containsEntry("reason", "spam");
    }

    @Test
    void testMultipleGuardrailEntriesAllRunInOneInstance() throws Exception {
        ChatClient chatClient = mockClient(new LlmClassifierUtils.Response(0.9, true));
        GuardrailCheckFunction function = Custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "no-competitors", PROMPT, "Detect competitor mentions"),
                    Map.of(NAME, "no-commitments", PROMPT, "Detect commitment language")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        java.util.List<Violation> violations = function.applyAll("some input", context);

        assertThat(violations).hasSize(2);
        assertThat(violations.stream()
            .map(Violation::guardrail)
            .toList())
                .containsExactly("no-competitors", "no-commitments");
    }

    @Test
    void testMultipleGuardrailsEmptyEntryIsRejected() {
        GuardrailCheckFunction function = Custom.of()
            .getElement();

        ChatClient chatClient = mockClient(new LlmClassifierUtils.Response(0.1, false));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(Map.of(NAME, "", PROMPT, "p")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        assertThatThrownBy(() -> function.apply("x", context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-empty");
    }

    @Test
    void testEmptyGuardrailsListThrowsConfigurationError() {
        // The Classifiers list is required; an empty array is a configuration error that the advisor's
        // isConfigurationError branch routes to fail-closed.
        GuardrailCheckFunction function = Custom.of()
            .getElement();

        ChatClient chatClient = mockClient(new LlmClassifierUtils.Response(0.9, true));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of("guardrails", java.util.List.of())),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        assertThatThrownBy(() -> function.apply("sensitive", context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one entry");
    }

    @Test
    void testSingleEntryGuardrailsListExecutesEntry() throws Exception {
        // A guardrails list with one entry runs that entry exactly the same way as a multi-entry list.
        GuardrailCheckFunction function = Custom.of()
            .getElement();

        ChatClient chatClient = mockClient(new LlmClassifierUtils.Response(0.9, true));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "array-entry", PROMPT, "Prompt from array")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        Optional<Violation> violation = function.apply("sensitive", context);

        assertThat(violation).isPresent();

        Violation violation1 = violation.get();

        assertThat(violation1.guardrail()).isEqualTo("array-entry");
    }

    @Test
    void testMultipleFlaggedEntriesEmitOneViolationPerEntry() throws Exception {
        ChatClient chatClient = mockClient(new LlmClassifierUtils.Response(0.9, true));

        GuardrailCheckFunction function = Custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "first", PROMPT, "first prompt"),
                    Map.of(NAME, "second", PROMPT, "second prompt"),
                    Map.of(NAME, "third", PROMPT, "third prompt")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        java.util.List<Violation> violations = function.applyAll("input", context);

        assertThat(violations)
            .hasSize(3)
            .allMatch(violation -> violation instanceof Violation.ClassifiedViolation);
        assertThat(violations.stream()
            .map(Violation::guardrail)
            .toList())
                .containsExactly("first", "second", "third");
    }

    @Test
    void testPartialEntryFailureEmitsExecutionFailureViolationAlongsideClassifiedViolation() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class, RETURNS_DEEP_STUBS);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.messages(anyList())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class)))
            .thenReturn(new LlmClassifierUtils.Response(0.9, true))
            .thenThrow(new RuntimeException("LLM down"));

        GuardrailCheckFunction function = Custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "flagged-entry", PROMPT, "Flags"),
                    Map.of(NAME, "broken-entry", PROMPT, "Times out")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        java.util.List<Violation> violations = function.applyAll("input", context);

        assertThat(violations).hasSize(2);

        Violation classified = violations.get(0);

        assertThat(classified)
            .isInstanceOf(Violation.ClassifiedViolation.class);
        assertThat(classified.guardrail()).isEqualTo("flagged-entry");

        Violation failure = violations.get(1);

        assertThat(failure)
            .isInstanceOf(Violation.ExecutionFailureViolation.class);
        assertThat(failure.guardrail()).isEqualTo("custom:broken-entry");
        assertThat(((Violation.ExecutionFailureViolation) failure).exception())
            .hasMessageContaining("LLM down");
    }

    @Test
    void testAllEntriesFailedThrowsGuardrailUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class, RETURNS_DEEP_STUBS);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.messages(anyList())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class)))
            .thenThrow(new RuntimeException("LLM down for entry 1"))
            .thenThrow(new RuntimeException("LLM down for entry 2"));

        GuardrailCheckFunction function = Custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "entry-a", PROMPT, "P"),
                    Map.of(NAME, "entry-b", PROMPT, "P")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        assertThatThrownBy(() -> function.apply("input", context))
            .isInstanceOf(GuardrailUnavailableException.class)
            .hasMessageContaining("Every Custom guardrail entry failed");
    }

    @Test
    void testThresholdOutOfRangeAboveOneFailsClosed() {
        assertThresholdFailsClosed(1.5, "out-of-range threshold");
    }

    @Test
    void testThresholdOutOfRangeBelowZeroFailsClosed() {
        assertThresholdFailsClosed(-0.1, "out-of-range threshold");
    }

    @Test
    void testThresholdNaNFailsClosed() {
        assertThresholdFailsClosed(Double.NaN, "out-of-range threshold");
    }

    @Test
    void testThresholdUnparseableStringFailsClosed() {
        assertThresholdFailsClosed("not-a-number", "unparseable threshold");
    }

    private static void assertThresholdFailsClosed(Object thresholdValue, String expectedFragment) {
        ChatClient chatClient = mockClient(new LlmClassifierUtils.Response(0.9, true));
        GuardrailCheckFunction function = Custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "bad-threshold", PROMPT, "Detect X", THRESHOLD, thresholdValue)))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient,
            mock(Context.class));

        // The bad threshold surfaces as a per-entry execution-failure violation rather than throwing through
        // the apply() method — Custom collects entry failures so siblings can still report.
        java.util.List<Violation> results;
        try {
            results = function.applyAll("any text", context);
        } catch (Exception exception) {
            assertThat(exception)
                .hasMessageContaining("bad-threshold")
                .hasMessageContaining(expectedFragment);

            return;
        }

        assertThat(results)
            .as("bad-threshold entry must surface as an ExecutionFailureViolation or throw")
            .anySatisfy(violation -> assertThat(violation)
                .isInstanceOfSatisfying(Violation.ExecutionFailureViolation.class,
                    failure -> assertThat(failure.exception()
                        .getMessage())
                            .contains("bad-threshold")
                            .contains(expectedFragment)));
    }

    @Test
    void testMissingModelChildThrows() {
        GuardrailCheckFunction function = Custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(Map.of(NAME, "my-custom", PROMPT, "Detect X")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            null,
            mock(Context.class));

        assertThatThrownBy(() -> function.apply("any text", context))
            .isInstanceOf(MissingModelChildException.class)
            .hasMessageContaining("custom");
    }

    private static ChatClient mockClient(LlmClassifierUtils.Response response) {
        ChatClient client = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(client.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.messages(anyList())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenReturn(response);

        return client;
    }
}
