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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.COLUMNS;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayOptionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marija Horvat
 */
public class MondayGetBoardValuesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getBoardValues")
        .title("Get Board Values")
        .description("Get a list of board's items.")
        .help("", "https://docs.bytechef.io/reference/components/monday_v1#get-board-values")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .description("ID of the workspace where the board is located.")
                .options((OptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(false),
            string(BOARD_ID)
                .label("Board ID")
                .description("ID of the board to return values for.")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true),
            array(COLUMNS)
                .label("Columns")
                .description("Select specific columns to return values for. If empty, all columns will be returned.")
                .items(string())
                .options((OptionsFunction<String>) MondayOptionUtils::getColumnIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID, BOARD_ID)
                .required(false))
        .output()
        .perform(MondayGetBoardValuesAction::perform);

    private MondayGetBoardValuesAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<String> columns = inputParameters.getList(COLUMNS, String.class);

        String columnFilter = "";

        if (columns != null && !columns.isEmpty()) {
            String formattedColumns = columns.stream()
                .map(col -> "\"" + col + "\"")
                .collect(Collectors.joining(","));

            columnFilter = "(ids: [" + formattedColumns + "])";
        }

        String query = ("query{boards(ids: %s){items_page{items{id name group{id title}column_values %s{text value " +
            "column{id title}}}}}}").formatted(inputParameters.getRequiredString(BOARD_ID), columnFilter);

        Map<String, Object> body = executeGraphQLQuery(query, context);

        return body.get(DATA);
    }
}
