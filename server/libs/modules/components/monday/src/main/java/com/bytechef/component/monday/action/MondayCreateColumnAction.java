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
import static com.bytechef.component.monday.constant.MondayConstants.COLUMN_TYPE;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayOptionUtils.getColumnTypeOptions;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayOptionUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MondayCreateColumnAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createColumn")
        .title("Create Column")
        .description("Create a new column in board.")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .options((OptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(false),
            string(BOARD_ID)
                .label("Board ID")
                .description("Id of the board where the new column should be created.")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The new column's title.")
                .required(true),
            string(COLUMN_TYPE)
                .label("Column Type")
                .description("The type of column to create.")
                .options(getColumnTypeOptions())
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("create_column")
                            .properties(
                                string(ID)
                                    .description("ID of the column."),
                                string(TITLE)
                                    .description("Title of the column.")))))
        .perform(MondayCreateColumnAction::perform);

    private MondayCreateColumnAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = "mutation{create_column(board_id: %s, title: \"%s\", column_type: %s){id title}}"
            .formatted(inputParameters.getRequiredString(BOARD_ID), inputParameters.getRequiredString(TITLE),
                inputParameters.getRequiredString(COLUMN_TYPE));

        Map<String, Object> body = executeGraphQLQuery(query, context);

        return body.get(DATA);
    }
}
