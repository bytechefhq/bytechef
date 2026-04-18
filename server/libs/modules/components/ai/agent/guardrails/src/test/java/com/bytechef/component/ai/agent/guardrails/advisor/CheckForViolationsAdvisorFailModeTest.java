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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_CLOSED;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_MODE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_OPEN;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
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
class CheckForViolationsAdvisorFailModeTest {

    private static final GuardrailCheckFunction BROKEN_LLM = new GuardrailCheckFunction() {

        @Override
        public Optional<Violation> apply(
            String text, com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext context) {
            throw new RuntimeException("LLM down");
        }

        @Override
        public GuardrailStage stage() {
            return GuardrailStage.LLM;
        }
    };

    @Test
    void failOpenCheckIsNotRecordedAsBlockingViolation() {
        Parameters failOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", BROKEN_LLM, failOpen, empty, empty, empty, Map.of(), null)
            .build();

        List<Violation> violations = advisor.runChecksForTesting(requestWithUser("hello"));

        assertThat(violations).isEmpty();
    }

    @Test
    void failClosedCheckIsRecordedAsExecutionFailureAndBlocks() {
        Parameters failClosed = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_CLOSED));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", BROKEN_LLM, failClosed, empty, empty, empty, Map.of(), null)
            .build();

        List<Violation> violations = advisor.runChecksForTesting(requestWithUser("hello"));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)
            .guardrail()).isEqualTo("jailbreak");
        assertThat(violations.get(0)).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void defaultFailModeIsClosedWhenNoParameterProvided() {
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", BROKEN_LLM, empty, empty, empty, empty, Map.of(), null)
            .build();

        List<Violation> violations = advisor.runChecksForTesting(requestWithUser("hello"));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void failOpenDoesNotShortCircuitOtherChecks() {
        Parameters failOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        GuardrailCheckFunction pii = (text, context) -> Optional.of(Violation.ofMatch("pii", "a@b.com"));

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", BROKEN_LLM, failOpen, empty, empty, empty, Map.of(), null)
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .build();

        List<Violation> violations = advisor.runChecksForTesting(requestWithUser("hack at a@b.com"));

        assertThat(violations).hasSize(1);
        assertThat(violations.get(0)
            .guardrail()).isEqualTo("pii");
    }

    private static ChatClientRequest requestWithUser(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(text)))
            .build();
    }
}
