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

package com.bytechef.component.mongodb;

import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.CONNECTION_STRING;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DATABASE;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DOCUMENT;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DOCUMENTS;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.FILTER;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.PIPELINE;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.action.MongoDBAggregateAction;
import com.bytechef.component.mongodb.action.MongoDBDeleteManyAction;
import com.bytechef.component.mongodb.action.MongoDBDeleteOneAction;
import com.bytechef.component.mongodb.action.MongoDBFindAction;
import com.bytechef.component.mongodb.action.MongoDBInsertManyAction;
import com.bytechef.component.mongodb.action.MongoDBInsertOneAction;
import com.bytechef.component.mongodb.action.MongoDBUpdateManyAction;
import com.bytechef.component.mongodb.action.MongoDBUpdateOneAction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for the MongoDB connector actions and trigger, exercised against a real MongoDB instance spun up
 * via Testcontainers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class MongoDBComponentHandlerIntTest {

    private static final String DATABASE_NAME = "testdb";
    private static final String COLLECTION_NAME = "people";

    @Container
    private static final MongoDBContainer MONGODB_CONTAINER = new MongoDBContainer(
        DockerImageName.parse("mongo:7.0"));

    private Parameters connectionParameters;

    @BeforeEach
    void setUp() {
        connectionParameters = MockParametersFactory.create(
            Map.of(
                CONNECTION_STRING, MONGODB_CONTAINER.getConnectionString(),
                DATABASE, DATABASE_NAME));

        cleanUp();
    }

    @AfterAll
    void tearDown() {
        cleanUp();
    }

    @Test
    void testInsertOneAndFind() {
        Map<String, Object> insertResult = MongoDBInsertOneAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, DOCUMENT, Map.of("name", "Joe", "age", 30))),
            connectionParameters, null);

        Object insertedId = insertResult.get("insertedId");

        assertTrue(insertedId instanceof String id && !id.isBlank());

        List<Map<String, Object>> documents = MongoDBFindAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of("name", "Joe"))),
            connectionParameters, null);

        assertEquals(1, documents.size());
        assertEquals("Joe", documents.get(0)
            .get("name"));
        assertEquals(30, documents.get(0)
            .get("age"));

        // Round trip: the _id returned by find is the normalized hex string and matches insertOne's insertedId,
        // so it can be fed straight back into a filter.
        Object foundId = documents.get(0)
            .get("_id");

        assertEquals(insertedId, foundId);

        List<Map<String, Object>> byId = MongoDBFindAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of("name", "Joe"))),
            connectionParameters, null);

        assertEquals(insertedId, byId.get(0)
            .get("_id"));
    }

    @Test
    void testInsertManyAndFindWithSortAndLimit() {
        insertPeople();

        List<Map<String, Object>> documents = MongoDBFindAction.perform(
            inputParameters(
                Map.of(
                    COLLECTION, COLLECTION_NAME, FILTER, Map.of(),
                    "sort", Map.of("age", 1), "limit", 2)),
            connectionParameters, null);

        assertEquals(2, documents.size());
        assertEquals("Ann", documents.get(0)
            .get("name"));
        assertEquals("Bob", documents.get(1)
            .get("name"));
    }

    @Test
    void testUpdateOne() {
        insertPeople();

        Map<String, Object> updateResult = MongoDBUpdateOneAction.perform(
            inputParameters(
                Map.of(
                    COLLECTION, COLLECTION_NAME, FILTER, Map.of("name", "Bob"),
                    UPDATE, Map.of("$set", Map.of("age", 99)))),
            connectionParameters, null);

        assertEquals(1L, updateResult.get("matchedCount"));
        assertEquals(1L, updateResult.get("modifiedCount"));

        List<Map<String, Object>> documents = MongoDBFindAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of("name", "Bob"))),
            connectionParameters, null);

        assertEquals(99, documents.get(0)
            .get("age"));
    }

    @Test
    void testUpdateMany() {
        insertPeople();

        Map<String, Object> updateResult = MongoDBUpdateManyAction.perform(
            inputParameters(
                Map.of(
                    COLLECTION, COLLECTION_NAME, FILTER, Map.of("active", true),
                    UPDATE, Map.of("$set", Map.of("active", false)))),
            connectionParameters, null);

        assertEquals(2L, updateResult.get("matchedCount"));
        assertEquals(2L, updateResult.get("modifiedCount"));
    }

    @Test
    void testDeleteOne() {
        insertPeople();

        Map<String, Object> deleteResult = MongoDBDeleteOneAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of("name", "Ann"))),
            connectionParameters, null);

        assertEquals(1L, deleteResult.get("deletedCount"));

        List<Map<String, Object>> documents = MongoDBFindAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of())),
            connectionParameters, null);

        assertEquals(2, documents.size());
    }

    @Test
    void testDeleteMany() {
        insertPeople();

        Map<String, Object> deleteResult = MongoDBDeleteManyAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of("active", true))),
            connectionParameters, null);

        assertEquals(2L, deleteResult.get("deletedCount"));
    }

    @Test
    void testAggregate() {
        insertPeople();

        Map<String, Object> groupStage = new HashMap<>();

        groupStage.put("_id", null);
        groupStage.put("totalAge", Map.of("$sum", "$age"));

        List<Map<String, Object>> documents = MongoDBAggregateAction.perform(
            inputParameters(
                Map.of(
                    COLLECTION, COLLECTION_NAME,
                    PIPELINE,
                    List.of(
                        Map.of("$match", Map.of("active", true)),
                        Map.of("$group", groupStage)))),
            connectionParameters, null);

        assertEquals(1, documents.size());
        assertEquals(55, documents.get(0)
            .get("totalAge"));
    }

    private void insertPeople() {
        MongoDBInsertManyAction.perform(
            inputParameters(
                Map.of(
                    COLLECTION, COLLECTION_NAME,
                    DOCUMENTS,
                    List.of(
                        Map.of("name", "Ann", "age", 25, "active", true),
                        Map.of("name", "Bob", "age", 30, "active", true),
                        Map.of("name", "Cara", "age", 40, "active", false)))),
            connectionParameters, null);
    }

    private void cleanUp() {
        MongoDBDeleteManyAction.perform(
            inputParameters(Map.of(COLLECTION, COLLECTION_NAME, FILTER, Map.of())), connectionParameters, null);
    }

    private static Parameters inputParameters(Map<String, Object> map) {
        return MockParametersFactory.create(map);
    }
}
