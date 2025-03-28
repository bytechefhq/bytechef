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

package com.bytechef.component.baserow.action;

import static com.bytechef.component.baserow.constant.BaserowConstants.DELETE_ROW;
import static com.bytechef.component.baserow.constant.BaserowConstants.DELETE_ROW_DESCRIPTION;
import static com.bytechef.component.baserow.constant.BaserowConstants.DELETE_ROW_TITLE;
import static com.bytechef.component.baserow.constant.BaserowConstants.ROW_ID;
import static com.bytechef.component.baserow.constant.BaserowConstants.TABLE_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Arina Kolodeznikova
 */
public class BaserowDeleteRowAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        integer(TABLE_ID)
            .label("Table ID")
            .description("ID of the table containing the row to be deleted.")
            .required(true),
        integer(ROW_ID)
            .label("Row ID")
            .description("ID of the row to be deleted.")
            .required(true)
    };

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DELETE_ROW)
        .title(DELETE_ROW_TITLE)
        .description(DELETE_ROW_DESCRIPTION)
        .properties(PROPERTIES)
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
