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
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.datastream.ItemReader;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class AirtableItemReader implements ItemReader {

    public static final ClusterElementDefinition<AirtableItemReader> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<AirtableItemReader>clusterElement("read")
            .title("Read table row")
            .type(SOURCE)
            .object(AirtableItemReader.class)
            .properties(
                string("baseId").label("Base ID")
                    .description("ID of the base where table is located.")
                    .required(true)
                    .options((OptionsDataSource.ActionOptionsFunction<String>) AirtableUtils::getBaseIdOptions),
                string("tableId").label("Table ID")
                    .description("ID of the table where the record is located.")
                    .required(true)
                    .options((OptionsDataSource.ActionOptionsFunction<String>) AirtableUtils::getTableIdOptions)
                    .optionsLookupDependsOn("baseId"));

    @Override
    public Map<String, ?> read() throws Exception {
        // TODO

        return null;
    }
}
