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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * @author Ivica Cardic
 */
public class TextGenerationFirstAdvisor implements CallAdvisor {

    private static final String REDACTED_THINKING_DATA = "data";
    private static final String THINKING_SIGNATURE = "signature";

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return chatClientResponse;
        }

        List<Generation> generations = chatResponse.getResults();

        List<Generation> answerGenerations = new ArrayList<>();
        List<Generation> thinkingGenerations = new ArrayList<>();

        for (Generation generation : generations) {
            if (isThinking(generation)) {
                thinkingGenerations.add(generation);
            } else {
                answerGenerations.add(generation);
            }
        }

        if (thinkingGenerations.isEmpty() || answerGenerations.isEmpty()) {
            return chatClientResponse;
        }

        List<Generation> reorderedGenerations = new ArrayList<>(answerGenerations);

        reorderedGenerations.addAll(thinkingGenerations);

        ChatResponse reorderedChatResponse = ChatResponse.builder()
            .from(chatResponse)
            .generations(reorderedGenerations)
            .build();

        return chatClientResponse.mutate()
            .chatResponse(reorderedChatResponse)
            .build();
    }

    @Override
    public String getName() {
        return TextGenerationFirstAdvisor.class.getSimpleName();
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 1000;
    }

    private static boolean isThinking(Generation generation) {
        Map<String, Object> metadata = generation.getOutput()
            .getMetadata();

        return metadata.containsKey(THINKING_SIGNATURE) || metadata.containsKey(REDACTED_THINKING_DATA);
    }
}
