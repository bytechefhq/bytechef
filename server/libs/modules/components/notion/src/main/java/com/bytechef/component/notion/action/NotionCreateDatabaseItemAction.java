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
import static com.bytechef.component.notion.constant.NotionConstants.FIELDS;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.TYPE;

import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.notion.util.NotionUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionCreateDatabaseItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createDatabaseItem")
        .title("Create Database Item")
        .description(".")
        .properties(
            string(ID)
                .label("Database ID")
                .description("The ID of the database.")
                .options(NotionUtils.gePageOrDatabaseIdOptions(false))
                .required(true),
            dynamicProperties(FIELDS)
                .properties((PropertiesFunction) NotionUtils::createPropertiesForDatabaseItem)
                .propertiesLookupDependsOn(ID))
        .output()
        .perform(NotionCreateDatabaseItemAction::perform);

    private NotionCreateDatabaseItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String databaseId = inputParameters.getRequiredString(ID);

        Map<String, Object> propertiesMap = NotionUtils.convertPropertiesToNotionValues(
            context, inputParameters.getMap(FIELDS), databaseId);

        return context.http(http -> http.post("/pages"))
            .body(
                Http.Body.of(
                    "parent", Map.of(TYPE, "database_id", "database_id", databaseId),
                    "properties", propertiesMap))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
