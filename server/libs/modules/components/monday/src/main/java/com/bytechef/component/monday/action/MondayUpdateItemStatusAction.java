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

package com.bytechef.component.monday.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.COLUMN_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.ITEM_ID;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.STATUS;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayOptionUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class MondayUpdateItemStatusAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateItemStatus")
        .title("Update Item Status")
        .description(
            "Update status column on a specific item. Available only for boards that have defined status column.")
        .help("", "https://docs.bytechef.io/reference/components/monday_v1#update-item-status")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .description("ID of the workspace where the board is located.")
                .options((OptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(false),
            string(BOARD_ID)
                .label("Board ID")
                .description(
                    "ID of the board where the item is located. Returns only boards with defined status column.")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardIdWithStatusOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true),
            string(COLUMN_ID)
                .label("Column ID")
                .description("Column ID.")
                .options((OptionsFunction<String>) MondayOptionUtils::getColumnStatusIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID, BOARD_ID)
                .required(true),
            string(ITEM_ID)
                .label("Item ID")
                .description("ID of the item to update.")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardItemsOptions)
                .optionsLookupDependsOn(BOARD_ID, WORKSPACE_ID)
                .required(true),
            string(STATUS)
                .label("Status")
                .description("The status label.")
                .options((OptionsFunction<String>) MondayOptionUtils::getStatusOptions)
                .optionsLookupDependsOn(BOARD_ID, COLUMN_ID)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("change_column_value")
                            .properties(
                                string(ID)
                                    .description("ID of the item."),
                                string(NAME)
                                    .description("Name of the item.")))))
        .perform(MondayUpdateItemStatusAction::perform);

    private MondayUpdateItemStatusAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        String value = "{\"label\":\"%s\"}".formatted(inputParameters.getRequiredString(STATUS));

        String query =
            "mutation{change_column_value(board_id: %s, item_id: %s, column_id: \"%s\", value: \"%s\"){id name}}"
                .formatted(
                    inputParameters.getRequiredString(BOARD_ID), inputParameters.getRequiredString(ITEM_ID),
                    inputParameters.getRequiredString(COLUMN_ID), value.replace("\"", "\\\""));

        Map<String, Object> body = executeGraphQLQuery(query, context);

        return body.get(DATA);
    }
}
