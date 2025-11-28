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

package com.bytechef.component.notion.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.notion.constant.NotionConstants.CONTENT;
import static com.bytechef.component.notion.constant.NotionConstants.FIELDS;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.TEXT;
import static com.bytechef.component.notion.constant.NotionConstants.TYPE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.notion.util.NotionUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionCreateDatabaseItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createDatabaseItem")
        .title("Create Database Item")
        .description("Creates a new item in Notion database.")
        .properties(
            string(ID)
                .label("Database ID")
                .description("The ID of the database.")
                .options((OptionsFunction<String>) NotionUtils::getDatabaseIdOptions)
                .required(true),
            dynamicProperties(FIELDS)
                .properties((PropertiesFunction) NotionUtils::createPropertiesForDatabaseItem)
                .propertiesLookupDependsOn(ID)
                .required(true),
            string(CONTENT)
                .label("Content")
                .description("The content to append to item.")
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output()
        .perform(NotionCreateDatabaseItemAction::perform);

    private NotionCreateDatabaseItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String databaseId = inputParameters.getRequiredString(ID);

        Map<String, Object> propertiesMap = NotionUtils.convertPropertiesToNotionValues(
            context, inputParameters.getMap(FIELDS), databaseId);

        List<Map<String, ?>> children = null;

        String content = inputParameters.getString(CONTENT);

        if (content != null) {
            Map<String, ?> contentMap = Map.of(
                "object", "block",
                TYPE, "paragraph",
                "paragraph", Map.of("rich_text", List.of(Map.of(TYPE, TEXT, TEXT, Map.of(CONTENT, content)))));

            children = List.of(contentMap);
        }

        return context.http(http -> http.post("/pages"))
            .body(
                Http.Body.of(
                    "parent", Map.of(TYPE, "database_id", "database_id", databaseId),
                    "properties", propertiesMap,
                    "children", children))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
