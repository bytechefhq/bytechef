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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ai.agent.guardrails.MissingModelChildException;
import com.bytechef.component.ai.agent.guardrails.SanitizerExecutionFailureException;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins the per-sanitizer failMode contract introduced alongside the advisor: FAIL_OPEN skips the broken sanitizer and
 * preserves the last-good intermediate text, while FAIL_CLOSED (the default) aborts the pass via
 * {@link SanitizerExecutionFailureException}.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorFailModeTest {

    @Test
    void failOpenSanitizerDoesNotAbortPass() {
        GuardrailSanitizerFunction goodMask = (text, context) -> text.replace("a@b.com", "<EMAIL>");
        GuardrailSanitizerFunction broken = (text, context) -> {
            throw new RuntimeException("LLM unavailable");
        };

        Parameters empty = ParametersFactory.create(Map.of());
        Parameters failOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("goodMask", goodMask, empty, empty, empty, Map.of(), null)
            .add("brokenButOpen", broken, failOpen, empty, empty, Map.of(), null)
            .build();

        String result = advisor.sanitiseForTesting("reach me at a@b.com");

        assertThat(result).isEqualTo("reach me at <EMAIL>");
    }

    @Test
    void failClosedSanitizerStillAbortsPass() {
        GuardrailSanitizerFunction goodMask = (text, context) -> text.replace("a@b.com", "<EMAIL>");
        GuardrailSanitizerFunction broken = (text, context) -> {
            throw new RuntimeException("LLM unavailable");
        };

        Parameters empty = ParametersFactory.create(Map.of());
        Parameters failClosed = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_CLOSED));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("goodMask", goodMask, empty, empty, empty, Map.of(), null)
            .add("brokenClosed", broken, failClosed, empty, empty, Map.of(), null)
            .build();

        assertThatThrownBy(() -> advisor.sanitiseForTesting("reach me at a@b.com"))
            .isInstanceOf(SanitizerExecutionFailureException.class)
            .hasMessageContaining("brokenClosed");
    }

    @Test
    void missingFailModeDefaultsToClosed() {
        GuardrailSanitizerFunction broken = (text, context) -> {
            throw new RuntimeException("boom");
        };

        Parameters empty = ParametersFactory.create(Map.of());

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("broken", broken, empty, empty, empty, Map.of(), null)
            .build();

        assertThatThrownBy(() -> advisor.sanitiseForTesting("x"))
            .isInstanceOf(SanitizerExecutionFailureException.class);
    }

    @Test
    void missingModelChildExceptionInSanitizerAggregatesAsClosedFailure() {
        // On the sanitize side, an LLM sanitizer that runs without a MODEL child throws MissingModelChildException.
        // The advisor must aggregate it as a FAIL_CLOSED sanitizer failure so the call path can withhold the response
        // via its placeholder (see SanitizeTextAdvisor.adviseCall).
        GuardrailSanitizerFunction llmSanitizer = (text, context) -> {
            throw new MissingModelChildException("llmPii");
        };

        Parameters empty = ParametersFactory.create(Map.of());
        Parameters failClosed = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_CLOSED));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("llmPii", llmSanitizer, failClosed, empty, empty, Map.of(), null)
            .build();

        assertThatThrownBy(() -> advisor.sanitiseForTesting("reach me at alice@corp.com"))
            .isInstanceOfSatisfying(SanitizerExecutionFailureException.class, exception -> {
                assertThat(exception.failures()).containsKey("llmPii");
                assertThat(exception.failures()
                    .get("llmPii")).isInstanceOf(MissingModelChildException.class);
            });
    }

    @Test
    void mixOfFailOpenAndFailClosedAggregatesOnlyClosedFailures() {
        GuardrailSanitizerFunction brokenOpen = (text, context) -> {
            throw new RuntimeException("open-broken");
        };
        GuardrailSanitizerFunction brokenClosed = (text, context) -> {
            throw new RuntimeException("closed-broken");
        };

        Parameters failOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters failClosed = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_CLOSED));

        SanitizeTextAdvisor advisor = SanitizeTextAdvisor.builder()
            .add("open", brokenOpen, failOpen, failOpen, failOpen, Map.of(), null)
            .add("closed", brokenClosed, failClosed, failClosed, failClosed, Map.of(), null)
            .build();

        assertThatThrownBy(() -> advisor.sanitiseForTesting("x"))
            .isInstanceOfSatisfying(SanitizerExecutionFailureException.class, exception -> {
                assertThat(exception.failures()).containsOnlyKeys("closed");
                assertThat(exception.failures()).doesNotContainKey("open");
            });
    }
}
