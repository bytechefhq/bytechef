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

package com.bytechef.component.mongodb.util;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.CONNECTION_STRING;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DATABASE;

import com.bytechef.component.definition.Parameters;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoDriverInformation;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

public final class MongoDBUtils {

    private static final MongoDriverInformation DRIVER_INFORMATION = MongoDriverInformation.builder()
        .driverName("ByteChef")
        .build();

    private MongoDBUtils() {
    }

    public static MongoClient getMongoClient(Parameters connectionParameters) {
        ConnectionString connectionString = new ConnectionString(
            connectionParameters.getRequiredString(CONNECTION_STRING));

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
            .applyConnectionString(connectionString);

        String username = connectionParameters.getString(USERNAME);
        String password = connectionParameters.getString(PASSWORD);
        String database = connectionParameters.getRequiredString(DATABASE);

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            settingsBuilder.credential(
                MongoCredential.createCredential(username, database, password.toCharArray()));
        }

        return MongoClients.create(settingsBuilder.build(), DRIVER_INFORMATION);
    }

    public static MongoCollection<Document> getCollection(
        MongoClient mongoClient, Parameters connectionParameters, String collectionName) {

        MongoDatabase mongoDatabase = mongoClient.getDatabase(connectionParameters.getRequiredString(DATABASE));

        return mongoDatabase.getCollection(collectionName);
    }

    public static Document toDocument(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return new Document();
        }

        return new Document(new LinkedHashMap<>(map));
    }

    public static List<Document> toDocuments(List<? extends Map<String, ?>> maps) {
        if (maps == null) {
            return List.of();
        }

        return maps.stream()
            .map(MongoDBUtils::toDocument)
            .toList();
    }

    /**
     * Converts documents returned by the driver into plain JSON-friendly maps, replacing BSON-specific types (such as
     * {@link ObjectId}) with values that serialize cleanly in workflow output.
     */
    public static List<Map<String, Object>> normalizeDocuments(List<Document> documents) {
        return documents.stream()
            .map(MongoDBUtils::normalizeDocument)
            .toList();
    }

    public static Map<String, Object> normalizeDocument(Document document) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : document.entrySet()) {
            result.put(entry.getKey(), normalizeValue(entry.getValue()));
        }

        return result;
    }

    /**
     * Unwraps a BSON identifier (e.g. the {@code _id} returned by an insert) into a plain value, so that the value can
     * be used directly in a subsequent filter.
     */
    public static Object fromBsonValue(BsonValue bsonValue) {
        if (bsonValue == null) {
            return null;
        }

        if (bsonValue.isObjectId()) {
            return bsonValue.asObjectId()
                .getValue()
                .toHexString();
        }

        if (bsonValue.isString()) {
            return bsonValue.asString()
                .getValue();
        }

        if (bsonValue.isInt32()) {
            return bsonValue.asInt32()
                .getValue();
        }

        if (bsonValue.isInt64()) {
            return bsonValue.asInt64()
                .getValue();
        }

        return bsonValue.toString();
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeValue(Object value) {
        return switch (value) {
            case null -> null;
            case ObjectId objectId -> objectId.toHexString();
            case Decimal128 decimal128 -> decimal128.bigDecimalValue();
            case Binary binary -> Base64.getEncoder()
                .encodeToString(binary.getData());
            case Document document -> normalizeDocument(document);
            case Map<?, ?> map -> normalizeDocument(new Document((Map<String, Object>) map));
            case List<?> list -> list.stream()
                .map(MongoDBUtils::normalizeValue)
                .toList();
            default -> value;
        };
    }
}
