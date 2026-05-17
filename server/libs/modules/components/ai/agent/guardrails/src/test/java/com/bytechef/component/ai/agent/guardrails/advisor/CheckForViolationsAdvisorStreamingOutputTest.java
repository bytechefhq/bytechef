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
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VIOLATIONS_METADATA_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Pins the {@link CheckForViolationsAdvisor#adviseStream(ChatClientRequest, StreamAdvisorChain)} contract:
 *
 * <ul>
 * <li>PREFLIGHT (rule-based) output checks run per chunk.</li>
 * <li>LLM-stage output checks are silently skipped — running an LLM classifier per chunk would exhaust rate limits and
 * produce nonsense verdicts on single-token fragments.</li>
 * <li>When a PREFLIGHT check fires mid-stream, downstream chunks are replaced by the blocked-response placeholder
 * rather than continuing.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorStreamingOutputTest {

    @Test
    void testLlmStageOutputCheckIsSkippedOnStreamingWhilePreflightRunsPerChunk() {
        AtomicInteger preflightInvocations = new AtomicInteger();
        AtomicInteger llmInvocations = new AtomicInteger();

        GuardrailCheckFunction preflight = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                preflightInvocations.incrementAndGet();

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.PREFLIGHT;
            }
        };

        GuardrailCheckFunction llmOutputCheck = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                llmInvocations.incrementAndGet();

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters outputOnlyParameters = ParametersFactory.create(Map.of(VALIDATE_INPUT, false));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("preflightOutput", preflight, outputOnlyParameters, null, null)
            .add("llmOutput", llmOutputCheck, outputOnlyParameters, null, null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("safe input");

        ChatClientResponse chunkOne = chunkResponse("chunk-1 ");
        ChatClientResponse chunkTwo = chunkResponse("chunk-2");

        when(streamChain.nextStream(request)).thenReturn(Flux.just(chunkOne, chunkTwo));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses)
            .as("upstream emitted 2 chunks and no violation fired — both must pass through")
            .hasSize(2);

        assertThat(llmInvocations.get())
            .as("LLM-stage OUTPUT check must NEVER run on streaming — would issue one LLM call per token")
            .isZero();

        assertThat(preflightInvocations.get())
            .as("PREFLIGHT runs once per chunk on output (2 chunks → 2 invocations; input is disabled by parameters)")
            .isEqualTo(2);
    }

    @Test
    void testPreflightViolationMidStreamReplacesRemainderWithBlockedResponse() {
        GuardrailCheckFunction preflight = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                if (text.contains("BAD")) {
                    return Optional.of(Violation.ofMatch("preflightOutput", "BAD"));
                }

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.PREFLIGHT;
            }
        };

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("BLOCKED")
            .add("preflightOutput", preflight, null, null, null)
            .context(mock(Context.class))
            .build();

        StreamAdvisorChain streamChain = mock(StreamAdvisorChain.class);
        ChatClientRequest request = requestWithUser("safe input");

        ChatClientResponse safeChunk = chunkResponse("safe-1 ");
        ChatClientResponse violatingChunk = chunkResponse("BAD content");
        ChatClientResponse trailingChunk = chunkResponse("trailing-3");

        when(streamChain.nextStream(request))
            .thenReturn(Flux.just(safeChunk, violatingChunk, trailingChunk));

        List<ChatClientResponse> responses = advisor.adviseStream(request, streamChain)
            .collectList()
            .block();

        assertThat(responses)
            .as("stream emits safe chunk, then a blocked-response replacing the violating chunk, then completes")
            .hasSize(2);

        ChatClientResponse first = responses.get(0);

        assertThat(first.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("first chunk was safe and must have been delivered verbatim")
                .isEqualTo("safe-1 ");

        ChatClientResponse second = responses.get(1);

        assertThat(second.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("violating chunk must be replaced by the BLOCKED placeholder, not delivered")
                .isEqualTo("BLOCKED");

        assertThat(second.chatResponse()
            .getMetadata()
            .containsKey(VIOLATIONS_METADATA_KEY))
                .as("blocked response must carry violations metadata so the caller can observe the failure")
                .isTrue();
    }

    private static ChatClientResponse chunkResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }

    private static ChatClientRequest requestWithUser(String text) {
        Prompt prompt = new Prompt(List.<Message>of(new UserMessage(text)));

        return ChatClientRequest.builder()
            .prompt(prompt)
            .build();
    }
}
