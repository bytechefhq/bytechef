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

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import java.util.List;
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
 * @author Ivica Cardic
 */
class SanitizeTextAdvisorPartialLeakStreamTest {

    @Test
    void partialLeakSanitizerFailureAfterShippedChunkReplacesRemainderWithWithheldPlaceholder() {
        // Match on the content rather than invocation count: the sanitizer is also invoked once on the user input
        // ("hello") before the stream begins, so counting invocations would trigger the failure on the first chunk
        // instead of the second.
        GuardrailSanitizerFunction sanitizer = (text, context) -> {
            if ("second chunk".equals(text)) {
                throw new RuntimeException("simulated mid-stream failure");
            }

            return text;
        };

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("flaky", sanitizer, null, null)
            .context(mock(Context.class))
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
            .context(mock(Context.class))
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
            .context(mock(Context.class))
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
