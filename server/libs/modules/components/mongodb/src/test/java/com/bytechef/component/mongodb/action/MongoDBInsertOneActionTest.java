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
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DOCUMENT;
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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import java.util.Map;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class MongoDBInsertOneActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of());

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(COLLECTION, "users", DOCUMENT, Map.of("name", "Joe")));

        MongoClient mockedMongoClient = mock(MongoClient.class);
        MongoCollection<Document> mockedCollection = mock(MongoCollection.class);
        InsertOneResult mockedInsertOneResult = mock(InsertOneResult.class);

        ArgumentCaptor<Document> documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);

        when(mockedInsertOneResult.getInsertedId())
            .thenReturn(new BsonObjectId(new ObjectId("507f1f77bcf86cd799439011")));
        when(mockedCollection.insertOne(documentArgumentCaptor.capture()))
            .thenReturn(mockedInsertOneResult);

        try (MockedStatic<MongoDBUtils> mongoDBUtilsMockedStatic = mockStatic(MongoDBUtils.class, CALLS_REAL_METHODS)) {
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getMongoClient(any()))
                .thenReturn(mockedMongoClient);
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getCollection(any(), any(), anyString()))
                .thenReturn(mockedCollection);

            Map<String, Object> result = MongoDBInsertOneAction.perform(
                inputParameters, mockedConnectionParameters, mockedActionContext);

            assertEquals(Map.of("insertedId", "507f1f77bcf86cd799439011"), result);
            assertEquals(new Document("name", "Joe"), documentArgumentCaptor.getValue());
        }
    }
}
