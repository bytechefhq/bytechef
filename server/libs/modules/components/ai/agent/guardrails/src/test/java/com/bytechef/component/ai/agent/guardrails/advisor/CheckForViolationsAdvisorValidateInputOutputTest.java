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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_OUTPUT;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VIOLATIONS_METADATA_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorValidateInputOutputTest {

    @Test
    void validateInputFalseSkipsInputCheckEvenWhenInputContainsViolation() {
        // A check with VALIDATE_INPUT=false must never run against user input. Even when the input
        // would match a violation, the chain is called and the model response returned unblocked.
        // The check fires on the marker word so we can distinguish input vs output runs — only the
        // input contains it, the model output does not, so the output-side pass (validateOutput defaults
        // to true) sees no violation either.
        GuardrailCheckFunction violatesOnBad = (text, context) -> text.contains("bad")
            ? Optional.of(Violation.ofMatch("kw", "bad"))
            : Optional.empty();

        Parameters noInputCheck = ParametersFactory.create(Map.of(VALIDATE_INPUT, false));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("kw", violatesOnBad, noInputCheck, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("this is bad input that would normally be blocked");
        ChatClientResponse modelResponse = assistantResponse("ok");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isSameAs(modelResponse);
        verify(chain).nextCall(request);
    }

    @Test
    void validateInputFalseSelectivelySkipsOneCheckWhileSiblingStillRuns() {
        // Per-check granularity: only the check with VALIDATE_INPUT=false is skipped; the sibling
        // with default (true) still fires and its violation blocks the request.
        GuardrailCheckFunction mustNotRun = (text, context) -> {
            throw new AssertionError("this check must not run on input");
        };

        GuardrailCheckFunction mustRun = (text, context) -> Optional.of(Violation.ofMatch("pii", "secret"));

        Parameters noInputCheck = ParametersFactory.create(Map.of(VALIDATE_INPUT, false));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("skipped", mustNotRun, noInputCheck, empty, empty, empty, Map.of(), null)
            .add("pii", mustRun, empty, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        ChatClientResponse response = advisor.adviseCall(requestWithUser("secret data"), chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void validateInputFalseOnAllChecksCallsChainWithoutRunningAnyCheck() {
        // When every check has VALIDATE_INPUT=false the advisor produces no violations on input and
        // delegates to the chain immediately. The check throws only when invoked on the input marker;
        // the output text deliberately omits the marker so the validateOutput-true default does not
        // turn this test into a guardrail-disabled assertion (which would require both flags off,
        // which the Builder rejects).
        String inputMarker = "INPUT_ONLY_MARKER";
        GuardrailCheckFunction mustNotRunOnInput = (text, context) -> {
            if (text.contains(inputMarker)) {
                throw new AssertionError("check must not run when VALIDATE_INPUT=false");
            }

            return Optional.empty();
        };

        Parameters noInputCheck = ParametersFactory.create(Map.of(VALIDATE_INPUT, false));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("first", mustNotRunOnInput, noInputCheck, empty, empty, empty, Map.of(), null)
            .add("second", mustNotRunOnInput, noInputCheck, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser(inputMarker);
        ChatClientResponse modelResponse = assistantResponse("fine");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isSameAs(modelResponse);
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────
    // VALIDATE_OUTPUT = true
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void validateOutputTrueDetectsViolationInModelResponse() {
        // With VALIDATE_OUTPUT=true the check runs against the model's response text. A violating
        // response is blocked even though the input was clean.
        GuardrailCheckFunction outputCheck = (text, context) -> text.contains("toxic")
            ? Optional.of(Violation.ofMatch("nsfw", "toxic"))
            : Optional.empty();

        Parameters outputOnly = ParametersFactory.create(Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("nsfw", outputCheck, outputOnly, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("tell me a story");

        when(chain.nextCall(request)).thenReturn(assistantResponse("once upon a time something toxic happened"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
    }

    @Test
    void validateOutputTrueCleanResponsePassesThrough() {
        // When VALIDATE_OUTPUT=true but the model response is clean, the original response is returned.
        GuardrailCheckFunction noop = (text, context) -> Optional.empty();

        Parameters withOutputCheck = ParametersFactory.create(Map.of(VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", noop, withOutputCheck, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");
        ChatClientResponse modelResponse = assistantResponse("clean answer");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isSameAs(modelResponse);
    }

    @Test
    void validateOutputTrueViolationMetadataAppearsOnBlockedResponse() {
        // The blocked response must carry the output violation in VIOLATIONS_METADATA_KEY so
        // downstream consumers can distinguish "input blocked" from "output blocked".
        GuardrailCheckFunction outputCheck = (text, context) -> text.contains("leaked")
            ? Optional.of(Violation.ofMatch("pii", "leaked"))
            : Optional.empty();

        Parameters outputOnly = ParametersFactory.create(Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("pii", outputCheck, outputOnly, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("what is my password?");

        when(chain.nextCall(request)).thenReturn(assistantResponse("your leaked password is abc123"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(violations)
            .hasSize(1)
            .singleElement()
            .satisfies(view -> {
                assertThat(view.get("guardrail")).isEqualTo("pii");
                assertThat(view.get("executionFailed")).isEqualTo(false);
            });
    }

    @Test
    void validateOutputTrueExceptionIsFailClosed() {
        // An exception thrown by an output check must block the response — fail-closed semantics
        // apply equally to output checks as to input checks. A broken output guardrail must not
        // silently let model responses through.
        GuardrailCheckFunction brokenOutputCheck = (text, context) -> {
            throw new RuntimeException("output classifier down");
        };

        Parameters withOutputCheck = ParametersFactory.create(Map.of(VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("classifier", brokenOutputCheck, withOutputCheck, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        when(chain.nextCall(request)).thenReturn(assistantResponse("response text"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
    }

    @Test
    void validateOutputTrueLlmStageCheckRunsOnModelResponse() {
        // LLM-stage checks must also honour VALIDATE_OUTPUT=true — the check runs against the model
        // response text, not the user input.
        GuardrailCheckFunction llmOutputCheck = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return text.contains("jailbroken-output")
                    ? Optional.of(Violation.ofMatch("jailbreak-out", "jailbroken-output"))
                    : Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters outputOnly = ParametersFactory.create(Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("jailbreak-out", llmOutputCheck, outputOnly, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        when(chain.nextCall(request)).thenReturn(assistantResponse("here is the jailbroken-output you asked for"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
    }

    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────
    // Both directions
    // ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────

    @Test
    void validateInputTrueAndOutputTrueBlocksOnInputBeforeCallingChain() {
        // When a check flags both directions and the input is bad, the request is blocked before
        // the chain is called — the output phase never runs.
        GuardrailCheckFunction bidirectional = (text, context) -> text.contains("bad")
            ? Optional.of(Violation.ofMatch("kw", "bad"))
            : Optional.empty();

        Parameters both = ParametersFactory.create(Map.of(VALIDATE_INPUT, true, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("kw", bidirectional, both, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        ChatClientResponse response = advisor.adviseCall(requestWithUser("bad input"), chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain, never()).nextCall(any());
    }

    @Test
    void validateInputTrueAndOutputTrueBlocksOnOutputWhenInputIsClean() {
        // When the input is clean but the model response contains a violation and VALIDATE_OUTPUT=true,
        // the response is blocked after the chain call.
        GuardrailCheckFunction bidirectional = (text, context) -> text.contains("leaked-data")
            ? Optional.of(Violation.ofMatch("pii", "leaked-data"))
            : Optional.empty();

        Parameters both = ParametersFactory.create(Map.of(VALIDATE_INPUT, true, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("pii", bidirectional, both, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("show me the report");

        when(chain.nextCall(request)).thenReturn(assistantResponse("the report contains leaked-data from the db"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
        verify(chain).nextCall(request);
    }

    @Test
    void validateInputTrueAndOutputTrueBothCleanPassesThrough() {
        // When both input and output are clean and both flags are true, the original response is returned.
        GuardrailCheckFunction bidirectional = (text, context) -> Optional.empty();

        Parameters both = ParametersFactory.create(Map.of(VALIDATE_INPUT, true, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", bidirectional, both, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");
        ChatClientResponse modelResponse = assistantResponse("clean answer");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isSameAs(modelResponse);
    }

    @Test
    void validateOutputTrueStreamBlocksChunkWithViolation() {
        // On the streaming path, a response chunk that triggers the output check is replaced with a
        // blocked response — the check runs per-chunk (same caveat as SanitizeTextAdvisor).
        GuardrailCheckFunction outputCheck = (text, context) -> text.contains("toxic")
            ? Optional.of(Violation.ofMatch("nsfw", "toxic"))
            : Optional.empty();

        Parameters outputOnly = ParametersFactory.create(Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("nsfw", outputCheck, outputOnly, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("tell me a story");

        when(streamChain.nextStream(request)).thenReturn(Flux.just(assistantResponse("this is toxic content")));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
    }

    @Test
    void validateOutputTrueStreamCleanChunksPassThrough() {
        // Clean stream chunks are emitted unchanged when the output check finds no violation.
        GuardrailCheckFunction noop = (text, context) -> Optional.empty();

        Parameters withOutputCheck = ParametersFactory.create(Map.of(VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", noop, withOutputCheck, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");
        ChatClientResponse chunk1 = assistantResponse("hello ");
        ChatClientResponse chunk2 = assistantResponse("world");

        when(streamChain.nextStream(request)).thenReturn(Flux.just(chunk1, chunk2));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0)).isSameAs(chunk1);
        assertThat(responses.get(1)).isSameAs(chunk2);
    }

    @Test
    void validateOutputTrueStreamMixedChunksBlocksOnViolatingChunk() {
        // The first (clean) chunk passes through; the second (violating) chunk is replaced with a
        // blocked response. This pins the per-chunk processing on the stream path.
        GuardrailCheckFunction outputCheck = (text, context) -> text.contains("toxic")
            ? Optional.of(Violation.ofMatch("nsfw", "toxic"))
            : Optional.empty();

        Parameters outputOnly = ParametersFactory.create(Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("nsfw", outputCheck, outputOnly, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("tell me a story");
        ChatClientResponse cleanChunk = assistantResponse("once upon a time");
        ChatClientResponse toxicChunk = assistantResponse("then something toxic");

        when(streamChain.nextStream(request)).thenReturn(Flux.just(cleanChunk, toxicChunk));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0)).isSameAs(cleanChunk);
        assertThat(responses.get(1)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("BLOCKED");
    }

    private static ChatClientRequest requestWithUser(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.<Message>of(new UserMessage(text))))
            .build();
    }

    private static ChatClientResponse assistantResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }
}
