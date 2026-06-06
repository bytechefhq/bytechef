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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.PIPELINE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;

/**
 * @author Alex Bevilacqua
 */
public class MongoDBAggregateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("aggregate")
        .title("Aggregate")
        .description("Runs an aggregation pipeline against a collection.")
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to aggregate.")
                .required(true),
            array(PIPELINE)
                .label("Pipeline")
                .description("The aggregation pipeline, an ordered list of stages, each as a JSON object, e.g. " +
                    "{\"$match\": {\"active\": true}}.")
                .items(object().additionalProperties(object()))
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(object())
                    .description("The list of documents produced by the aggregation pipeline.")))
        .perform(MongoDBAggregateAction::perform);

    private MongoDBAggregateAction() {
    }

    public static List<Document> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            List<Document> pipeline = MongoDBUtils.toDocuments(
                inputParameters.getRequiredList(PIPELINE, new TypeReference<Map<String, ?>>() {}));

            return collection.aggregate(pipeline)
                .into(new ArrayList<>());
        }
    }
}
