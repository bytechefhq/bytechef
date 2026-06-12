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
import static com.bytechef.component.mongodb.constant.MongoDBConstants.CONNECTION_STRING;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DATABASE;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DOCUMENT;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.ORDER_BY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.mongodb.action.MongoDBInsertOneAction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class MongoDBNewDocumentTriggerIntTest {

    private static final String DATABASE_NAME = "testdb";
    private static final String COLLECTION_NAME = "events";

    @Container
    private static final MongoDBContainer MONGODB_CONTAINER = new MongoDBContainer(
        DockerImageName.parse("mongo:8.2"));

    private Parameters connectionParameters;

    @BeforeEach
    void setUp() {
        connectionParameters = MockParametersFactory.create(
            Map.of(
                CONNECTION_STRING, MONGODB_CONTAINER.getConnectionString(),
                DATABASE, DATABASE_NAME));
    }

    @Test
    void testPollEmitsNewDocuments() {
        insertEvent(1);
        insertEvent(2);

        Parameters triggerInput = MockParametersFactory.create(
            Map.of(COLLECTION, COLLECTION_NAME, ORDER_BY, "seq"));

        // First poll records the current position and skips the existing backlog
        PollOutput firstPoll = MongoDBNewDocumentTrigger.poll(
            triggerInput, connectionParameters, MockParametersFactory.create(Map.of()), null);

        assertTrue(firstPoll.records()
            .isEmpty());

        insertEvent(3);

        PollOutput secondPoll = MongoDBNewDocumentTrigger.poll(
            triggerInput, connectionParameters, toParameters(firstPoll.closureParameters()), null);

        List<?> records = secondPoll.records();

        assertEquals(1, records.size());
        assertEquals(3, ((Map<?, ?>) records.get(0)).get("seq"));
    }

    private void insertEvent(int seq) {
        MongoDBInsertOneAction.perform(
            MockParametersFactory.create(Map.of(COLLECTION, COLLECTION_NAME, DOCUMENT, Map.of("seq", seq))),
            connectionParameters, null);
    }

    private static Parameters toParameters(Map<String, ?> map) {
        return MockParametersFactory.create(new HashMap<>(map));
    }
}
