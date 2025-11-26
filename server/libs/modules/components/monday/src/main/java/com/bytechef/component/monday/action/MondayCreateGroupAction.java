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
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.GROUP_NAME;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
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
public class MondayCreateGroupAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createGroup")
        .title("Create Group")
        .description("Creates a new group in board.")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .options((OptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
                .required(false),
            string(BOARD_ID)
                .label("Board ID")
                .description("Id of the board where new item will be created.")
                .options((OptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
                .optionsLookupDependsOn(WORKSPACE_ID)
                .required(true),
            string(GROUP_NAME)
                .label("Group Name")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("create_group")
                            .properties(
                                string(ID)
                                    .description("ID of the group."),
                                string(NAME)
                                    .description("Name of the group.")))))
        .perform(MondayCreateGroupAction::perform);

    private MondayCreateGroupAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = "mutation{create_group(board_id: %s, group_name: \"%s\"){id title}}"
            .formatted(inputParameters.getRequiredString(BOARD_ID), inputParameters.getRequiredString(GROUP_NAME));

        Map<String, Object> body = executeGraphQLQuery(query, context);

        return body.get(DATA);
    }
}
