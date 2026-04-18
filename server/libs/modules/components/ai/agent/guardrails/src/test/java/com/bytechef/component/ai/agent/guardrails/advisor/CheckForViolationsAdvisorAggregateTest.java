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

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorAggregateTest {

    @Test
    void aggregatesViolationsFromEveryCheckEvenAfterFirstMatch() {
        GuardrailCheckFunction keywords = (text, context) -> Optional.of(Violation.ofMatch("keywords", "hack"));
        GuardrailCheckFunction pii = (text, context) -> Optional.of(Violation.ofMatch("pii", "a@b.com"));
        GuardrailCheckFunction urls = (text, context) -> Optional.empty();

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("keywords", keywords, empty, empty, empty, empty, Map.of(), null)
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .add("urls", urls, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("Please hack me at a@b.com")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(Violation::guardrail)
            .containsExactly("keywords", "pii");
    }

    @Test
    void exceptionsBecomeExecutionFailureViolationsAndDoNotAbortOtherChecks() {
        GuardrailCheckFunction brokenLlm = (text, context) -> {
            throw new RuntimeException("LLM timeout");
        };
        GuardrailCheckFunction pii = (text, context) -> Optional.of(Violation.ofMatch("pii", "a@b.com"));

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", brokenLlm, empty, empty, empty, empty, Map.of(), null)
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hack at a@b.com")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations).hasSize(2);
        // First: execution failure (no longer short-circuits)
        assertThat(violations.get(0)
            .guardrail()).isEqualTo("jailbreak");
        assertThat(violations.get(0)).isInstanceOf(Violation.ExecutionFailureViolation.class);
        assertThat(((Violation.ExecutionFailureViolation) violations.get(0)).exception())
            .isInstanceOf(RuntimeException.class);
        // Second: the genuine PII violation still runs
        assertThat(violations.get(1)
            .guardrail()).isEqualTo("pii");
        assertThat(violations.get(1)).isNotInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void emptyChecksPassThrough() {
        GuardrailCheckFunction noop = (text, context) -> Optional.empty();

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("keywords", noop, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hello world")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations).isEmpty();
    }
}
