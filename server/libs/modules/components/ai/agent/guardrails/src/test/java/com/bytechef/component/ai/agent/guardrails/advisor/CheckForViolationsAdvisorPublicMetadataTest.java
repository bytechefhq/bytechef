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
 * Pins the metadata contract: the violations serialised on the blocked response must use {@link Violation#info()} (with
 * {@code maskEntities} and other internal keys stripped), must NEVER expose raw {@code matchedSubstrings} (which for
 * PII/secret/URL detectors are the sensitive values themselves), and must emit a {@code matchCount} summary plus a
 * {@code failureKind} when a check failed.
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsAdvisorPublicMetadataTest {

    @Test
    void blockedResponseMetadataStripsInternalMaskEntitiesAndRawSubstrings() {
        GuardrailCheckFunction pii = (text, context) -> Optional.of(Violation.ofMatches(
            "pii",
            List.of("a@b.com"),
            Map.of(
                "entityTypes", List.of("EMAIL"),
                "maskEntities", Map.of("EMAIL", List.of("a@b.com")))));

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("reach me at a@b.com")))
            .build();

        when(chain.nextCall(any(ChatClientRequest.class))).thenAnswer(invocation -> {
            throw new AssertionError("Chain must not be called when a violation fires");
        });

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfacedViolations)
            .hasSize(1)
            .singleElement()
            .satisfies(view -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> info = (Map<String, Object>) view.get("info");

                assertThat(info)
                    .containsEntry("entityTypes", List.of("EMAIL"))
                    .doesNotContainKey("maskEntities");

                assertThat(view.get("guardrail")).isEqualTo("pii");
                assertThat(view.get("matchCount")).isEqualTo(1);
                assertThat(view)
                    .doesNotContainKey("matchedSubstrings")
                    .doesNotContainKey("confidenceScore");
            });
    }

    @Test
    void rawMatchedSubstringsNeverLeakThroughMetadata() {
        GuardrailCheckFunction secrets = (text, context) -> Optional.of(Violation.ofMatches(
            "secretKeysCheck",
            List.of("sk-ABCDEFGHIJKLMNOPQRSTUVWXYZ", "AKIA0123456789ABCDEF"),
            Map.of("providerTypes", List.of("openai", "aws_access_key"))));

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("secretKeysCheck", secrets, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("token sk-ABCDEFGHIJKLMNOPQRSTUVWXYZ AKIA0123456789ABCDEF")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        String serialised = surfacedViolations.toString();

        assertThat(serialised)
            .doesNotContain("sk-ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            .doesNotContain("AKIA0123456789ABCDEF");

        assertThat(surfacedViolations)
            .singleElement()
            .satisfies(view -> {
                assertThat(view.get("matchCount")).isEqualTo(2);
                assertThat(view).doesNotContainKey("matchedSubstrings");
            });
    }

    @Test
    void classifiedViolationMetadataIncludesConfidenceScore() {
        GuardrailCheckFunction llm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.of(Violation.ofClassification("nsfw", 0.92));
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("nsfw", llm, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("bad text")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfacedViolations)
            .singleElement()
            .satisfies(view -> assertThat(view).containsEntry("confidenceScore", 0.92));
    }

    @Test
    void classifiedViolationMetadataAlsoStripsInternalKeys() {
        GuardrailCheckFunction llm = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                return Optional.of(Violation.ofClassification(
                    "custom", 0.77,
                    Map.of(
                        "reason", "off-topic",
                        "maskEntities", Map.of("ignored", List.of("x")))));
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("custom", llm, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("anything")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfacedViolations)
            .singleElement()
            .satisfies(view -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> info = (Map<String, Object>) view.get("info");

                assertThat(info)
                    .containsEntry("reason", "off-topic")
                    .doesNotContainKey("maskEntities");
            });
    }

    @Test
    void executionFailureMetadataSurfacesFailureKind() {
        // RuntimeException is deliberately chosen over IllegalStateException/IllegalArgumentException/NPE/CCE —
        // those types are on isConfigurationError's allowlist and would surface as CONFIGURATION:<class>, not
        // UNKNOWN:<class>. Plain RuntimeException is the cleanest exemplar of a "genuinely unknown" failure.
        GuardrailCheckFunction broken = (text, context) -> {
            throw new RuntimeException("simulated LLM outage");
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("broken", broken, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hi")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfacedViolations)
            .singleElement()
            .satisfies(view -> {
                assertThat(view.get("executionFailed")).isEqualTo(true);
                // Non-GuardrailException, non-configuration causes surface as UNKNOWN:<SimpleClassName> so
                // downstream consumers can distinguish "known guardrail failure kind" from "unexpected runtime
                // exception"; GuardrailException subclasses surface their stable GuardrailExceptionKind name;
                // configuration-classified causes surface as CONFIGURATION:<SimpleClassName>.
                assertThat(view.get("failureKind")).isEqualTo("UNKNOWN:RuntimeException");
                assertThat(view.get("matchCount")).isEqualTo(0);
            });
    }

    @Test
    void executionFailureMetadataSurfacesGuardrailExceptionKindForKnownCauses() {
        GuardrailCheckFunction broken = (text, context) -> {
            throw new com.bytechef.component.ai.agent.guardrails.MissingModelChildException("jailbreak");
        };

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("jailbreak", broken, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("hi")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfacedViolations)
            .singleElement()
            .satisfies(view -> assertThat(view.get("failureKind")).isEqualTo("MISSING_MODEL"));
    }

    @Test
    void multipleViolationsSurfaceInAdvisorConfigurationOrder() {
        // Downstream consumers (UI, audit logs) rely on a deterministic ordering so that "which guardrail fired
        // first" is reproducible. The advisor iterates its configured check list in declaration order, so violations
        // must surface in the same order — NOT re-sorted by name, severity, or confidence. This pins the contract so
        // a future refactor that switches to a parallel stream or sorted collection produces a visible test failure
        // instead of a subtle ordering drift that breaks observability.
        GuardrailCheckFunction pii = (text, context) -> Optional.of(
            Violation.ofMatches("pii", List.of("a@b.com"), Map.of("entityTypes", List.of("EMAIL"))));
        GuardrailCheckFunction secrets = (text, context) -> Optional.of(
            Violation.ofMatches("secretKeysCheck", List.of("sk-ABC"), Map.of("providerTypes", List.of("openai"))));
        GuardrailCheckFunction keywords = (text, context) -> Optional.of(
            Violation.ofMatches("keywords", List.of("forbidden"), Map.of("matchedKeywords", List.of("forbidden"))));

        Parameters empty = ParametersFactory.create(Map.of());

        CheckForViolationsAdvisor advisor = CheckForViolationsAdvisor.builder()
            .blockedMessage("blocked")
            .add("pii", pii, empty, empty, empty, empty, Map.of(), null)
            .add("secretKeysCheck", secrets, empty, empty, empty, empty, Map.of(), null)
            .add("keywords", keywords, empty, empty, empty, empty, Map.of(), null)
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage("some text with a@b.com, sk-ABC, and forbidden words")))
            .build();

        ChatClientResponse response = advisor.adviseCall(request, chain);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> surfacedViolations = (List<Map<String, Object>>) response.chatResponse()
            .getMetadata()
            .get(VIOLATIONS_METADATA_KEY);

        assertThat(surfacedViolations)
            .extracting(view -> view.get("guardrail"))
            .containsExactly("pii", "secretKeysCheck", "keywords");
    }
}
