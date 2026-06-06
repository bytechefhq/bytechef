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
import static com.bytechef.component.mongodb.constant.MongoDBConstants.FILTER;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Alex Bevilacqua
 */
class MongoDBUpdateOneActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of());

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(
                COLLECTION, "users", FILTER, Map.of("name", "Joe"),
                UPDATE, Map.of("$set", Map.of("active", true))));

        MongoClient mockedMongoClient = mock(MongoClient.class);
        MongoCollection<Document> mockedCollection = mock(MongoCollection.class);
        UpdateResult mockedUpdateResult = mock(UpdateResult.class);

        ArgumentCaptor<Document> filterArgumentCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> updateArgumentCaptor = ArgumentCaptor.forClass(Document.class);

        when(mockedUpdateResult.getMatchedCount())
            .thenReturn(1L);
        when(mockedUpdateResult.getModifiedCount())
            .thenReturn(1L);
        when(mockedUpdateResult.getUpsertedId())
            .thenReturn(null);
        when(mockedCollection.updateOne(
            filterArgumentCaptor.capture(), updateArgumentCaptor.capture(), any(UpdateOptions.class)))
                .thenReturn(mockedUpdateResult);

        try (MockedStatic<MongoDBUtils> mongoDBUtilsMockedStatic = mockStatic(MongoDBUtils.class)) {
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getMongoClient(any()))
                .thenReturn(mockedMongoClient);
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getCollection(any(), any(), anyString()))
                .thenReturn(mockedCollection);
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.toDocument(any()))
                .thenCallRealMethod();

            Map<String, Object> result = MongoDBUpdateOneAction.perform(
                inputParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals(1L, result.get("matchedCount"));
            assertEquals(1L, result.get("modifiedCount"));
            assertNull(result.get("upsertedId"));
            assertEquals(new Document("name", "Joe"), filterArgumentCaptor.getValue());
            assertEquals(new Document("$set", Map.of("active", true)), updateArgumentCaptor.getValue());
        }
    }
}
