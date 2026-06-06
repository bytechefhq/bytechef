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
import static com.bytechef.component.mongodb.constant.MongoDBConstants.FILTER;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.LIMIT;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.PROJECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.SORT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

/**
 * @author Alex Bevilacqua
 */
public class MongoDBFindAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("find")
        .title("Find")
        .description("Finds documents in a collection matching a filter.")
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to query.")
                .required(true),
            object(FILTER)
                .label("Filter")
                .description("The query filter as a JSON object. Leave empty to match all documents.")
                .additionalProperties(object())
                .required(false),
            object(PROJECTION)
                .label("Projection")
                .description("The fields to include or exclude as a JSON object, e.g. {\"name\": 1, \"_id\": 0}.")
                .additionalProperties(integer())
                .required(false),
            object(SORT)
                .label("Sort")
                .description("The sort order as a JSON object, e.g. {\"createdAt\": -1} for descending.")
                .additionalProperties(integer())
                .required(false),
            integer(LIMIT)
                .label("Limit")
                .description("The maximum number of documents to return. Leave empty for no limit.")
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(object())
                    .description("The list of matching documents.")))
        .perform(MongoDBFindAction::perform);

    private MongoDBFindAction() {
    }

    public static List<Document> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            FindIterable<Document> findIterable = collection.find(
                MongoDBUtils.toDocument(inputParameters.getMap(FILTER)));

            Document projection = MongoDBUtils.toDocument(inputParameters.getMap(PROJECTION));

            if (!projection.isEmpty()) {
                findIterable.projection(projection);
            }

            Document sort = MongoDBUtils.toDocument(inputParameters.getMap(SORT));

            if (!sort.isEmpty()) {
                findIterable.sort(sort);
            }

            Integer limit = inputParameters.getInteger(LIMIT);

            if (limit != null && limit > 0) {
                findIterable.limit(limit);
            }

            return findIterable.into(new ArrayList<>());
        }
    }
}
