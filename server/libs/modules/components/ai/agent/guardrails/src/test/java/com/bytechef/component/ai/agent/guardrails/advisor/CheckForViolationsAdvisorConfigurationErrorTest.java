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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_MODE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_OPEN;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VIOLATIONS_METADATA_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.ai.agent.guardrails.util.RegexParser;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pins the "configuration errors always fail closed" contract: when a check throws {@link IllegalArgumentException} or
 * {@link java.util.regex.PatternSyntaxException} (typically from an invalid user-supplied regex), the advisor must
 * block-closed even when the operator set {@code failMode=FAIL_OPEN}.
 *
 * <p>
 * Motivation: a misconfigured Pii/SecretKeys custom regex would otherwise silently leave the guardrail inert under
 * FAIL_OPEN — the user would believe the custom regex is protecting them while the guardrail does nothing.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorConfigurationErrorTest {

    @Test
    void illegalArgumentExceptionAlwaysFailsClosedEvenWhenFailModeIsOpen() {
        GuardrailCheckFunction misconfigured = (text, context) -> {
            throw new IllegalArgumentException("Invalid Pii custom regex '[unclosed': unclosed character class");
        };

        Parameters withFailOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("piiCheck", misconfigured, withFailOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .as("configuration errors override FAIL_OPEN — misconfigured guardrails must block-closed")
            .hasSize(1);
        assertThat(violations.getFirst()).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void patternSyntaxExceptionAlwaysFailsClosedEvenWhenFailModeIsOpen() {
        GuardrailCheckFunction misconfigured = (text, context) -> {
            throw new java.util.regex.PatternSyntaxException("unclosed character class", "[unclosed", 0);
        };

        Parameters withFailOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("secretKeysCheck", misconfigured, withFailOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .as("PatternSyntaxException always fails closed — regex misconfig is not a transient outage")
            .hasSize(1);
        assertThat(violations.getFirst()).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void illegalStateExceptionAlwaysFailsClosedEvenWhenFailModeIsOpen() {
        // IllegalStateException from detector invariant violations (e.g. "unexpected internal state" thrown by
        // detector code). LlmClassifier.isUnrecoverable already treats ISE as non-transient; the advisor's
        // isConfigurationError must stay in lockstep, otherwise a FAIL_OPEN tenant silently forwards the prompt
        // on what every other layer considers a permanent failure.
        GuardrailCheckFunction broken = (text, context) -> {
            throw new IllegalStateException("detector invariant violated — internal bug");
        };

        Parameters withFailOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("broken", broken, withFailOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .as("IllegalStateException always fails closed — invariant bug is not a transient outage")
            .hasSize(1);
        assertThat(violations.getFirst()).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void nullPointerExceptionAlwaysFailsClosedEvenWhenFailModeIsOpen() {
        // NPE escaping detector code is a programming bug, not a transient outage. Treating it as transient would
        // let FAIL_OPEN mask real defects — the guardrail would silently do nothing while the operator assumed
        // protection was in place. Pinned so a future refactor that removes NullPointerException from
        // isConfigurationError's allowlist produces a visible failure.
        GuardrailCheckFunction broken = (text, context) -> {
            throw new NullPointerException("internal null dereference");
        };

        Parameters withFailOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("brokenCheck", broken, withFailOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .as("NullPointerException always fails closed — detector programming bug must not degrade to fail-open")
            .hasSize(1);
        assertThat(violations.getFirst()).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void classCastExceptionAlwaysFailsClosedEvenWhenFailModeIsOpen() {
        // Companion of NPE — the same argument applies to ClassCastException. Treating a CCE as transient would
        // let FAIL_OPEN mask a real generics/reflection bug while the operator thought the guardrail was active.
        GuardrailCheckFunction broken = (text, context) -> {
            throw new ClassCastException("cannot cast String to Integer");
        };

        Parameters withFailOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("brokenCheck", broken, withFailOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .as("ClassCastException always fails closed — detector programming bug must not degrade to fail-open")
            .hasSize(1);
        assertThat(violations.getFirst()).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }

    @Test
    void configurationCauseSurfacesAsConfigurationFailureKind() {
        // Alerting pipelines depend on the "CONFIGURATION:<class>" prefix (per CheckForViolationsAdvisor Javadoc)
        // to page operators rather than treat the failure as a transient outage. Without this test, a refactor
        // that drops or reorders the isConfigurationError check in resolveFailureKind could silently swap
        // CONFIGURATION→UNKNOWN and break alerting with every existing test still green.
        GuardrailCheckFunction misconfigured = (text, context) -> {
            throw new IllegalArgumentException("bad regex");
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("misconfigured", misconfigured, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfaced = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfaced)
            .singleElement()
            .satisfies(view -> assertThat(view.get("failureKind")).isEqualTo("CONFIGURATION:IllegalArgumentException"));
    }

    @Test
    void regexExecutionLimitExceptionAlwaysFailsClosedEvenWhenFailModeIsOpen() {
        // A pathological operator-supplied regex that hits the RegexParser DoS bound surfaces as
        // RegexExecutionLimitException. The advisor must treat it as a configuration error (the guardrail is
        // effectively inert — any input could trip the limit), not as a transient outage that FAIL_OPEN could waive.
        // Pinned here so a future refactor that forgets to list RegexExecutionLimitException in isConfigurationError
        // produces a visible failure instead of silently allowing unsafe traffic through.
        GuardrailCheckFunction pathological = (text, context) -> {
            throw new RegexParser.RegexExecutionLimitException("pattern exceeded DoS cap");
        };

        Parameters withFailOpen = ParametersFactory.create(Map.of(FAIL_MODE, FAIL_OPEN));
        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("customRegexCheck", pathological, withFailOpen, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("any text")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(violations)
            .as("RegexExecutionLimitException always fails closed — ReDoS-bounded pathological regex is not transient")
            .hasSize(1);
        assertThat(violations.getFirst()).isInstanceOf(Violation.ExecutionFailureViolation.class);
    }
}
