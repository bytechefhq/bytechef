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

package com.bytechef.component.airtable.datastream;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.component.exception.ProviderException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class AirtableItemReader implements ItemReader {

    public static final ModifiableClusterElementDefinition<AirtableItemReader> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<AirtableItemReader>clusterElement("read")
            .title("Read table row")
            .description("Reads a single row from a table.")
            .type(SOURCE)
            .object(AirtableItemReader.class)
            .properties(
                string("baseId").label("Base ID")
                    .description("ID of the base where table is located.")
                    .required(true)
                    .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getBaseIdOptions),
                string("tableId").label("Table ID")
                    .description("ID of the table where the record is located.")
                    .required(true)
                    .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getTableIdOptions)
                    .optionsLookupDependsOn("baseId"));

    private String baseId;
    private Context context;
    private String nextOffset;
    private Iterator<?> recordIterator = Collections.emptyIterator();
    private String tableId;

    @Override
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        this.baseId = inputParameters.getRequiredString("baseId");
        this.context = context;
        this.tableId = inputParameters.getRequiredString("tableId");

        loadPage(null);
    }

    @Override
    public Map<String, Object> read() {
        // If current page exhausted, try to load the next page using offset
        while (!recordIterator.hasNext()) {
            if (nextOffset == null || nextOffset.isEmpty()) {
                return null;
            }

            loadPage(nextOffset);
        }

        Object next = recordIterator.next();

        if (next instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> record = (Map<String, Object>) map;

            @SuppressWarnings("unchecked")
            Map<String, Object> fields = (Map<String, Object>) record.get("fields");

            return fields;
        }

        // Fallback: wrap non-map record as value
        return Map.of("value", next);
    }

    private void loadPage(String offset) {
        Http.Executor executor = context.http(http -> http.get("/%s/%s".formatted(baseId, tableId)));

        if (offset != null && !offset.isEmpty()) {
            executor.queryParameter("offset", offset);
        }

        Http.Response response = executor
            .configuration(Http.responseType(ResponseType.JSON))
            .execute();

        Map<String, ?> body = response.getBody(new TypeReference<>() {});

        if (body.containsKey("error")) {
            Object error = body.get("error");

            String message = error instanceof Map<?, ?> errorMap
                ? (String) errorMap.get("message") : String.valueOf(error);

            throw new ProviderException.BadRequestException(message);
        }

        Object recordsObj = body.get("records");

        List<?> records = recordsObj instanceof List<?> list ? list : List.of();

        this.nextOffset = (String) body.get("offset");
        this.recordIterator = records.iterator();
    }
}
