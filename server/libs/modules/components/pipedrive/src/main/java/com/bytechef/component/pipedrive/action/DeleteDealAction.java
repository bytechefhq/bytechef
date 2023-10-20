
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class DeleteDealAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("deleteDeal")
        .display(
            display("Delete a deal")
                .description("Marks a deal as deleted. After 30 days, the deal will be permanently deleted."))
        .metadata(
            Map.of(
                "requestMethod", "DELETE",
                "path", "/deals/{id}"

            ))
        .properties(integer("id").label("Id")
            .description("The ID of the deal")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object(null).properties(bool("success").label("Success")
            .description("If the request was successful or not")
            .required(false),
            object("data").properties(integer("id").label("Id")
                .description("The ID of the deal that was deleted")
                .required(false))
                .label("Data")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput("{\"success\":true,\"data\":{\"id\":123}}");
}
