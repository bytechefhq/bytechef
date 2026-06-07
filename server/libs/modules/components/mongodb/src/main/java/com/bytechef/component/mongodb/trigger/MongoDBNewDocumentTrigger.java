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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.COLLECTION;
import static com.bytechef.component.mongodb.constant.MongoDBConstants.ORDER_BY;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.mongodb.util.MongoDBUtils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;

public class MongoDBNewDocumentTrigger {

    private static final String LAST_VALUE = "lastValue";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newDocument")
        .title("New Document")
        .description("Triggers when a new document is added to a collection.")
        .type(TriggerType.POLLING)
        .properties(
            string(COLLECTION)
                .label("Collection")
                .description("The name of the collection to watch.")
                .required(true),
            string(ORDER_BY)
                .label("Order By Field")
                .description(
                    "The field used to detect new documents. Use a monotonically increasing field such as _id " +
                        "or a creation timestamp.")
                .defaultValue("_id")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(object())
                    .description("The list of new documents.")))
        .poll(MongoDBNewDocumentTrigger::poll);

    private MongoDBNewDocumentTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        String orderBy = inputParameters.getRequiredString(ORDER_BY);
        Object lastValue = closureParameters.get(LAST_VALUE);

        try (MongoClient mongoClient = MongoDBUtils.getMongoClient(connectionParameters)) {
            MongoCollection<Document> collection = MongoDBUtils.getCollection(
                mongoClient, connectionParameters, inputParameters.getRequiredString(COLLECTION));

            // On the first poll, record the current position and skip the existing backlog
            if (lastValue == null) {
                Document newest = collection.find()
                    .sort(new Document(orderBy, -1))
                    .first();

                Object position = newest == null ? "" : newest.get(orderBy);

                return new PollOutput(List.of(), Map.of(LAST_VALUE, position), false);
            }

            List<Document> documents = collection.find(new Document(orderBy, new Document("$gt", lastValue)))
                .sort(new Document(orderBy, 1))
                .into(new ArrayList<>());

            Object newLastValue = lastValue;

            if (!documents.isEmpty()) {
                Document lastDocument = documents.get(documents.size() - 1);

                newLastValue = lastDocument.get(orderBy);
            }

            return new PollOutput(documents, Map.of(LAST_VALUE, newLastValue), false);
        }
    }
}
