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
import static org.mockito.Mockito.mock;

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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pins the invariant that raw secret / PII values never reach the advisor's public-view metadata via the
 * {@code exception} chain on an {@link Violation#ofExecutionFailure}.
 * {@code CheckForViolationsAdvisorPublicMetadataTest} already covers leakage through {@code matchedSubstrings} and
 * {@code info}; this test covers the {@code Violation.exception()} path specifically.
 *
 * <p>
 * An implementation bug where a detector includes raw input in an exception message (e.g. "failed to parse response
 * {...'secret':'sk-REAL'}") would otherwise silently forward the secret to every downstream telemetry pipeline that
 * reads the blocked-response metadata. {@code toPublicView} must project only the stable {@code failureKind} tag, not
 * the exception message.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorExceptionMessageLeakageTest {

    private static final String SENSITIVE_SECRET = "sk-REAL-SECRET-VALUE-DO-NOT-LEAK-ABCDEFGHIJKL";
    private static final String SENSITIVE_EMAIL = "alice@confidential.example.com";

    @Test
    void exceptionMessageContainingRawSecretMustNotSurfaceInPublicMetadata() {
        // Simulate a detector that unsafely includes the user input in its exception message.
        GuardrailCheckFunction leakyDetector = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                throw new IllegalStateException(
                    "Failed to parse response while scanning input containing: " + text);
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("secretKeysCheck", leakyDetector, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("please look at " + SENSITIVE_SECRET)))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, mock(CallAdvisorChain.class));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        String serialised = violations.toString();

        assertThat(serialised)
            .as(
                "Violation.exception().getMessage() must NOT reach the public metadata view — toPublicView projects "
                    + "only failureKind. A leaky detector that echoes user input in its exception message would "
                    + "otherwise smuggle secrets past the advisor boundary.")
            .doesNotContain(SENSITIVE_SECRET);

        // Positive check: the stable kind tag IS exposed.
        assertThat(violations)
            .singleElement()
            .satisfies(view -> {
                assertThat(view.get("executionFailed")).isEqualTo(true);
                assertThat(view.get("failureKind")).isInstanceOf(String.class);
            });
    }

    @Test
    void exceptionMessageContainingRawPiiMustNotSurfaceInPublicMetadata() {
        GuardrailCheckFunction leakyDetector = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) throws Exception {
                // Common leak: detector echoes the scanned text in an error message.
                throw new IllegalArgumentException(
                    "PII detector rejected malformed input starting with: "
                        + text.substring(0, Math.min(text.length(), 80)));
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", leakyDetector, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("user provided " + SENSITIVE_EMAIL + " in the request body")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, mock(CallAdvisorChain.class));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        String serialised = violations.toString();

        assertThat(serialised)
            .as("Raw PII values echoed by a detector's exception message must not leak via public metadata")
            .doesNotContain(SENSITIVE_EMAIL);
    }
}
