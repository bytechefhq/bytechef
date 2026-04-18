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

package com.bytechef.component.ai.agent.guardrails.checkforviolations.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
 * Spring-wired test for the CheckForViolations cluster element.
 *
 * <p>
 * <b>Not a CLAUDE.md-style {@code IntTest}:</b> the ByteChef convention (see CLAUDE.md) reserves the {@code IntTest}
 * suffix for Testcontainers-backed end-to-end tests that talk to real infrastructure. This test does NOT use
 * Testcontainers, does NOT talk to a real LLM, and stubs out the {@link ClusterElementDefinitionService} with Mockito.
 * It is therefore named {@code ClusterWiringTest} to reflect its actual scope: the Spring DI wiring of the
 * {@code @Component("checkForViolations_v1_ClusterElement")} bean and the cluster-element extension-map shape.
 *
 * <p>
 * Exercises the full pipeline: extensions-parsing → child cluster-element resolution → advisor assembly → per-check
 * execution → aggregated blocking response. Catches regressions in constructor wiring, {@code @Component} discovery,
 * and the cluster-element extension-map shape that unit tests would miss.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringJUnitConfig(classes = CheckForViolationsClusterWiringTest.TestConfig.class)
class CheckForViolationsClusterWiringTest {

    @Autowired
    private CheckForViolations checkForViolations;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @BeforeEach
    void resetMocks() {
        reset(clusterElementDefinitionService);
    }

    @Test
    void testMultipleGuardrailsAllRunAndAggregateBeforeBlocking() {
        AtomicInteger secondGuardrailCalls = new AtomicInteger();

        GuardrailCheckFunction firstGuardrail = (text, context) -> text.contains("bad")
            ? Optional.of(Violation.ofMatch("first", "bad"))
            : Optional.empty();

        GuardrailCheckFunction secondGuardrail = (text, context) -> {
            secondGuardrailCalls.incrementAndGet();

            return Optional.empty();
        };

        Advisor advisor = buildAdvisorWithTwoGuardrails(firstGuardrail, secondGuardrail);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = runAgainst((CallAdvisor) advisor, chain, "this is bad input");

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("Request blocked by guardrail.");

        assertThat(secondGuardrailCalls.get())
            .as("aggregating advisor runs every check even after the first violation")
            .isEqualTo(1);
        verify(chain, never()).nextCall(any());
    }

    @Test
    void testNoViolationsPassesThroughToChain() {
        AtomicInteger firstGuardrailCalls = new AtomicInteger();
        AtomicInteger secondGuardrailCalls = new AtomicInteger();

        GuardrailCheckFunction firstGuardrail = (text, context) -> {
            firstGuardrailCalls.incrementAndGet();

            return Optional.empty();
        };

        GuardrailCheckFunction secondGuardrail = (text, context) -> {
            secondGuardrailCalls.incrementAndGet();

            return Optional.empty();
        };

        Advisor advisor = buildAdvisorWithTwoGuardrails(firstGuardrail, secondGuardrail);

        ChatClientResponse response = runAgainst((CallAdvisor) advisor, mock(CallAdvisorChain.class), "all clear");

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("LLM response");
        assertThat(firstGuardrailCalls.get()).isEqualTo(1);
        assertThat(secondGuardrailCalls.get()).isEqualTo(1);
    }

    @Test
    void testDeclaredOrderIsPreservedForChecks() {
        List<String> callOrder = new ArrayList<>();

        GuardrailCheckFunction first = (text, context) -> {
            callOrder.add("first");

            return Optional.empty();
        };

        GuardrailCheckFunction second = (text, context) -> {
            callOrder.add("second");

            return Optional.empty();
        };

        Advisor advisor = buildAdvisorWithTwoGuardrails(first, second);

        runAgainst((CallAdvisor) advisor, mock(CallAdvisorChain.class), "clean");

        assertThat(callOrder).containsExactly("first", "second");
    }

    @Test
    void testGuardrailExceptionIsFailedClosed() {
        GuardrailCheckFunction throwing = (text, context) -> {
            throw new IllegalStateException("simulated config error");
        };

        GuardrailCheckFunction shouldNotRun = (text, context) -> Optional.empty();

        Advisor advisor = buildAdvisorWithTwoGuardrails(throwing, shouldNotRun);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);
        ChatClientResponse response = runAgainst((CallAdvisor) advisor, chain, "anything");

        assertThat(response.chatResponse()
            .getResult()
            .getOutput()
            .getText()).isEqualTo("Request blocked by guardrail.");
        verify(chain, never()).nextCall(any());
    }

    private Advisor buildAdvisorWithTwoGuardrails(
        GuardrailCheckFunction firstFunction, GuardrailCheckFunction secondFunction) {

        when(clusterElementDefinitionService.<GuardrailCheckFunction>getClusterElement(
            eq("guardrailFirst"), anyInt(), anyString()))
                .thenReturn(firstFunction);
        when(clusterElementDefinitionService.<GuardrailCheckFunction>getClusterElement(
            eq("guardrailSecond"), anyInt(), anyString()))
                .thenReturn(secondFunction);

        GuardrailsFunction guardrailsFunction = (GuardrailsFunction) checkForViolations.of()
            .getElement();

        Parameters inputParameters = ParametersFactory.create(Map.of(
            "blockedMessage", "Request blocked by guardrail."));

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "checkForViolations", List.of(
                    Map.of(
                        "name", "firstNode",
                        "type", "guardrailFirst/v1/checkA",
                        "parameters", Map.of()),
                    Map.of(
                        "name", "secondNode",
                        "type", "guardrailSecond/v1/checkB",
                        "parameters", Map.of())))));

        try {
            return guardrailsFunction.apply(inputParameters, ParametersFactory.create(Map.of()), extensions, Map.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ChatClientResponse runAgainst(CallAdvisor advisor, CallAdvisorChain chain, String userText) {
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(List.<Message>of(new UserMessage(userText))))
            .build();

        ChatResponse chainResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("LLM response"))))
            .build();
        ChatClientResponse downstreamResponse = ChatClientResponse.builder()
            .chatResponse(chainResponse)
            .build();

        when(chain.nextCall(request)).thenReturn(downstreamResponse);

        return advisor.adviseCall(request, chain);
    }

    /**
     * Minimal Spring context: one production {@link CheckForViolations} bean wired with a Mockito-mocked
     * {@link ClusterElementDefinitionService}. Spring's constructor injection runs, which validates the
     * {@code @Component} wiring without the weight of a full {@code @SpringBootTest} autoconfiguration.
     */
    @Configuration
    static class TestConfig {

        @Bean
        ClusterElementDefinitionService clusterElementDefinitionService() {
            return mock(ClusterElementDefinitionService.class);
        }

        @Bean
        CheckForViolations checkForViolations(ClusterElementDefinitionService clusterElementDefinitionService) {
            return new CheckForViolations(clusterElementDefinitionService);
        }
    }
}
