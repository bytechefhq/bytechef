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

package com.bytechef.component.gaurus.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.gaurus.property.GaurusExternalUserRequestProperties;
import com.bytechef.component.gaurus.property.GaurusServiceResponseProperties;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GaurusPostExternalUsersAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("postExternalUsers")
        .title("Creates external users with bank consent jobs.")
        .description(null)
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/external-users", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(array("__items").items(object().properties(GaurusExternalUserRequestProperties.PROPERTIES))
            .placeholder("Add to Items")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("External Users")
            .required(true))
        .output(outputSchema(object().properties(GaurusServiceResponseProperties.PROPERTIES)
            .description("Standard response wrapper for all API endpoints.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))),
            sampleOutput(Map.<String, Object>ofEntries(Map.entry("data",
                List.of(Map.<String, Object>ofEntries(Map.entry("existingMails", List.of("existing@example.com")),
                    Map.entry("newMails", List.of("new@example.com"))))),
                Map.entry("errors", List.of()))));

    private GaurusPostExternalUsersAction() {
    }
}
