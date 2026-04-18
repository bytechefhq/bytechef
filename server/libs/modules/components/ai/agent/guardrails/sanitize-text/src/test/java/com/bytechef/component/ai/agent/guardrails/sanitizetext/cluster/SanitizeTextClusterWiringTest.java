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

package com.bytechef.component.ai.agent.guardrails.sanitizetext.cluster;

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_MODE;
import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.FAIL_OPEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Spring-wired integration test for the SanitizeText cluster element.
 *
 * <p>
 * Spring-wired test for the SanitizeText cluster element. <b>Not a CLAUDE.md-style {@code IntTest}</b>: no
 * Testcontainers, no real LLM. Named {@code ClusterWiringTest} to reflect its actual scope — see
 * {@code CheckForViolationsClusterWiringTest} for the longer rationale.
 *
 * <p>
 * Mirrors {@code CheckForViolationsClusterWiringTest}: runs inside a real Spring
 * {@link org.springframework.context.ApplicationContext} so the {@link SanitizeText} bean is constructor-injected with
 * {@link ClusterElementDefinitionService} via Spring DI. The dependency is a Mockito mock per test for stubbing; the
 * cluster element itself is the production {@code @Component("sanitizeText_v1_ClusterElement")}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringJUnitConfig(classes = SanitizeTextClusterWiringTest.TestConfig.class)
class SanitizeTextClusterWiringTest {

    @Autowired
    private SanitizeText sanitizeText;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @BeforeEach
    void resetMocks() {
        reset(clusterElementDefinitionService);
    }

    @Test
    void testTwoSanitizersChainInOrder() {
        List<String> callOrder = new ArrayList<>();

        GuardrailSanitizerFunction first = (text, context) -> {
            callOrder.add("first");

            return text.replace("foo", "<X>");
        };

        GuardrailSanitizerFunction second = (text, context) -> {
            callOrder.add("second");

            return text.replace("<X>", "[redacted]");
        };

        Advisor advisor = buildAdvisorWithTwoSanitizers(first, second, Map.of(), Map.of());

        ChatClientResponse response = runAgainst((CallAdvisor) advisor, "has foo inside");

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("has [redacted] inside");
        assertThat(callOrder).containsExactly("first", "second");
    }

    @Test
    void testSanitizerExceptionUnderFailOpenSkipsAndContinues() {
        // With explicit FAIL_OPEN configured on the throwing sanitizer, its failure is logged and subsequent sanitizers
        // still run against the last-good intermediate text.
        GuardrailSanitizerFunction throwing = (text, context) -> {
            throw new IllegalStateException("simulated failure");
        };

        GuardrailSanitizerFunction second = (text, context) -> text.replace("secret", "[redacted]");

        Advisor advisor = buildAdvisorWithTwoSanitizers(
            throwing, second,
            Map.of(FAIL_MODE, FAIL_OPEN),
            Map.of(FAIL_MODE, FAIL_OPEN));

        ChatClientResponse response = runAgainst((CallAdvisor) advisor, "model output with secret");

        String text = response.chatResponse()
            .getResult()
            .getOutput()
            .getText();

        assertThat(text).isEqualTo("model output with [redacted]");
    }

    @Test
    void testNoSanitizersPassesThrough() {
        GuardrailsFunction guardrailsFunction = (GuardrailsFunction) sanitizeText.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(Map.of("clusterElements", Map.of()));

        Advisor advisor;

        try {
            advisor = guardrailsFunction.apply(
                inputParameters, ParametersFactory.create(Map.of()), extensions, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ChatClientResponse response = runAgainst((CallAdvisor) advisor, "unchanged output");

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("unchanged output");
    }

    private Advisor buildAdvisorWithTwoSanitizers(
        GuardrailSanitizerFunction firstFunction, GuardrailSanitizerFunction secondFunction,
        Map<String, Object> firstParams, Map<String, Object> secondParams) {

        when(clusterElementDefinitionService.<GuardrailSanitizerFunction>getClusterElement(
            eq("sanitizerFirst"), anyInt(), anyString()))
                .thenReturn(firstFunction);
        when(clusterElementDefinitionService.<GuardrailSanitizerFunction>getClusterElement(
            eq("sanitizerSecond"), anyInt(), anyString()))
                .thenReturn(secondFunction);

        GuardrailsFunction guardrailsFunction = (GuardrailsFunction) sanitizeText.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of());

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "sanitizeText", List.of(
                    Map.of(
                        "name", "firstNode",
                        "type", "sanitizerFirst/v1/sanitizeA",
                        "parameters", firstParams),
                    Map.of(
                        "name", "secondNode",
                        "type", "sanitizerSecond/v1/sanitizeB",
                        "parameters", secondParams)))));

        try {
            return guardrailsFunction.apply(inputParameters, ParametersFactory.create(Map.of()), extensions, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ChatClientResponse runAgainst(CallAdvisor advisor, String assistantText) {
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(List.<Message>of(new UserMessage("user message"))))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        ChatResponse chainResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(assistantText))))
            .build();
        ChatClientResponse downstreamResponse = ChatClientResponse.builder()
            .chatResponse(chainResponse)
            .build();

        when(chain.nextCall(request)).thenReturn(downstreamResponse);

        return advisor.adviseCall(request, chain);
    }

    @Configuration
    static class TestConfig {

        @Bean
        ClusterElementDefinitionService clusterElementDefinitionService() {
            return mock(ClusterElementDefinitionService.class);
        }

        @Bean
        SanitizeText sanitizeText(ClusterElementDefinitionService clusterElementDefinitionService) {
            return new SanitizeText(clusterElementDefinitionService);
        }
    }
}
