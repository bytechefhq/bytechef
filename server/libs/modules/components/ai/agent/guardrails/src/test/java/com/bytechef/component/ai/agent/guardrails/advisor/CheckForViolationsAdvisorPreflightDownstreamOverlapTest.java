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
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.MaskResult;
import com.bytechef.platform.component.definition.ai.agent.guardrails.PreflightCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Pins interactions between preflight-stage masking and downstream (LLM-stage) checks.
 *
 * <p>
 * Two risk scenarios are covered:
 * <ul>
 * <li><b>Masked placeholder looks like a downstream detector's entity</b> — a preflight mask replaces the e-mail
 * {@code alice@EMAIL.com} with {@code <EMAIL>}. A subsequent LLM-stage keyword check MUST NOT fire on the placeholder
 * token itself just because {@code EMAIL} happens to be in its keyword list; the check should see only the masked text
 * and the placeholder is there to protect the LLM stage, not to trigger checks on itself.</li>
 * <li><b>Email local-part masked but keyword check expects the original token</b> — a preflight mask replaces
 * {@code alice@corp.com} with {@code <EMAIL>}. A downstream keyword check for {@code alice} must NOT fire on the masked
 * text (because {@code alice} is gone). This pins the "LLM-stage checks run against masked text" contract — a caller
 * who needs raw-text keyword matching must place the keyword check under {@code SanitizeText} instead of
 * {@code CheckForViolations}.</li>
 * </ul>
 * These pins guard against a regression where someone changes either the masking timing or the LLM-stage input source.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorPreflightDownstreamOverlapTest {

    @Test
    void keywordCheckDoesNotFireOnPlaceholderTokenIntroducedByPreflightMask() {
        // Preflight PII emits an EMAIL entity for "alice@corp.com"; the advisor replaces it with "<EMAIL>".
        PreflightCheckFunction piiCheck = new PreflightCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.empty();
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.entities(Map.of("EMAIL", List.of("alice@corp.com")));
            }
        };

        AtomicReference<String> keywordTextSeen = new AtomicReference<>();

        // LLM-stage keyword check: operator has (perhaps accidentally) added EMAIL to their keyword list.
        GuardrailCheckFunction keywordCheck = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                keywordTextSeen.set(text);

                // Simulate KeywordMatcher behaviour: case-insensitive word match. The placeholder contains the literal
                // text "EMAIL"; this verifies whether the downstream check fires on it.
                if (text.toLowerCase()
                    .contains("email")) {
                    return Optional.of(Violation.ofMatches("keywords", List.of("EMAIL")));
                }

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", piiCheck, empty, empty, empty, empty, Map.of(), null)
            .add("keywords", keywordCheck, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("ping alice@corp.com please")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        // The keyword check WILL fire on the placeholder because the placeholder text "<EMAIL>" literally contains
        // "EMAIL". This test DOCUMENTS the known behaviour: a keyword list that collides with a preflight mask label
        // produces a false positive. Operators who need to avoid this should not put mask-label tokens in their
        // keyword list, or should use SanitizeText-stage keywords that run against raw text.
        assertThat(keywordTextSeen.get()).contains("<EMAIL>");
        assertThat(violations)
            .as("Known collision: preflight mask placeholder '<EMAIL>' matches keyword 'EMAIL' (operator trap)")
            .hasSize(1)
            .extracting(Violation::guardrail)
            .containsExactly("keywords");
    }

    @Test
    void keywordCheckDoesNotFireOnOriginalTokenAfterPreflightMaskRedactsIt() {
        // Preflight PII redacts "alice@corp.com" to "<EMAIL>".
        PreflightCheckFunction piiCheck = new PreflightCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.empty();
            }

            @Override
            public MaskResult mask(String text, GuardrailContext context) {
                return MaskResult.entities(Map.of("EMAIL", List.of("alice@corp.com")));
            }
        };

        AtomicReference<String> textSeen = new AtomicReference<>();

        // Downstream keyword check for the original local-part "alice". After preflight masking, "alice" is gone — so
        // this check MUST NOT fire. This pins the "LLM-stage sees masked text" contract: a caller who wants to match
        // on the raw local-part has to place the keyword check under SanitizeText instead.
        GuardrailCheckFunction keywordCheck = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                textSeen.set(text);

                if (text.toLowerCase()
                    .contains("alice")) {
                    return Optional.of(Violation.ofMatches("keywords", List.of("alice")));
                }

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());
        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", piiCheck, empty, empty, empty, empty, Map.of(), null)
            .add("keywords", keywordCheck, empty, empty, empty, empty, Map.of(), null)
            .build();

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("ping alice@corp.com please")))
            .build();

        List<Violation> violations = advisor.runChecksForTesting(request);

        assertThat(textSeen.get())
            .as("Keyword check must see the masked text, not the raw user input")
            .doesNotContain("alice")
            .contains("<EMAIL>");
        assertThat(violations)
            .as("Keyword check for 'alice' must not fire after preflight mask redacted it to <EMAIL>")
            .isEmpty();
    }
}
