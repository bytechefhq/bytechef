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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DELETE_ITEM;
import static com.bytechef.component.monday.constant.MondayConstants.DELETE_ITEM_DESCRIPTION;
import static com.bytechef.component.monday.constant.MondayConstants.DELETE_ITEM_TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.ITEM_ID;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static com.bytechef.component.monday.util.MondayUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.monday.util.MondayOptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Kalaiyarasan Raja
 */
public class MondayDeleteItemAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(WORKSPACE_ID)
            .label("Workspace ID")
            .options((ActionOptionsFunction<String>) MondayOptionUtils::getWorkspaceIdOptions)
            .required(true),
        string(BOARD_ID)
            .label("Board ID")
            .description("ID of the board where the item is located.")
            .options((ActionOptionsFunction<String>) MondayOptionUtils::getBoardIdOptions)
            .optionsLookupDependsOn(WORKSPACE_ID)
            .required(true),
        string(ITEM_ID)
            .label("Item ID")
            .description("ID of the item to delete.")
            .options((ActionOptionsFunction<String>) MondayOptionUtils::getBoardItemsOptions)
            .optionsLookupDependsOn(BOARD_ID, WORKSPACE_ID)
            .required(true)
    };

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DELETE_ITEM)
        .title(DELETE_ITEM_TITLE)
        .description(DELETE_ITEM_DESCRIPTION)
        .properties(PROPERTIES)
        .perform(MondayDeleteItemAction::perform);

    private MondayDeleteItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = "mutation{delete_item(item_id: %s){id}}"
            .formatted(inputParameters.getRequiredString(ITEM_ID));

        executeGraphQLQuery(query, context);

        return null;
    }

}
