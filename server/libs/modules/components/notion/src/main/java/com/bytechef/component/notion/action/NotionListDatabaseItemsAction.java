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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.notion.constant.NotionConstants.DIRECTION;
import static com.bytechef.component.notion.constant.NotionConstants.ID;
import static com.bytechef.component.notion.constant.NotionConstants.PROPERTY;
import static com.bytechef.component.notion.util.NotionUtils.getAllItems;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.notion.util.NotionUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class NotionListDatabaseItemsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listDatabaseItems")
        .title("List Database Items")
        .description("List all items in a Notion database.")
        .properties(
            string(ID)
                .label("Database ID")
                .description("The ID of the database.")
                .options((OptionsFunction<String>) NotionUtils::getDatabaseIdOptions)
                .required(true),
            string(PROPERTY)
                .label("Sort By")
                .description("Property to sort the items by.")
                .options((OptionsFunction<String>) NotionUtils::getDatabasePropertyOptions)
                .required(true),
            string(DIRECTION)
                .label("Sort Direction")
                .description("The direction to sort the items.")
                .options(
                    option("Ascending", "ascending"),
                    option("Descending", "descending"))
                .defaultValue("descending")
                .required(true))
        .output()
        .perform(NotionListDatabaseItemsAction::perform);

    private NotionListDatabaseItemsAction() {
    }

    public static List<?> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String url = "/data_sources/%s/query".formatted(inputParameters.getRequiredString(ID));
        String property = inputParameters.getRequiredString(PROPERTY);
        String direction = inputParameters.getRequiredString(DIRECTION);

        return getAllItems(
            context, url, false, "sorts", List.of(Map.of(PROPERTY, property, DIRECTION, direction)));
    }
}
