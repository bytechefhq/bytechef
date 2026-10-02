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

import static com.bytechef.component.ai.agent.guardrails.constant.GuardrailsConstants.VALIDATE_INPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction;
import com.bytechef.platform.component.definition.ai.agent.guardrails.HumanToolResponses;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SanitizeTextAdvisorHumanToolResponseTest {

    private static final GuardrailSanitizerFunction MASK_CARD_NUMBER =
        (text, context) -> text.replace("4111111111111111", "<CARD>");

    @Test
    void testSanitizesTheHumanResponseButNotOtherToolResponses() {
        SanitizeTextAdvisor advisor = createAdvisor();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any(ChatClientRequest.class))).thenReturn(assistantResponse("ok"));

        ToolResponseMessage toolResponseMessage = HumanToolResponses.markHumanResponse(
            ToolResponseMessage.builder()
                .responses(
                    List.of(
                        new ToolResponseMessage.ToolResponse("call_a", "lookUpCard", "4111111111111111"),
                        new ToolResponseMessage.ToolResponse("call_b", "askUserQuestion",
                            "{\"card\":\"4111111111111111\"}")))
                .build(),
            "call_b");

        advisor.adviseCall(request(new UserMessage("which card?"), toolResponseMessage), chain);

        ArgumentCaptor<ChatClientRequest> chatClientRequestArgumentCaptor =
            ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(chatClientRequestArgumentCaptor.capture());

        List<Message> instructions = chatClientRequestArgumentCaptor.getValue()
            .prompt()
            .getInstructions();

        ToolResponseMessage sanitizedToolResponseMessage = (ToolResponseMessage) instructions.get(1);

        assertThat(sanitizedToolResponseMessage.getResponses()).containsExactly(
            new ToolResponseMessage.ToolResponse("call_a", "lookUpCard", "4111111111111111"),
            new ToolResponseMessage.ToolResponse("call_b", "askUserQuestion", "{\"card\":\"<CARD>\"}"));
        assertThat(HumanToolResponses.getHumanResponseText(sanitizedToolResponseMessage))
            .isEqualTo("{\"card\":\"<CARD>\"}");
    }

    @Test
    void testLeavesAToolResponseWithoutAHumanResponseUnchanged() {
        SanitizeTextAdvisor advisor = createAdvisor();

        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        ChatClientRequest request = request(
            new UserMessage("which card?"),
            ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse("call_a", "lookUpCard", "4111111111111111")))
                .build());

        when(chain.nextCall(request)).thenReturn(assistantResponse("ok"));

        advisor.adviseCall(request, chain);

        verify(chain).nextCall(request);
    }

    private static SanitizeTextAdvisor createAdvisor() {
        return SanitizeTextAdvisor.builder()
            .add(
                "mask", MASK_CARD_NUMBER, ParametersFactory.create(Map.of(VALIDATE_INPUT, true)),
                ParametersFactory.create(Map.of()))
            .context(mock(Context.class))
            .build();
    }

    private static ChatClientRequest request(Message... messages) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of(messages)))
            .build();
    }

    private static ChatClientResponse assistantResponse(String text) {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(text))))
            .build();

        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .build();
    }
}
