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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveDeleteLeadAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("deleteLead")
        .title("Delete a lead")
        .description("Deletes a specific lead.")
        .metadata(
            Map.of(
                "method", "DELETE",
                "path", "/leads/{id}"

            ))
        .properties(string("id").label("Id")
            .description("The ID of the lead")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object()
            .properties(bool("success").required(false), object("data").properties(string("id").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)),
            Map.<String, Object>ofEntries(Map.entry("success", true),
                Map.entry("data",
                    Map.<String, Object>ofEntries(Map.entry("id", "adf21080-0e10-11eb-879b-05d71fb426ec")))));
}
