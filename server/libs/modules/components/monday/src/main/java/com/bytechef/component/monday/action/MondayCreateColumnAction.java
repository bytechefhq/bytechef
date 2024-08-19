/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.COLUMN_TYPE;
import static com.bytechef.component.monday.constant.MondayConstants.CREATE_COLUMN;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayOptionUtils.getColumnTypeOptions;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayOptionUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MondayCreateColumnAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_COLUMN)
        .title("Create column")
        .description("Create a new column in board.")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace")
                .options((ActionOptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(true),
            string(BOARD_ID)
                .label("Board")
                .description("The board where the new column should be created.")
                .options((ActionOptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("The new column's title.")
                .required(true),
            string(COLUMN_TYPE)
                .label("Column type")
                .description("The type of column to create.")
                .options(getColumnTypeOptions())
                .required(true))
        .outputSchema(
            object()
                .properties(
                    object("create_column")
                        .properties(
                            string(ID),
                            string(TITLE))))
        .perform(MondayCreateColumnAction::perform);

    private MondayCreateColumnAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String query = "mutation{create_column(board_id: %s, title: \"%s\", column_type: %s){id title}}"
            .formatted(inputParameters.getRequiredString(BOARD_ID), inputParameters.getRequiredString(TITLE),
                inputParameters.getRequiredString(COLUMN_TYPE));

        Map<String, Object> body = executeGraphQLQuery(actionContext, query);

        return body.get(DATA);
    }
}
