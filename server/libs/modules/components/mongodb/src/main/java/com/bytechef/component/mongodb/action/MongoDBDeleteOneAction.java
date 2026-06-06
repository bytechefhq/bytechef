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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.FILTER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import java.util.Map;
import org.bson.Document;

/**
 * @author Alex Bevilacqua
 */
public class MongoDBDeleteOneAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteOne")
        .title("Delete One")
        .description("Deletes a single document from a collection matching a filter.")
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to delete from.")
                .required(true),
            object(FILTER)
                .label("Filter")
                .description("The query filter that selects the document to delete, as a JSON object.")
                .additionalProperties(object())
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("deletedCount")
                            .description("The number of documents deleted."))))
        .perform(MongoDBDeleteOneAction::perform);

    private MongoDBDeleteOneAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            DeleteResult deleteResult = collection.deleteOne(
                MongoDBUtils.toDocument(inputParameters.getRequiredMap(FILTER)));

            return Map.of("deletedCount", deleteResult.getDeletedCount());
        }
    }
}
