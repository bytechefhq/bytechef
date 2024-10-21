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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_KIND;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_NAME;
import static com.bytechef.component.monday.constant.MondayConstants.CREATE_BOARD;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.DESCRIPTION;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Kalaiyarasan Raja
 */
public class MondayCreateBoardAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_BOARD)
        .title("Create board")
        .description("Create a new board.")
        .properties(
            string(BOARD_NAME)
                .label("Board Name")
                .description("Name of the new board.")
                .required(true),
            string(BOARD_KIND)
                .label("Board Kind")
                .description("The type of board to create.")
                .options(
                    option("Private", "private"),
                    option("Public", "public"),
                    option("Share", "share"))
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Detailed description of the new board.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("create_board")
                            .properties(
                                string(ID),
                                string(BOARD_NAME)))))
        .perform(MondayCreateBoardAction::perform);

    private MondayCreateBoardAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String query = "mutation{create_board(board_name: \"%s\", description: \"%s\", board_kind: %s){id board_name}}"
            .formatted(inputParameters.getRequiredString(BOARD_NAME), inputParameters.getString(DESCRIPTION, ""),
                inputParameters.getRequiredString(BOARD_KIND));

        Map<String, Object> body = executeGraphQLQuery(query, actionContext);

        return body.get(DATA);
    }

}
