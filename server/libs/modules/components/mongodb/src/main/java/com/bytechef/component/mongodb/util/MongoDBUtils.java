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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/**
 * @author Alex Bevilacqua
 */
public final class MongoDBUtils {

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

        return MongoClients.create(settingsBuilder.build());
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

        return new Document(new java.util.LinkedHashMap<>(map));
    }

    public static List<Document> toDocuments(List<? extends Map<String, ?>> maps) {
        if (maps == null) {
            return List.of();
        }

        return maps.stream()
            .map(MongoDBUtils::toDocument)
            .toList();
    }
}
