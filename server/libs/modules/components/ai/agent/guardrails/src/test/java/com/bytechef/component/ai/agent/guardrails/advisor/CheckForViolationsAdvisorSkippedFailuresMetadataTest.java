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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VIOLATIONS_METADATA_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.agent.guardrails.GuardrailUnavailableException;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorSkippedFailuresMetadataTest {

    private static final GuardrailCheckFunction BROKEN_LLM = new GuardrailCheckFunction() {

        @Override
        public Optional<Violation> apply(String text, GuardrailContext context) {
            throw new GuardrailUnavailableException(
                "jailbreak", "LLM outage", new RuntimeException("upstream 503"));
        }

        @Override
        public GuardrailStage stage() {
            return GuardrailStage.LLM;
        }
    };

    @Test
    void guardrailUnavailableExceptionBlocksRequest() {
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", BROKEN_LLM, empty, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello")))
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenAnswer(invocation -> {
            throw new AssertionError("Chain must not be called when a violation fires");
        });

        ChatClientResponse response = advisor.adviseCall(request, chain);

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText())
                .as("GuardrailUnavailableException must block the request (fail-closed)")
                .isEqualTo("blocked");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(violations)
            .as("execution failure must appear in violations metadata")
            .hasSize(1)
            .singleElement()
            .satisfies(view -> {
                assertThat(view.get("guardrail")).isEqualTo("jailbreak");
                assertThat(view.get("executionFailed")).isEqualTo(true);
                assertThat(view.get("failureKind")).isEqualTo("UPSTREAM_UNAVAILABLE");
                assertThat(view)
                    .doesNotContainKey("matchedSubstrings")
                    .doesNotContainKey("exception");
            });

        Object skipped = response.chatResponse()
            .getMetadata()
            .get("guardrail.skippedFailures");

        assertThat(skipped)
            .as("all exceptions are fail-closed; no skipped-failures metadata is emitted")
            .isNull();
    }

    @Test
    void guardrailUnavailableExceptionAndRealViolationBothAppearInViolations() {
        Parameters empty = ParametersFactory.create(Map.of());

        GuardrailCheckFunction pii = (text, context) -> Optional.of(Violation.ofMatch("pii", "a@b.com"));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", BROKEN_LLM, empty, empty, empty, empty, Map.of(), null)
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hack at a@b.com")))
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenAnswer(invocation -> {
            throw new AssertionError("Chain must not be called when a blocking violation fires");
        });

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(violations)
            .as("both the execution failure and the real violation must appear in violations (all fail-closed)")
            .hasSize(2);

        // pii runs in PREFLIGHT stage (first), jailbreak (LLM) runs second — order matches declaration within stage
        assertThat(violations.get(0)
            .get("guardrail")).isEqualTo("pii");
        assertThat(violations.get(0)
            .get("executionFailed")).isEqualTo(false);

        assertThat(violations.get(1)
            .get("guardrail")).isEqualTo("jailbreak");
        assertThat(violations.get(1)
            .get("executionFailed")).isEqualTo(true);
        assertThat(violations.get(1)
            .get("failureKind")).isEqualTo("UPSTREAM_UNAVAILABLE");

        Object skipped = response.chatResponse()
            .getMetadata()
            .get("guardrail.skippedFailures");

        assertThat(skipped)
            .as("no skipped-failures metadata is emitted; all exceptions are fail-closed")
            .isNull();
    }

    @Test
    void cleanCheckProducesNoSkippedFailuresMetadata() {
        GuardrailCheckFunction clean = (text, context) -> Optional.empty();
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("keywords", clean, empty, empty, empty, empty, Map.of(), null)
            .context(mock(Context.class))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hi")))
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(upstreamResponse(request, "clean"));

        ChatClientResponse response = advisor.adviseCall(request, chain);

        Object skipped = response.chatResponse()
            .getMetadata()
            .get("guardrail.skippedFailures");

        assertThat(skipped)
            .as("no violations ⇒ no metadata key")
            .isNull();
    }

    private static ChatClientResponse upstreamResponse(ChatClientRequest request, String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(request.context())
            .build();
    }
}
