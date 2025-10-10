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
import com.bytechef.component.definition.datastream.ItemWriter;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class AirtableItemWriter implements ItemWriter {

    public static final ModifiableClusterElementDefinition<AirtableItemWriter> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<AirtableItemWriter>clusterElement("write")
            .title("Write tabel rows")
            .description("Writes a list of rows to a table.")
            .type(DESTINATION)
            .object(AirtableItemWriter.class)
            .properties(
                string("baseId").label("Base ID")
                    .description("ID of the base where table is located.")
                    .required(true)
                    .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getBaseIdOptions),
                string("tableId").label("Table ID")
                    .description("The table where the record will be created.")
                    .required(true)
                    .options((ActionDefinition.OptionsFunction<String>) AirtableUtils::getTableIdOptions)
                    .optionsLookupDependsOn("baseId"));

    private String baseId;
    private Context context;
    private String tableId;

    @Override
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        this.baseId = inputParameters.getRequiredString("baseId");
        this.tableId = inputParameters.getRequiredString("tableId");
        this.context = context;
    }

    @Override
    public void write(List<? extends Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Map<String, ?>> records = new ArrayList<>(items.size());

        for (Map<String, ?> item : items) {
            Object fieldsObj = item.get("fields");

            Map<?, ?> fieldsMap = fieldsObj instanceof Map<?, ?> fm ? fm : item;

            records.add(Map.of("fields", fieldsMap));
        }

        Map<String, Object> payload = Map.of("records", records);

        Http.Response response = context
            .http(http -> http.post("/%s/%s".formatted(baseId, tableId)))
            .body(Http.Body.of(payload))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute();

        Map<String, ?> body = response.getBody(new TypeReference<>() {});

        if (body.containsKey("error")) {
            Object error = body.get("error");

            String message = error instanceof Map<?, ?> errorMap
                ? (String) errorMap.get("message") : String.valueOf(error);

            throw new ProviderException.BadRequestException(message);
        }
    }
}
