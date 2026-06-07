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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.DOCUMENTS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertManyResult;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class MongoDBInsertManyAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("insertMany")
        .title("Insert Many")
        .description("Inserts multiple documents into a collection.")
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to insert into.")
                .required(true),
            array(DOCUMENTS)
                .label("Documents")
                .description("The documents to insert, each as a JSON object.")
                .items(object().additionalProperties(object()))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("insertedCount")
                            .description("The number of documents inserted."),
                        array("insertedIds")
                            .items(string())
                            .description("The identifiers of the inserted documents."))))
        .perform(MongoDBInsertManyAction::perform);

    private MongoDBInsertManyAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            List<Document> documents = MongoDBUtils.toDocuments(
                inputParameters.getRequiredList(DOCUMENTS, new TypeReference<Map<String, ?>>() {}));

            InsertManyResult insertManyResult = collection.insertMany(documents);

            List<Object> insertedIds = insertManyResult.getInsertedIds()
                .values()
                .stream()
                .map(MongoDBUtils::fromBsonValue)
                .toList();

            return Map.of(
                "insertedCount", insertedIds.size(),
                "insertedIds", insertedIds);
        }
    }
}
