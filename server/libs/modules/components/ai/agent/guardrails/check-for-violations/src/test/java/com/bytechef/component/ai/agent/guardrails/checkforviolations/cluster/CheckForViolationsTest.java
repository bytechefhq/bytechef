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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailContext;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailStage;
import com.bytechef.platform.component.definition.ai.agent.guardrails.Violation;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CheckForViolationsTest {

    @Test
    void testOfReturnsDefinitionWithCorrectTypeAndName() {
        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        CheckForViolations checkForViolations = new CheckForViolations(clusterElementDefinitionService);

        ClusterElementDefinition<GuardrailsFunction> definition = checkForViolations.of();

        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("checkForViolations");
    }

    @Test
    void testBuildsAdvisorWithChildIdentities() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        GuardrailCheckFunction keywords = (text, context) -> Optional.of(Violation.ofMatch("keywords", "alpha"));

        when(clusterElementDefinitionService.<GuardrailCheckFunction>getClusterElement(
            eq("keywords"), anyInt(), anyString())).thenReturn(keywords);

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "checkForViolations", List.of(
                    Map.of(
                        "name", "keywords_1",
                        "type", "keywords/v1/keywords",
                        "parameters", Map.of())))));

        long taskExecutionId = 7_001L;

        Advisor advisor = apply(clusterElementDefinitionService, extensions, 101L, taskExecutionId);

        ((CallAdvisor) advisor).adviseCall(request("alpha"), mock(CallAdvisorChain.class));

        List<ClusterElementCallRecord> records = ClusterElementCallRecorder.getInstance()
            .drain(taskExecutionId)
            .records();

        assertThat(records).hasSize(2);
        assertThat(records.get(0)
            .workflowNodeName()).isEqualTo("checkForViolations_1");
        assertThat(records.get(0)
            .componentName()).isEqualTo("checkForViolations");
        assertThat(records.get(1)
            .workflowNodeName()).isEqualTo("keywords_1");
        assertThat(records.get(1)
            .componentName()).isEqualTo("keywords");
        assertThat(records.get(1)
            .parentId()).isEqualTo(records.get(0)
                .id());
    }

    @Test
    void testSharedModelFailureIsOwnedByTheModelChild() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        ChatModel chatModel = mock(ChatModel.class);
        ModelFunction modelFunction = mock(ModelFunction.class);

        ChatOptions chatOptions = ChatOptions.builder()
            .build();

        when(chatModel.getDefaultOptions()).thenReturn(chatOptions);
        when(chatModel.getOptions()).thenReturn(chatOptions);
        when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider down"));
        when(modelFunction.apply(any(), any(), anyBoolean())).thenAnswer(invocation -> chatModel);
        when(clusterElementDefinitionService.<ModelFunction>getClusterElement(
            eq("openAi"), anyInt(), anyString())).thenReturn(modelFunction);

        GuardrailCheckFunction jailbreak = new GuardrailCheckFunction() {

            @Override
            public Optional<Violation> apply(String text, GuardrailContext context) {
                ChatClient chatClient = context.chatClient()
                    .orElseThrow();

                chatClient.prompt()
                    .user(text)
                    .call()
                    .content();

                return Optional.empty();
            }

            @Override
            public GuardrailStage stage() {
                return GuardrailStage.LLM;
            }

            @Override
            public boolean requiresChatClient() {
                return true;
            }
        };

        when(clusterElementDefinitionService.<GuardrailCheckFunction>getClusterElement(
            eq("jailbreak"), anyInt(), anyString())).thenReturn(jailbreak);

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "model", Map.of(
                    "name", "model_1",
                    "type", "openAi/v1/model",
                    "parameters", Map.of()),
                "checkForViolations", List.of(
                    Map.of(
                        "name", "jailbreak_1",
                        "type", "jailbreak/v1/jailbreak",
                        "parameters", Map.of())))));

        long taskExecutionId = 7_002L;

        Advisor advisor = apply(clusterElementDefinitionService, extensions, 102L, taskExecutionId);

        ((CallAdvisor) advisor).adviseCall(request("hello"), mock(CallAdvisorChain.class));

        List<ClusterElementCallRecord> records = ClusterElementCallRecorder.getInstance()
            .drain(taskExecutionId)
            .records();

        assertThat(records)
            .filteredOn(callRecord -> callRecord.status() == Status.FAILED)
            .extracting(ClusterElementCallRecord::workflowNodeName)
            .containsExactly("model_1");
        assertThat(records)
            .filteredOn(callRecord -> "jailbreak_1".equals(callRecord.workflowNodeName()))
            .singleElement()
            .satisfies(callRecord -> {
                assertThat(callRecord.status()).isEqualTo(Status.BLOCKED);
                assertThat(callRecord.output()).contains("could not run");
            });
    }

    private static Advisor apply(
        ClusterElementDefinitionService clusterElementDefinitionService, Parameters extensions, long jobId,
        long taskExecutionId) throws Exception {

        CheckForViolations checkForViolations = new CheckForViolations(clusterElementDefinitionService);

        GuardrailsFunction guardrailsFunction = (GuardrailsFunction) checkForViolations.of()
            .getElement();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getJobId()).thenReturn(jobId);
        when(context.getTaskExecutionId()).thenReturn(taskExecutionId);

        return guardrailsFunction.apply(
            ParametersFactory.create(Map.of("blockedMessage", "blocked")), ParametersFactory.create(Map.of()),
            extensions, Map.of(), context, List.of(), "checkForViolations_1", "checkForViolations");
    }

    private static ChatClientRequest request(String userText) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(userText)))
            .build();
    }
}
