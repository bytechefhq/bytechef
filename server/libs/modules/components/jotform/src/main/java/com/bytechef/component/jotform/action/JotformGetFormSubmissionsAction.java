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

package com.bytechef.component.jotform.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.jotform.util.JotformUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class JotformGetFormSubmissionsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getFormSubmissions")
        .title("Get Form Submissions")
        .description("Get all submissions for a specific form.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/form/{formId}/submissions"

            ))
        .properties(string("formId").label("Form ID")
            .description("ID of the form to retrieve submissions for.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) JotformUtils::getFormIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(
            outputSchema(object()
                .properties(array("content")
                    .items(object().properties(string("id").description("The ID of the submission.")
                        .required(false),
                        string("form_id").description("The ID of the form.")
                            .required(false),
                        string("status").description("The status of the submission.")
                            .required(false),
                        string("new").description("Is 1 if the submission is not read yet.")
                            .required(false),
                        string("notes").description("The notes of the submission.")
                            .required(false)))
                    .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON))));

    private JotformGetFormSubmissionsAction() {
    }
}
