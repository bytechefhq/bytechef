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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @author Ivica Cardic
 */
class CombinedRagAdvisorTest {

    private static final String QUESTION = "When does the store open?";

    private final AdvisorChain advisorChain = mock(AdvisorChain.class);

    @Test
    void testEveryKnowledgeBaseIsSearchedWithTheOriginalQuestion() {
        VectorStore hoursVectorStore = vectorStore("The store opens at 9.");
        VectorStore holidaysVectorStore = vectorStore("The store is closed on holidays.");

        CombinedRagAdvisor combinedRagAdvisor = new CombinedRagAdvisor(
            List.of(
                QuestionAnswerAdvisor.builder(hoursVectorStore)
                    .build(),
                QuestionAnswerAdvisor.builder(holidaysVectorStore)
                    .build()));

        ChatClientRequest advisedRequest = combinedRagAdvisor.before(
            ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage(QUESTION)))
                .context(Map.of())
                .build(),
            advisorChain);

        assertThat(searchQuery(hoursVectorStore)).isEqualTo(QUESTION);
        assertThat(searchQuery(holidaysVectorStore)).isEqualTo(QUESTION);

        String augmentedUserText = advisedRequest.prompt()
            .getUserMessage()
            .getText();

        assertThat(augmentedUserText).contains(QUESTION, "The store opens at 9.", "The store is closed on holidays.");
        assertThat((List<?>) advisedRequest.context()
            .get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS)).hasSize(2);
    }

    private static VectorStore vectorStore(String documentText) {
        VectorStore vectorStore = mock(VectorStore.class);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document(documentText)));

        return vectorStore;
    }

    private static String searchQuery(VectorStore vectorStore) {
        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

        verify(vectorStore).similaritySearch(searchRequestCaptor.capture());

        return searchRequestCaptor.getValue()
            .getQuery();
    }
}
