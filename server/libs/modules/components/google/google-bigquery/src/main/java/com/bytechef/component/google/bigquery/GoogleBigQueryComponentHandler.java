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

package com.bytechef.component.google.bigquery;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.bigquery.action.GoogleBigQueryQueryAction;
import com.bytechef.component.google.bigquery.connection.GoogleBigQueryConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class GoogleBigQueryComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("bigQuery")
        .title("BigQuery")
        .description(
            "BigQuery is the autonomous data to AI platform, automating the entire data life cycle, from ingestion " +
                "to AI-driven insights, so you can go from data to AI to action faster.")
        .icon("path:assets/google-bigquery.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(GoogleBigQueryConnection.CONNECTION_DEFINITION)
        .clusterElements(tool(GoogleBigQueryQueryAction.ACTION_DEFINITION))
        .actions(GoogleBigQueryQueryAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
