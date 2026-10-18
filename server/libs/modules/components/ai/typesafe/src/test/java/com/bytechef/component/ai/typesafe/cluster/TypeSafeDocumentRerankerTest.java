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

package com.bytechef.component.ai.typesafe.cluster;

import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.INSTRUCTIONS;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.MINIMUM_SCORE;
import static com.bytechef.component.ai.typesafe.constant.TypeSafeConstants.TOP_K;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.JevBatchOptions;
import org.springaicommunity.typesafe.JevBatchResult;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.NoulAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springaicommunity.typesafe.response.Usage;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

/**
 * @author Ivica Cardic
 */
class TypeSafeDocumentRerankerTest {

    private static final Query QUERY = new Query("Did Milton make landfall in Florida, and where?");

    private static final List<Document> DOCUMENTS = List.of(
        new Document("track", "Milton crossed the Gulf of Mexico from the west.", Map.of()),
        new Document("landfall", "Milton made landfall near Siesta Key on October 9, 2024.", Map.of()),
        new Document("strength", "Milton weakened to a Category 3 hurricane before landfall.", Map.of()));

    private final List<SystemOneRequest> capturedRequests = new ArrayList<>();
    private final TypeSafeClient typeSafeClient = mock(TypeSafeClient.class);

    @Test
    void testProcess() {
        mockAnswers(0.40, 0.97, 0.88);

        List<Document> documents = TypeSafeDocumentReranker.create(
            MockParametersFactory.create(Map.of(TOP_K, 2)), typeSafeClient)
            .process(QUERY, DOCUMENTS);

        assertEquals(List.of("landfall", "strength"), getIds(documents));
        assertEquals(0.97, documents.getFirst()
            .getMetadata()
            .get("jev.rerank.score"));
    }

    @Test
    void testProcessDropsPassagesBelowMinimumScore() {
        mockAnswers(0.40, 0.97, 0.88);

        List<Document> documents = TypeSafeDocumentReranker.create(
            MockParametersFactory.create(Map.of(MINIMUM_SCORE, 0.5)), typeSafeClient)
            .process(QUERY, DOCUMENTS);

        assertEquals(List.of("landfall", "strength"), getIds(documents));
    }

    @Test
    void testProcessWithCustomInstructions() {
        mockAnswers(0.40, 0.97, 0.88);

        TypeSafeDocumentReranker.create(
            MockParametersFactory.create(Map.of(INSTRUCTIONS, "Does the `passage` name where the storm came ashore?")),
            typeSafeClient)
            .process(QUERY, DOCUMENTS);

        Noul question = (Noul) capturedRequests.getFirst()
            .questions()
            .get("answers_query");

        assertEquals("Does the `passage` name where the storm came ashore?", question.instructions()
            .value());
    }

    private void mockAnswers(double... scores) {
        when(typeSafeClient.defaultModel())
            .thenReturn("jev-latest");
        when(typeSafeClient.systemOneAll(anyList(), any(JevBatchOptions.class)))
            .thenAnswer(invocation -> {
                List<SystemOneRequest> requests = invocation.getArgument(0);
                List<JevBatchResult<SystemOneResponse>> results = new ArrayList<>();

                capturedRequests.addAll(requests);

                for (int i = 0; i < requests.size(); i++) {
                    SystemOneResponse response = new SystemOneResponse(
                        "jev-1.13.0", Map.of("answers_query", new NoulAnswer(scores[i])), Usage.EMPTY);

                    results.add(new JevBatchResult<>(i, response, null));
                }

                return results;
            });
    }

    private static List<String> getIds(List<Document> documents) {
        return documents.stream()
            .map(Document::getId)
            .toList();
    }
}
