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

package com.bytechef.component.baserow.action;

import static com.bytechef.component.baserow.constant.BaserowConstants.ROW_ID;
import static com.bytechef.component.baserow.constant.BaserowConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Arina Kolodeznikova
 */
public class BaserowDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRow")
        .title("Delete Row")
        .description("Deletes the specified row.")
        .help("", "https://docs.bytechef.io/reference/components/baserow_v1#delete-row")
        .properties(
            integer(TABLE_ID)
                .label("Table ID")
                .description("ID of the table containing the row to be deleted.")
                .required(true),
            integer(ROW_ID)
                .label("Row ID")
                .description("ID of the row to be deleted.")
                .required(true))
        .perform(BaserowDeleteRowAction::perform);

    private BaserowDeleteRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.delete(
            "/database/rows/table/%s/%s/"
                .formatted(inputParameters.getRequiredString(TABLE_ID), inputParameters.getRequiredString(ROW_ID))))
            .execute();

        return null;
    }
}
