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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveGetLeadDetailsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getLeadDetails")
        .title("Get Lead Details")
        .description("Returns details of a specific lead. ")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/leads/{lead_id}"

            ))
        .properties(string("lead_id").label("Lead ID")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) PipedriveUtils::getLeadIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("data")
                .properties(string("id").required(false), string("title").required(false),
                    integer("owner_id").required(false),
                    object("value").properties(integer("amount").required(false), string("currency").required(false))
                        .required(false),
                    date("expected_close_date").required(false), integer("person_id").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipedriveGetLeadDetailsAction() {
    }
}
