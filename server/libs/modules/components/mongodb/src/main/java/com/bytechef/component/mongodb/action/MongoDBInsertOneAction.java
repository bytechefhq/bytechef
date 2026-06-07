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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DOCUMENT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import java.util.Map;
import org.bson.Document;

public class MongoDBInsertOneAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("insertOne")
        .title("Insert One")
        .description("Inserts a single document into a collection.")
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to insert into.")
                .required(true),
            object(DOCUMENT)
                .label("Document")
                .description("The document to insert as a JSON object.")
                .additionalProperties(object())
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("insertedId")
                            .description("The identifier of the inserted document."))))
        .perform(MongoDBInsertOneAction::perform);

    private MongoDBInsertOneAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            InsertOneResult insertOneResult = collection.insertOne(
                MongoDBUtils.toDocument(inputParameters.getRequiredMap(DOCUMENT)));

            Object insertedId = MongoDBUtils.fromBsonValue(insertOneResult.getInsertedId());

            return Map.of("insertedId", insertedId == null ? "" : insertedId);
        }
    }
}
