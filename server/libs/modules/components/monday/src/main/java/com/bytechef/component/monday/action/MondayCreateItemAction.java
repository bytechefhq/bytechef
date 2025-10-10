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
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.GROUP_ID;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.ITEM_NAME;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayPropertiesUtils.convertPropertyToMondayColumnValue;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayOptionUtils;
import com.bytechef.component.monday.util.MondayPropertiesUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MondayCreateItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createItem")
        .title("Create Item")
        .description("Create a new item in a board.")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .options((OptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(true),
            string(BOARD_ID)
                .label("Board ID")
                .description("ID of the board where new item will be created.")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true),
            string(GROUP_ID)
                .label("Group ID")
                .description("The item's group.")
                .options((OptionsFunction<String>) MondayOptionUtils::getGroupIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID, BOARD_ID)
                .required(false),
            string(ITEM_NAME)
                .label("Item Name")
                .description("The item's name.")
                .required(true),
            dynamicProperties("columnValues")
                .properties(MondayPropertiesUtils::createPropertiesForItem)
                .propertiesLookupDependsOn(WORKSPACE_ID, BOARD_ID, GROUP_ID)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("create_item")
                            .properties(
                                string(ID)
                                    .description("ID of the item."),
                                string(NAME)
                                    .description("Name of the item.")))))
        .perform(MondayCreateItemAction::perform);

    private MondayCreateItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> mondayColumnValues = convertPropertyToMondayColumnValue(
            inputParameters.getMap("columnValues"), inputParameters.getRequiredString(BOARD_ID), context);

        String jsonMondayColumnValues = context.json(json -> json.write(mondayColumnValues));
        String query =
            "mutation{create_item(board_id: %s, group_id: \"%s\", item_name: \"%s\", column_values:\"%s\"){id name}}"
                .formatted(
                    inputParameters.getRequiredString(BOARD_ID), inputParameters.getRequiredString(GROUP_ID),
                    inputParameters.getRequiredString(ITEM_NAME),
                    jsonMondayColumnValues.replace("\"", "\\\""));

        Map<String, Object> body = executeGraphQLQuery(query, context);

        return body.get(DATA);
    }

}
