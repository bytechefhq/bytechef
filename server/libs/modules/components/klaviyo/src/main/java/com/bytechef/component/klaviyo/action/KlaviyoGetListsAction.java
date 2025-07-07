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

package com.bytechef.component.klaviyo.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LISTS_OUTPUT_PROPERTY;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LIST_FIELDS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Marija Horvat
 */
public class KlaviyoGetListsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getLists")
        .title("Get Lists")
        .description("Get all lists in an account.")
        .properties(
            array(LIST_FIELDS)
                .label("List Fields")
                .description("List of fields to include for each related list object.")
                .items(string())
                .options(
                    List.of(
                        option("Name", "name"), option("Created", "created"),
                        option("Updated", "updated"), option("Opt In Process", "opt_in_process")))
                .required(true))
        .output(outputSchema(LISTS_OUTPUT_PROPERTY))
        .perform(KlaviyoGetListsAction::perform);

    private KlaviyoGetListsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/api/lists"))
            .queryParameter("fields[list]", String.join(",", inputParameters.getList(LIST_FIELDS, String.class)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
