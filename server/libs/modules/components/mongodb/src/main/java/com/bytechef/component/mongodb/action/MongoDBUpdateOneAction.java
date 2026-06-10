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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.FILTER;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.UPDATE;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.UPSERT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import java.util.HashMap;
import java.util.Map;
import org.bson.BsonValue;
import org.bson.Document;

public class MongoDBUpdateOneAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateOne")
        .title("Update One")
        .description("Updates a single document in a collection matching a filter.")
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to update.")
                .required(true),
            object(FILTER)
                .label("Filter")
                .description("The query filter that selects the document to update, as a JSON object.")
                .required(true),
            object(UPDATE)
                .label("Update")
                .description("The update to apply, as a JSON object using update operators, e.g. " +
                    "{\"$set\": {\"status\": \"active\"}}.")
                .required(true),
            bool(UPSERT)
                .label("Upsert")
                .description("Whether to insert a new document when no document matches the filter.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("matchedCount")
                            .description("The number of documents that matched the filter."),
                        integer("modifiedCount")
                            .description("The number of documents that were modified."),
                        string("upsertedId")
                            .description("The identifier of the upserted document, if any."))))
        .perform(MongoDBUpdateOneAction::perform);

    private MongoDBUpdateOneAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            UpdateOptions updateOptions = new UpdateOptions()
                .upsert(inputParameters.getBoolean(UPSERT, false));

            UpdateResult updateResult = collection.updateOne(
                MongoDBUtils.toDocument(inputParameters.getRequiredMap(FILTER)),
                MongoDBUtils.toDocument(inputParameters.getRequiredMap(UPDATE)),
                updateOptions);

            Map<String, Object> result = new HashMap<>();

            result.put("matchedCount", updateResult.getMatchedCount());
            result.put("modifiedCount", updateResult.getModifiedCount());

            BsonValue upsertedId = updateResult.getUpsertedId();

            result.put("upsertedId", upsertedId == null ? null : upsertedId.toString());

            return result;
        }
    }
}
