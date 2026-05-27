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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

        Advisor advisor = buildAdvisorWithTwoSanitizers(first, second);

        ChatClientResponse response = runAgainst((CallAdvisor) advisor, "has foo inside");

        String text = response.chatResponse()
            .getResult()
            .getOutput()
            .getText();

        assertThat(text).isEqualTo("has [redacted] inside");

        // SanitizeTextAdvisor runs the chain twice per call — once on the inbound user message
        // (sanitiseRequest) and once on the outbound assistant message (rewriteResponse) — because
        // validateInput and validateOutput both default to true. Chain order is preserved within
        // each pass; the user message contains no "foo"/"<X>" so it passes through unchanged while
        // still invoking both sanitizers.
        assertThat(callOrder).containsExactly("first", "second", "first", "second");
    }

    @Test
    void testOutputPassReMasksUserPiiThatLlmEchoes() {
        // Operators may toggle validateOutput=false on individual sanitizers as a perf optimization, but the
        // shared SanitizeText wiring must still run the same sanitizer chain on the outbound model response.
        // Otherwise an LLM that faithfully echoes back PII from the user prompt would leak it to the caller
        // even though the input pass masked it.
        GuardrailSanitizerFunction emailMasker = (text, context) -> text.replaceAll(
            "[A-Za-z0-9._-]+@[A-Za-z0-9.-]+", "<EMAIL>");

        when(clusterElementDefinitionService.<GuardrailSanitizerFunction>getClusterElement(
            eq("piiSanitize"), anyInt(), anyString()))
                .thenReturn(emailMasker);

        GuardrailsFunction guardrailsFunction = sanitizeText.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of());

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "sanitizeText", List.of(
                    Map.of(
                        "name", "piiNode",
                        "type", "piiSanitize/v1/piiSanitize")))));

        Advisor advisor;

        try {
            advisor =
                guardrailsFunction.apply(inputParameters, ParametersFactory.create(Map.of()), extensions, Map.of(),
                    mock(com.bytechef.component.definition.Context.class), List.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(List.<Message>of(new UserMessage("contact alice@example.com"))))
            .build();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        ChatResponse chainResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("You said alice@example.com to me"))))
            .build();
        ChatClientResponse downstreamResponse = ChatClientResponse.builder()
            .chatResponse(chainResponse)
            .build();

        when(chain.nextCall(any())).thenReturn(downstreamResponse);

        ChatClientResponse response = ((CallAdvisor) advisor).adviseCall(request, chain);

        String text = response.chatResponse()
            .getResult()
            .getOutput()
            .getText();

        assertThat(text)
            .as("LLM echoed the user's email back; the output pass must re-mask it")
            .isEqualTo("You said <EMAIL> to me")
            .doesNotContain("alice@example.com");
    }

    @Test
    void testNoSanitizersPassesThrough() {
        GuardrailsFunction guardrailsFunction = sanitizeText.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of());
        Parameters extensions = ParametersFactory.create(Map.of("clusterElements", Map.of()));

        Advisor advisor;

        try {
            advisor = guardrailsFunction.apply(
                inputParameters, ParametersFactory.create(Map.of()), extensions, Map.of(),
                mock(com.bytechef.component.definition.Context.class), List.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ChatClientResponse response = runAgainst((CallAdvisor) advisor, "unchanged output");

        String text = response.chatResponse()
            .getResult()
            .getOutput()
            .getText();

        assertThat(text).isEqualTo("unchanged output");
    }

    private Advisor buildAdvisorWithTwoSanitizers(
        GuardrailSanitizerFunction firstFunction, GuardrailSanitizerFunction secondFunction) {

        when(clusterElementDefinitionService.<GuardrailSanitizerFunction>getClusterElement(
            eq("sanitizerFirst"), anyInt(), anyString()))
                .thenReturn(firstFunction);
        when(clusterElementDefinitionService.<GuardrailSanitizerFunction>getClusterElement(
            eq("sanitizerSecond"), anyInt(), anyString()))
                .thenReturn(secondFunction);

        GuardrailsFunction guardrailsFunction = sanitizeText.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of());

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "sanitizeText", List.of(
                    Map.of(
                        "name", "firstNode",
                        "type", "sanitizerFirst/v1/sanitizeA"),
                    Map.of(
                        "name", "secondNode",
                        "type", "sanitizerSecond/v1/sanitizeB")))));

        try {
            return guardrailsFunction.apply(inputParameters, ParametersFactory.create(Map.of()), extensions, Map.of(),
                mock(com.bytechef.component.definition.Context.class), List.of());
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
