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

package com.bytechef.component.ai.llm.advisor;

import com.bytechef.component.ai.llm.converter.StructuredOutputUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * Removes a markdown code fence around a structured output reply before
 * {@link org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor} parses it. Ordered between that
 * advisor and the model, so each attempt is unwrapped before it is validated.
 *
 * @author Ivica Cardic
 */
public class CodeFenceStrippingAdvisor implements CallAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return chatClientResponse;
        }

        List<Generation> generations = chatResponse.getResults();

        List<Generation> strippedGenerations = new ArrayList<>(generations.size());

        boolean changed = false;

        for (Generation generation : generations) {
            AssistantMessage output = generation.getOutput();

            String text = output.getText();

            if (text == null) {
                strippedGenerations.add(generation);

                continue;
            }

            String strippedText = StructuredOutputUtils.stripCodeFence(text);

            if (strippedText.equals(text)) {
                strippedGenerations.add(generation);

                continue;
            }

            AssistantMessage strippedOutput = AssistantMessage.builder()
                .content(strippedText)
                .properties(output.getMetadata())
                .toolCalls(output.getToolCalls())
                .media(output.getMedia())
                .build();

            strippedGenerations.add(new Generation(strippedOutput, generation.getMetadata()));

            changed = true;
        }

        if (!changed) {
            return chatClientResponse;
        }

        ChatResponse strippedChatResponse = ChatResponse.builder()
            .from(chatResponse)
            .generations(strippedGenerations)
            .build();

        return chatClientResponse.mutate()
            .chatResponse(strippedChatResponse)
            .build();
    }

    @Override
    public String getName() {
        return CodeFenceStrippingAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 1500;
    }
}
