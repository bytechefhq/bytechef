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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.gaurus.property.GaurusServiceResponseProperties;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GaurusGetExternalUsersAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getExternalUsers")
        .title("Gets external users for provided client id.")
        .description(null)
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/external-users"

            ))
        .properties()
        .output(outputSchema(object().properties(GaurusServiceResponseProperties.PROPERTIES)
            .description("Standard response wrapper for all API endpoints.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))),
            sampleOutput(
                Map.<String, Object>ofEntries(
                    Map.entry("data",
                        List.of(
                            Map.<String, Object>ofEntries(Map.entry("id", 42), Map.entry("name", "Pero Perić d.o.o."),
                                Map.entry("oib", 1.2345678901E10), Map.entry("mail", "pero@example.com"),
                                Map.entry("bankEntries",
                                    List.of(Map.<String, Object>ofEntries(Map.entry("bankSlug", "erste"),
                                        Map.entry("ibans", List.of("HR1210010051863000160")),
                                        Map.entry("consentJobStatus", "PENDING"))))))),
                    Map.entry("errors", List.of()))));

    private GaurusGetExternalUsersAction() {
    }
}
