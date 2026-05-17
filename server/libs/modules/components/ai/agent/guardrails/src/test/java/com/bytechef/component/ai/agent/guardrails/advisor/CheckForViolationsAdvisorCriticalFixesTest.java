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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Pins the Critical fixes from the third-round PR review. Each test name maps to a specific bypass that was patched.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorCriticalFixesTest {

    @Test
    void testEmptyAssistantTextWithNonThrowingOutputCheckForwards() {
        // Empty assistant text falls through the output loop; non-throwing checks receive "" and produce no
        // violations, so the upstream response is forwarded. Pins the simplified behavior where the previous
        // fail-closed empty-output gate was removed.
        AtomicInteger invocations = new AtomicInteger();
        GuardrailCheckFunction noop = (text, context) -> {
            invocations.incrementAndGet();

            return Optional.empty();
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", noop, null, null, null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatResponse emptyTextResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(""))))
            .build();
        ChatClientResponse upstream = ChatClientResponse.builder()
            .chatResponse(emptyTextResponse)
            .build();

        when(chain.nextCall(any())).thenReturn(upstream);

        ChatClientResponse response = advisor.adviseCall(requestWithUser("hello"), chain);

        // 2 invocations: one on the input pass with "hello", one on the output pass with "".
        assertThat(invocations.get()).isEqualTo(2);
        assertThat(response.chatResponse()
            .getMetadata()
            .containsKey(VIOLATIONS_METADATA_KEY))
                .as("empty assistant text + non-throwing output check → forwarded without blocking")
                .isFalse();
    }

    @Test
    void testEmptyAssistantTextWithNoStructuredContentIsForwardedWhenNoOutputChecksEnabled() {
        // Plain empty text (no tool calls, no media) is forwarded unchanged when no output check is enabled — there
        // is nothing for the advisor to verify. This preserves prior behavior for legitimate empty-string responses
        // where the operator did not configure any output guardrail.
        Parameters outputDisabled = ParametersFactory.create(
            Map.of(VALIDATE_INPUT, true, VALIDATE_OUTPUT, false));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", (text, context) -> Optional.empty(), outputDisabled, null, null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        ChatResponse emptyTextResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(""))))
            .build();
        ChatClientResponse upstream = ChatClientResponse.builder()
            .chatResponse(emptyTextResponse)
            .build();

        when(chain.nextCall(any())).thenReturn(upstream);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getMetadata()
            .containsKey(VIOLATIONS_METADATA_KEY))
                .as("no structured content + no output checks → forwarded without blocking")
                .isFalse();
    }

    @Test
    void testStreamingSkipsLlmStageOutputChecks() {
        AtomicInteger llmInvocations = new AtomicInteger();
        AtomicInteger preflightInvocations = new AtomicInteger();

        GuardrailCheckFunction llmCheck = new GuardrailCheckFunction() {
            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }

            @Override
            public Optional<Violation> apply(
                String text, com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext context) {
                llmInvocations.incrementAndGet();

                return Optional.empty();
            }
        };

        GuardrailCheckFunction preflight = (text, context) -> {
            preflightInvocations.incrementAndGet();

            return Optional.empty();
        };

        Parameters outputEnabled = ParametersFactory.create(
            Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("llmCheck", llmCheck, outputEnabled, null, null)
            .add("preflight", preflight, outputEnabled, null, null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        // Simulate a multi-chunk stream — each chunk has plain assistant text so the LLM gate would otherwise fire.
        ChatClientResponse chunk1 = wrapInResponse("chunk-1-text");
        ChatClientResponse chunk2 = wrapInResponse("chunk-2-text");
        ChatClientResponse chunk3 = wrapInResponse("chunk-3-text");

        when(streamChain.nextStream(any())).thenReturn(Flux.just(chunk1, chunk2, chunk3));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(3);
        assertThat(llmInvocations.get())
            .as("LLM-stage output checks must be skipped per chunk in streaming mode")
            .isZero();
        assertThat(preflightInvocations.get())
            .as("PREFLIGHT (rule-based) output checks still run per chunk")
            .isEqualTo(3);
    }

    // CRITICAL #3: empty user text + output-only config must forward, not block.

    @Test
    void testEmptyUserTextForwardedWhenNoInputCheckEnabled() {
        // All checks have validateInput=false → empty user message (e.g. tool-only / agent-initiated call) must reach
        // the model. The output pass still runs on the response.
        Parameters outputOnly = ParametersFactory.create(
            Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("outputOnly", (text, context) -> Optional.empty(), outputOnly, null, null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("");

        ChatResponse upstream = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("ok"))))
            .build();
        when(chain.nextCall(any())).thenReturn(ChatClientResponse.builder()
            .chatResponse(upstream)
            .build());

        ChatClientResponse response = advisor.adviseCall(request, chain);

        verify(chain).nextCall(any());
        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("output-only configuration must allow empty user text through")
                .isEqualTo("ok");
    }

    @Test
    void testStreamUpstreamErrorFailsClosedWithBlockedResponse() {
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", (text, context) -> Optional.empty(), null, null, null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        when(streamChain.nextStream(any())).thenReturn(Flux.error(new RuntimeException("upstream blew up")));

        List<ChatClientResponse> responses = advisor.adviseStream(requestWithUser("hello"), streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("upstream stream error converts to a blocked response, never propagating raw error")
                .isEqualTo("BLOCKED");
    }

    @Test
    void testStreamingTerminatesAfterFirstBlockedChunkSoLaterChunksDoNotLeak() {
        AtomicInteger checkInvocations = new AtomicInteger();
        GuardrailCheckFunction violateOnSecondChunk = (text, context) -> {
            int invocation = checkInvocations.incrementAndGet();

            if (invocation == 2) {
                return Optional.of(Violation.ofMatches("violate-on-second", List.of(text)));
            }

            return Optional.empty();
        };

        Parameters outputEnabled = ParametersFactory.create(
            Map.of(VALIDATE_INPUT, false, VALIDATE_OUTPUT, true));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("violateOnSecond", violateOnSecondChunk, outputEnabled, null, null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientResponse chunk1 = wrapInResponse("ok-chunk-1");
        ChatClientResponse chunk2 = wrapInResponse("bad-chunk-2");
        ChatClientResponse chunk3 = wrapInResponse("third-must-not-leak");

        when(streamChain.nextStream(any())).thenReturn(Flux.just(chunk1, chunk2, chunk3));

        List<ChatClientResponse> responses = advisor.adviseStream(requestWithUser("hi"), streamChain)
            .collectList()
            .block();

        assertThat(responses)
            .as("only chunk1 (passed) and the blocked placeholder must emit — chunk3 must never reach the caller")
            .hasSize(2);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .isEqualTo("ok-chunk-1");

        ChatClientResponse blocked = responses.get(1);

        assertThat(blocked.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .isEqualTo("BLOCKED");
        assertThat(checkInvocations.get())
            .as("upstream stream must be cancelled — chunk3 never reaches the check")
            .isEqualTo(2);
    }

    // Test gap #3: blockedResponse metadata must not leak raw maskEntities (raw PII values).

    @Test
    void testBlockedResponseMetadataScrubsMaskEntities() {
        // A guardrail that puts maskEntities (raw PII values) into Violation.info — the public metadata view MUST
        // strip those before serialization. This is the single most important PII-scrubbing invariant in the SPI.
        LinkedHashMap<String, Serializable> leakyInfo = new LinkedHashMap<>();

        leakyInfo.put("entityTypes", new ArrayList<>(List.of("EMAIL_ADDRESS")));
        leakyInfo.put(
            "maskEntities",
            new LinkedHashMap<>(Map.of("EMAIL_ADDRESS", new ArrayList<>(List.of("a@b.com")))));

        GuardrailCheckFunction leaky = (text, context) -> Optional.of(
            Violation.ofMatches("pii", List.of("a@b.com"), leakyInfo));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("pii", leaky, null, null, null)
            .context(mock(Context.class))
            .build();

        ChatClientResponse response = advisor.adviseCall(requestWithUser("hello"), mock(CallAdvisorChain.class));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(violations).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) violations.get(0)
            .get("info");

        assertThat(info)
            .as("entityTypes is safe to expose; it carries no raw user content")
            .containsKey("entityTypes");

        assertThat(info)
            .as("maskEntities MUST be scrubbed — it carries raw PII values that must never leak to telemetry")
            .doesNotContainKey("maskEntities");
    }

    // Test gap #6: output-path malformed response (null/null-results) must fail-closed.

    @Test
    void testOutputPathNullChatResponseFailsClosed() {
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("noop", (text, context) -> Optional.empty(), null, null, null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse malformed = ChatClientResponse.builder()
            .chatResponse(null)
            .build();

        when(chain.nextCall(any())).thenReturn(malformed);

        ChatClientResponse response = advisor.adviseCall(requestWithUser("hello"), chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("malformed (null) chatResponse on output path must fail closed")
                .isEqualTo("BLOCKED");
    }

    private static ChatClientRequest requestWithUser(String text) {
        Prompt prompt = new Prompt(List.<Message>of(new UserMessage(text)));

        return ChatClientRequest.builder()
            .prompt(prompt)
            .build();
    }

    private static ChatClientResponse wrapInResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }
}
