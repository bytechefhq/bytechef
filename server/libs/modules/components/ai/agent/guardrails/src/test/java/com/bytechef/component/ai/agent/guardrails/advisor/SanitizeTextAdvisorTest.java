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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

class SanitizeTextAdvisorTest {

    @Test
    void testGetOrderIsLowestPrecedence() {
        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .build();

        assertThat(advisor.getOrder()).isEqualTo(Advisor.LOWEST_PRECEDENCE);
    }

    @Test
    void testTwoSanitizersChained() {
        GuardrailSanitizerFunction sanitizerA = (text, context) -> text.replace("foo", "<X>");
        GuardrailSanitizerFunction sanitizerB = (text, context) -> text.replace("<X>", "[redacted]");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("a", sanitizerA, null, null)
            .add("b", sanitizerB, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");
        ChatClientResponse modelResponse = assistantResponse("has foo here");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("has [redacted] here");
    }

    @Test
    void testEmptySanitizerListPassesThrough() {
        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");
        ChatClientResponse modelResponse = assistantResponse("original text");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response).isSameAs(modelResponse);
    }

    @Test
    void testSanitizerThrowsWithholdsResponse() {
        GuardrailSanitizerFunction throwing = (text, context) -> {
            throw new RuntimeException("simulated failure");
        };

        GuardrailSanitizerFunction sanitizerB = (text, context) -> text.replace("foo", "[redacted]");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("throwing", throwing, null, null)
            .add("b", sanitizerB, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");
        ChatClientResponse modelResponse = assistantResponse("has foo here");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        String text = response.chatResponse()
            .getResult()
            .getOutput()
            .getText();

        // Sanitizer failures aggregate into SanitizerExecutionFailureException, which the advisor converts to the
        // withheld placeholder to avoid leaking unredacted text.
        assertThat(text).isEqualTo("[sanitizer failed — response withheld]");
    }

    @Test
    void testStreamSanitizersApplied() {
        GuardrailSanitizerFunction sanitizerA = (text, context) -> text.replace("foo", "<X>");
        GuardrailSanitizerFunction sanitizerB = (text, context) -> text.replace("<X>", "[redacted]");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("a", sanitizerA, null, null)
            .add("b", sanitizerB, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");
        ChatClientResponse modelResponse = assistantResponse("has foo here");

        when(streamChain.nextStream(request)).thenReturn(Flux.just(modelResponse));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("has [redacted] here");
    }

    @Test
    void testStreamUpstreamErrorWithholdsResponse() {
        GuardrailSanitizerFunction noop = (text, context) -> text;

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("noop", noop, null, null)
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");

        when(streamChain.nextStream(request))
            .thenReturn(Flux.error(new RuntimeException("upstream exploded")));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0)
            .chatResponse()
            .getResult()
            .getOutput()
            .getText()).contains("sanitizer failed");
    }

    @Test
    void testPreservesAssistantMessageToolCalls() {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("id1", "function", "mytool", "{}");
        AssistantMessage original = AssistantMessage.builder()
            .content("contains foo")
            .toolCalls(List.of(toolCall))
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(original)))
            .build();
        ChatClientResponse modelResponse = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();

        GuardrailSanitizerFunction sanitizer = (text, context) -> text.replace("foo", "<X>");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("a", sanitizer, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        AssistantMessage result = response.chatResponse()
            .getResult()
            .getOutput();

        assertThat(result.getText()).isEqualTo("contains <X>");
        assertThat(result.getToolCalls()).containsExactly(toolCall);
    }

    @Test
    void testPreservesAssistantMessageProperties() {
        AssistantMessage original = AssistantMessage.builder()
            .content("contains foo")
            .properties(Map.of("custom-key", "custom-value"))
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(original)))
            .build();
        ChatClientResponse modelResponse = ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();

        GuardrailSanitizerFunction sanitizer = (text, context) -> text.replace("foo", "<X>");

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("a", sanitizer, null, null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = requestWithUser("user message");

        when(chain.nextCall(request)).thenReturn(modelResponse);

        ChatClientResponse response = advisor.adviseCall(request, chain);

        AssistantMessage result = response.chatResponse()
            .getResult()
            .getOutput();

        assertThat(result.getText()).isEqualTo("contains <X>");
        assertThat(result.getMetadata()).containsEntry("custom-key", "custom-value");
    }

    private static ChatClientRequest requestWithUser(String text) {
        Prompt prompt = new Prompt(List.<Message>of(new UserMessage(text)));

        return ChatClientRequest.builder()
            .prompt(prompt)
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

    @Test
    void testAdvisorIsThreadSafeAcrossConcurrentInvocations() {
        // Pin the per-request isolation invariant: MaskEntityMap and the intermediate-text accumulator live inside
        // sanitise() and must not leak between calls when the same advisor instance is reused by Spring AI's pool.
        // If the sanitiser saw state from another thread, the rewritten output would contain a substring the caller
        // never produced.
        AtomicInteger callCount = new AtomicInteger();

        GuardrailSanitizerFunction replacer = (text, context) -> {
            callCount.incrementAndGet();

            return text.replace("bad", "<REDACTED>");
        };

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("replacer", replacer, null, null)
            .build();

        int concurrency = 100;

        IntStream.range(0, concurrency)
            .parallel()
            .forEach(index -> {
                String input = "thread-" + index + ": bad content";
                String rewritten = advisor.sanitiseForTesting(input);

                assertThat(rewritten)
                    .as("thread %d must see only its own rewrite; cross-thread leakage would break this", index)
                    .isEqualTo("thread-" + index + ": <REDACTED> content");
            });

        assertThat(callCount.get()).isEqualTo(concurrency);
    }
}
