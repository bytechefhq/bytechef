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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.util.LlmClassifier;
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
        ChatClient client = mockClient(new LlmClassifier.Response(0.9, true));

        Optional<Violation> violation = Custom.classifyWith(
            client,
            "my-custom-guardrail",
            "Detect references to competitor products.",
            0.7,
            DEFAULT_SYSTEM_MESSAGE,
            "Have you tried using CompetitorApp instead?");

        assertThat(violation).isPresent();
        assertThat(violation.get()
            .guardrail()).isEqualTo("my-custom-guardrail");
        assertThat(((Violation.ClassifiedViolation) violation.get()).confidenceScore()).isEqualTo(0.9);
    }

    @Test
    void testBelowThresholdNoViolation() {
        ChatClient client = mockClient(new LlmClassifier.Response(0.5, true));

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
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testCustomGuardrailWithResponseSchemaSurfacesExtrasInViolationInfo() throws Exception {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);

        when(chatClient.prompt()
            .system(anyString())
            .user(anyString())
            .call()
            .entity(any(ParameterizedTypeReference.class)))
                .thenReturn(Map.of(
                    "flagged", true,
                    "confidenceScore", 0.85,
                    "reason", "spam"));

        GuardrailCheckFunction check = (GuardrailCheckFunction) new Custom()
            .of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of(
            NAME, "spamCheck",
            PROMPT, "Is this spam?",
            RESPONSE_SCHEMA, "{}",
            THRESHOLD, 0.7));

        GuardrailContext context = new GuardrailContext(
            inputParameters, ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Map.of(), chatClient);

        Optional<Violation> result = check.apply("hello", context);

        assertThat(result).isPresent();
        assertThat(((Violation.ClassifiedViolation) result.get()).confidenceScore()).isEqualTo(0.85);
        assertThat(result.get()
            .info()).containsEntry("reason", "spam");
    }

    @Test
    void testMultipleGuardrailEntriesAllRunInOneInstance() throws Exception {
        // Each entry classifies independently; the first hit surfaces as the violation.
        ChatClient chatClient = mockClient(new LlmClassifier.Response(0.9, true));
        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
            .getElement();

        // Reuses the test-support GuardrailContext constructor that takes a ChatClient.
        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "no-competitors", PROMPT, "Detect competitor mentions"),
                    Map.of(NAME, "no-commitments", PROMPT, "Detect commitment language")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient);

        Optional<Violation> violation = function.apply("some input", context);

        assertThat(violation).isPresent();
        // First entry wins since both fire (mocked response is flagged).
        assertThat(violation.get()
            .guardrail()).isEqualTo("no-competitors");
    }

    @Test
    void testMultipleGuardrailsEmptyEntryIsRejected() {
        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
            .getElement();

        ChatClient chatClient = mockClient(new LlmClassifier.Response(0.1, false));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(Map.of(NAME, "", PROMPT, "p")))),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient);

        assertThatThrownBy(() -> function.apply("x", context))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-empty");
    }

    @Test
    void testEmptyGuardrailsListFallsBackToTopLevelNameAndPrompt() throws Exception {
        // When `guardrails: []` the top-level NAME/PROMPT pair should be used.
        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
            .getElement();

        ChatClient chatClient = mockClient(new LlmClassifier.Response(0.9, true));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(),
                NAME, "top-level-guardrail",
                PROMPT, "Detect top-level")),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient);

        Optional<Violation> violation = function.apply("sensitive", context);

        assertThat(violation).isPresent();
        assertThat(violation.get()
            .guardrail()).isEqualTo("top-level-guardrail");
    }

    @Test
    void testSingleEntryGuardrailsListTakesPrecedenceOverTopLevelNameAndPrompt() throws Exception {
        // A non-empty `guardrails` array (even with one entry) must override the top-level NAME/PROMPT.
        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
            .getElement();

        ChatClient chatClient = mockClient(new LlmClassifier.Response(0.9, true));

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(
                "guardrails", java.util.List.of(
                    Map.of(NAME, "array-entry", PROMPT, "Prompt from array")),
                NAME, "IGNORED-top-level",
                PROMPT, "IGNORED-top-level-prompt")),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of(),
            chatClient);

        Optional<Violation> violation = function.apply("sensitive", context);

        assertThat(violation).isPresent();
        assertThat(violation.get()
            .guardrail())
                .as("non-empty guardrails list must win over top-level NAME/PROMPT")
                .isEqualTo("array-entry");
    }

    @Test
    void testMultipleFlaggedEntriesSurfaceAllNamesInPublicInfo() throws Exception {
        // When N entries fire, headline wins but info.flaggedEntries should list all N names.
        ChatClient chatClient = mockClient(new LlmClassifier.Response(0.9, true));

        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
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
            chatClient);

        Optional<Violation> violation = function.apply("input", context);

        assertThat(violation).isPresent();
        assertThat(violation.get()
            .guardrail()).isEqualTo("first");
        assertThat(violation.get()
            .info())
                .containsEntry("flaggedEntries", java.util.List.of("first", "second", "third"));
    }

    @Test
    void testPartialEntryFailureThrowsGuardrailUnavailableEvenWhenOtherEntriesFired() {
        // A partial pass is effectively not a full pass — when any entry throws, the failure must reach the advisor
        // via GuardrailUnavailableException so its fail-mode branch (FAIL_OPEN/FAIL_CLOSED) can decide. Previously the
        // failure vanished into an info-map field on the headline violation.
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class, RETURNS_DEEP_STUBS);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class)))
            .thenReturn(new LlmClassifier.Response(0.9, true)) // first entry classifies as violated
            .thenThrow(new RuntimeException("LLM down")); // second entry throws

        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
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
            chatClient);

        assertThatThrownBy(() -> function.apply("input", context))
            .isInstanceOf(GuardrailUnavailableException.class)
            .hasMessageContaining("broken-entry");
    }

    @Test
    void testMissingModelChildThrows() {
        Custom custom = new Custom();

        @SuppressWarnings("unchecked")
        GuardrailCheckFunction function = (GuardrailCheckFunction) custom.of()
            .getElement();

        GuardrailContext context = new GuardrailContext(
            ParametersFactory.create(Map.of(NAME, "my-custom", PROMPT, "Detect X")),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()),
            Map.of());

        assertThatThrownBy(() -> function.apply("any text", context))
            .isInstanceOf(MissingModelChildException.class)
            .hasMessageContaining("custom");
    }

    private static ChatClient mockClient(LlmClassifier.Response response) {
        ChatClient client = mock(ChatClient.class);
        ChatClientRequestSpec spec = mock(ChatClientRequestSpec.class);
        CallResponseSpec callSpec = mock(CallResponseSpec.class);

        when(client.prompt()).thenReturn(spec);
        when(spec.system(anyString())).thenReturn(spec);
        when(spec.user(anyString())).thenReturn(spec);
        when(spec.call()).thenReturn(callSpec);
        when(callSpec.entity(any(Class.class))).thenReturn(response);

        return client;
    }
}
