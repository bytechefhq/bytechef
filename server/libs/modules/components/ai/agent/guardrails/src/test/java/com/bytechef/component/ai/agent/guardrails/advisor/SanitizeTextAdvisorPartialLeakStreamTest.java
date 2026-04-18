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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Pins the streaming fail-closed semantics of {@link SanitizeTextAdvisor}. Specifically covers the
 * {@code alreadyShipped > 0} branch of {@code onErrorResume}: when a sanitizer throws on chunk N, chunks 1..N-1 have
 * already reached the caller and cannot be recalled. The advisor must log an ERROR-level partial-leak marker and swap
 * the remainder to the withheld-placeholder. Previously only the 0-chunk case was tested; a refactor that demoted the
 * partial-leak ERROR to WARN or collapsed the two branches would have passed every existing test while silently
 * disabling operator alerting on real streaming leaks.
 */
class SanitizeTextAdvisorPartialLeakStreamTest {

    @Test
    void partialLeakSanitizerFailureAfterShippedChunkReplacesRemainderWithWithheldPlaceholder() {
        AtomicInteger sanitizerInvocations = new AtomicInteger();

        GuardrailSanitizerFunction sanitizer = (text, context) -> {
            int invocation = sanitizerInvocations.incrementAndGet();

            if (invocation == 1) {
                return text;
            }

            throw new RuntimeException("simulated mid-stream failure");
        };

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("flaky", sanitizer, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        when(streamChain.nextStream(any(ChatClientRequest.class)))
            .thenReturn(Flux.just(assistantResponse("first chunk"), assistantResponse("second chunk")));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses)
            .as("first chunk reached the caller before the failure; remainder must be swapped to a withheld placeholder")
            .hasSize(2);

        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("chunk 1 already shipped — it cannot be recalled, so it survives unchanged")
                .isEqualTo("first chunk");

        assertThat(responses.get(1)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("chunk 2 triggered the sanitizer failure → remainder becomes the withheld placeholder, " +
                    "never the raw upstream text")
                .contains("sanitizer failed")
                .doesNotContain("second chunk");
    }

    @Test
    void partialLeakUpstreamStreamErrorAfterShippedChunkReplacesRemainderWithWithheldPlaceholder() {
        GuardrailSanitizerFunction passThrough = (text, context) -> text;

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", passThrough, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        when(streamChain.nextStream(any(ChatClientRequest.class))).thenReturn(
            Flux.concat(
                Flux.just(assistantResponse("ok chunk")),
                Flux.error(new RuntimeException("upstream stream reset"))));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        // Distinct branch from the sanitizer-throws case above: symmetric ERROR-level telemetry is expected, but the
        // cause is upstream rather than local. Both branches emit the withheld placeholder as the last chunk.
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("ok chunk");
        assertThat(responses.get(1)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .contains("sanitizer failed")
                .doesNotContain("upstream stream reset");
    }

    @Test
    void adviseCallUpstreamThrowProducesWithheldPlaceholder() {
        // Companion to the stream path: adviseCall must also fail closed when chain.nextCall throws — symmetric with
        // adviseStream's onErrorResume branch. Without this test, a refactor that removes the outer try/catch in
        // adviseCall would let the upstream exception propagate unchecked (carrying any partial content with it)
        // and every streaming test would still pass. Explicit pin.
        GuardrailSanitizerFunction passThrough = (text, context) -> text;

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", passThrough, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("hello");

        when(chain.nextCall(any(ChatClientRequest.class))).thenThrow(new RuntimeException("upstream 503"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .contains("sanitizer failed")
                .doesNotContain("upstream 503");
    }

    private static ChatClientRequest requestWithUser(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(text)))
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
