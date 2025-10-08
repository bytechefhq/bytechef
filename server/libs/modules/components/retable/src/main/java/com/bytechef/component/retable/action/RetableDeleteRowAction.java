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

package com.bytechef.component.retable.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.retable.constant.RetableConstants.PROJECT_ID;
import static com.bytechef.component.retable.constant.RetableConstants.RETABLE_ID;
import static com.bytechef.component.retable.constant.RetableConstants.ROWS_IDS;
import static com.bytechef.component.retable.constant.RetableConstants.WORKSPACE_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.retable.util.RetableUtils;

/**
 * @author Marija Horvat
 */
public class RetableDeleteRowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRow")
        .title("Delete Row")
        .description("Delete a row.")
        .properties(
            string(WORKSPACE_ID)
                .label("Workspace ID")
                .description("ID of the workspace.")
                .options((OptionsFunction<String>) RetableUtils::getWorkspaceIdOptions)
                .required(true),
            string(PROJECT_ID)
                .label("Project ID")
                .description("ID of the project.")
                .optionsLookupDependsOn(WORKSPACE_ID)
                .options((OptionsFunction<String>) RetableUtils::getProjectIdOptions)
                .required(true),
            string(RETABLE_ID)
                .label("Retable ID")
                .description("ID of the retable.")
                .optionsLookupDependsOn(PROJECT_ID)
                .options((OptionsFunction<String>) RetableUtils::getRetableIdOptions)
                .required(true),
            array(ROWS_IDS)
                .label("Rows IDs")
                .description("ID of the rows to delete.")
                .required(true)
                .items(integer()))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(
                                integer("deleted_row_count")
                                    .description("Number of rows deleted.")))))
        .perform(RetableDeleteRowAction::perform);

    private RetableDeleteRowAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.delete("/retable/" + inputParameters.getRequiredString(RETABLE_ID) + "/data"))
            .configuration(responseType(ResponseType.JSON))
            .body(Body.of(ROWS_IDS, inputParameters.getList(ROWS_IDS)))
            .execute()
            .getBody();
    }
}
