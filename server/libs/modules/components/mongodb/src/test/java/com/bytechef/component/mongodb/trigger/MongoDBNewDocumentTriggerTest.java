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

package com.bytechef.component.mongodb.trigger;

import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.ORDER_BY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class MongoDBNewDocumentTriggerTest {

    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of());

    @Test
    @SuppressWarnings("unchecked")
    void testPollReturnsNewDocuments() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(COLLECTION, "users", ORDER_BY, "_id"));
        Parameters closureParameters = MockParametersFactory.create(Map.of("lastValue", 10));

        MongoClient mockedMongoClient = mock(MongoClient.class);
        MongoCollection<Document> mockedCollection = mock(MongoCollection.class);
        FindIterable<Document> mockedFindIterable = mock(FindIterable.class);

        List<Document> newDocuments = new ArrayList<>(List.of(
            new Document("_id", 11), new Document("_id", 12)));

        ArgumentCaptor<Document> filterArgumentCaptor = ArgumentCaptor.forClass(Document.class);

        when(mockedCollection.find(filterArgumentCaptor.capture()))
            .thenReturn(mockedFindIterable);
        when(mockedFindIterable.sort(any()))
            .thenReturn(mockedFindIterable);
        when(mockedFindIterable.into(any()))
            .thenReturn(newDocuments);

        try (MockedStatic<MongoDBUtils> mongoDBUtilsMockedStatic = mockStatic(MongoDBUtils.class)) {
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getMongoClient(any()))
                .thenReturn(mockedMongoClient);
            mongoDBUtilsMockedStatic.when(() -> MongoDBUtils.getCollection(any(), any(), anyString()))
                .thenReturn(mockedCollection);

            PollOutput pollOutput = MongoDBNewDocumentTrigger.poll(
                inputParameters, mockedConnectionParameters, closureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(newDocuments, Map.of("lastValue", 12), false), pollOutput);
            assertEquals(new Document("_id", new Document("$gt", 10)), filterArgumentCaptor.getValue());
        }
    }
}
