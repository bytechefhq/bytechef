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

package com.bytechef.component.ai.vectorstore.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class VectorStoreConstants {

    public static final String ADDITIONAL_METADATA = "additionalMetadata";
    public static final String METADATA_FILTER = "metadataFilter";
    public static final String QUERY = "query";
    public static final String SIMILARITY_THRESHOLD = "similarityThreshold";
    public static final String TOP_K = "topK";

    public static final Property ADDITIONAL_METADATA_PROPERTY = object(ADDITIONAL_METADATA)
        .label("Additional Metadata")
        .description("Metadata key-value pairs to attach to the stored documents.")
        .additionalProperties(string(), integer(), number(), bool(), dateTime(), date(), time())
        .required(false);

    public static final Property METADATA_FILTER_PROPERTY = array(METADATA_FILTER)
        .label("Metadata Filter")
        .description(
            "List of metadata key-value pairs to filter by. Entries within a group are ANDed; groups are ORed.")
        .items(
            object()
                .additionalProperties(string(), integer(), number(), bool(), dateTime(), date(), time()))
        .required(false);

    public static final Property QUERY_PROPERTY = string(QUERY)
        .label("Query")
        .description("The query to be executed.")
        .required(true);

    public static final List<Property> SEARCH_PROPERTIES = List.of(
        integer(TOP_K)
            .label("Top K")
            .description("The top 'k' similar results to return.")
            .defaultValue(4)
            .required(false),
        number(SIMILARITY_THRESHOLD)
            .label("Similarity Threshold")
            .description(
                "Similarity threshold score to filter the search response by. Only documents " +
                    "with similarity score equal or greater than the threshold will be returned. " +
                    "A threshold value of 0 means any similarity is accepted. " +
                    "A threshold value of 1 means an exact match is required.")
            .defaultValue(0)
            .maxValue(1)
            .minValue(0)
            .required(false));

    private VectorStoreConstants() {
    }
}
