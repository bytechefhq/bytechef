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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
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
public class PipedriveGetPersonDetailsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getPersonDetails")
        .title("Get Details of Person")
        .description(
            "Returns the details of a person. This also returns some additional fields which are not present when asking for all persons.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/persons/{person_id}"

            ))
        .properties(integer("person_id").label("Person ID")
            .description("ID of the person to get details.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<Long>) PipedriveUtils::getPersonIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object()
            .properties(object("data")
                .properties(integer("id").required(false), integer("company_id").required(false),
                    object("owner_id")
                        .properties(integer("id").required(false), string("name").required(false),
                            string("email").required(false))
                        .required(false),
                    object("org_id")
                        .properties(string("name").required(false), integer("owner_id").required(false),
                            string("cc_email").required(false))
                        .required(false),
                    string("name").required(false),
                    array("phone")
                        .items(object().properties(string("value").required(false), bool("primary").required(false),
                            string("label").required(false)))
                        .required(false),
                    array("email")
                        .items(object().properties(string("value").required(false), bool("primary").required(false),
                            string("label").required(false)))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipedriveGetPersonDetailsAction() {
    }
}
