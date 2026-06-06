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

package com.bytechef.component.mongodb.action;

import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.PIPELINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Alex Bevilacqua
 */
class MongoDBAggregateActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of());

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(COLLECTION, "orders", PIPELINE, List.of(Map.of("$match", Map.of("active", true)))));

        MongoClient mockedMongoClient = mock(MongoClient.class);
        MongoCollection<Document> mockedCollection = mock(MongoCollection.class);
        AggregateIterable<Document> mockedAggregateIterable = mock(AggregateIterable.class);

        List<Document> expectedDocuments = List.of(new Document("total", 42));

        ArgumentCaptor<List<Document>> pipelineArgumentCaptor = ArgumentCaptor.forClass(List.class);

        when(mockedCollection.aggregate(pipelineArgumentCaptor.capture()))
            .thenReturn(mockedAggregateIterable);
        when(mockedAggregateIterable.into(any()))
            .thenReturn(new ArrayList<>(expectedDocuments));

        try (MockedStatic<MongoDBUtils> mongoDBUtilsMockedStatic = mockStatic(MongoDBUtils.class, CALLS_REAL_METHODS)) {
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getMongoClient(any()))
                .thenReturn(mockedMongoClient);
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getCollection(any(), any(), anyString()))
                .thenReturn(mockedCollection);

            List<Document> result = MongoDBAggregateAction.perform(
                inputParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals(expectedDocuments, result);
            assertEquals(
                List.of(new Document("$match", Map.of("active", true))), pipelineArgumentCaptor.getValue());
        }
    }
}
