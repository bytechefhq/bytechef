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

package com.bytechef.component.ai.agent.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

/**
 * Searches every RAG advisor of an agent with the user's original question and augments the prompt once with all the
 * retrieved documents.
 *
 * @author Ivica Cardic
 */
final class CombinedRagAdvisor implements BaseAdvisor {

    private static final List<String> DOCUMENT_CONTEXT_KEYS = List.of(
        QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS, RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);

    private final QueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
        .allowEmptyContext(true)
        .build();
    private final List<BaseAdvisor> ragAdvisors;

    CombinedRagAdvisor(List<BaseAdvisor> ragAdvisors) {
        if (ragAdvisors.isEmpty()) {
            throw new IllegalArgumentException("ragAdvisors must not be empty");
        }

        this.ragAdvisors = List.copyOf(ragAdvisors);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String userText = chatClientRequest.prompt()
            .getUserMessage()
            .getText();

        if (userText == null || userText.isBlank()) {
            return chatClientRequest;
        }

        List<Document> documents = new ArrayList<>();

        for (BaseAdvisor ragAdvisor : ragAdvisors) {
            ChatClientRequest advisedRequest = ragAdvisor.before(chatClientRequest, advisorChain);

            documents.addAll(getDocuments(advisedRequest.context()));
        }

        Map<String, Object> context = new HashMap<>(chatClientRequest.context());

        context.put(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS, documents);

        Query augmentedQuery = queryAugmenter.augment(new Query(userText), documents);

        return chatClientRequest.mutate()
            .prompt(chatClientRequest.prompt()
                .augmentUserMessage(augmentedQuery.text()))
            .context(context)
            .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return ragAdvisors.getFirst()
            .getOrder();
    }

    private static List<Document> getDocuments(Map<String, Object> context) {
        List<Document> documents = new ArrayList<>();

        for (String documentContextKey : DOCUMENT_CONTEXT_KEYS) {
            if (context.get(documentContextKey) instanceof List<?> contextDocuments) {
                for (Object contextDocument : contextDocuments) {
                    if (contextDocument instanceof Document document) {
                        documents.add(document);
                    }
                }
            }
        }

        return documents;
    }
}
