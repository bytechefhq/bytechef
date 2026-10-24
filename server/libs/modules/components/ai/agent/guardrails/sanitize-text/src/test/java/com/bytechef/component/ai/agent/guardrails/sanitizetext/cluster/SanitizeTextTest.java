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
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextTest {

    @Test
    void testOfReturnsDefinitionWithCorrectTypeAndName() {
        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        SanitizeText sanitizeText = new SanitizeText(clusterElementDefinitionService);

        ClusterElementDefinition<GuardrailsFunction> definition = sanitizeText.of();

        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("sanitizeText");
    }

    @Test
    void testBuildsAdvisorWithChildIdentities() throws Exception {
        ClusterElementDefinitionService clusterElementDefinitionService =
            mock(ClusterElementDefinitionService.class);
        GuardrailSanitizerFunction secretKeys = (text, context) -> text.replace("alpha", "*****");

        when(clusterElementDefinitionService.<GuardrailSanitizerFunction>getClusterElement(
            eq("secretKeys"), anyInt(), anyString())).thenReturn(secretKeys);

        Parameters extensions = ParametersFactory.create(Map.of(
            "clusterElements", Map.of(
                "sanitizeText", List.of(
                    Map.of(
                        "name", "secretKeys_1",
                        "type", "secretKeys/v1/secretKeys",
                        "parameters", Map.of())))));

        long taskExecutionId = 8_001L;

        Advisor advisor = apply(clusterElementDefinitionService, extensions, 201L, taskExecutionId);

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(chainResponse("fine"));

        ((CallAdvisor) advisor).adviseCall(request("alpha"), chain);

        List<ClusterElementCallRecord> records = ClusterElementCallRecorder.getInstance()
            .drain(taskExecutionId)
            .records();

        assertThat(records).hasSize(2);
        assertThat(records.get(0)
            .workflowNodeName()).isEqualTo("sanitizeText_1");
        assertThat(records.get(0)
            .componentName()).isEqualTo("sanitizeText");
        assertThat(records.get(0)
            .status()).isEqualTo(Status.REDACTED);
        assertThat(records.get(1)
            .workflowNodeName()).isEqualTo("secretKeys_1");
        assertThat(records.get(1)
            .componentName()).isEqualTo("secretKeys");
        assertThat(records.get(1)
            .parentId()).isEqualTo(records.get(0)
                .id());
    }

    private static Advisor apply(
        ClusterElementDefinitionService clusterElementDefinitionService, Parameters extensions, long jobId,
        long taskExecutionId) throws Exception {

        SanitizeText sanitizeText = new SanitizeText(clusterElementDefinitionService);

        GuardrailsFunction guardrailsFunction = (GuardrailsFunction) sanitizeText.of()
            .getElement();

        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getJobId()).thenReturn(jobId);
        when(context.getTaskExecutionId()).thenReturn(taskExecutionId);

        return guardrailsFunction.apply(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), extensions, Map.of(), context,
            List.of(), "sanitizeText_1", "sanitizeText");
    }

    private static ChatClientRequest request(String userText) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(userText)))
            .build();
    }

    private static ChatClientResponse chainResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }
}
