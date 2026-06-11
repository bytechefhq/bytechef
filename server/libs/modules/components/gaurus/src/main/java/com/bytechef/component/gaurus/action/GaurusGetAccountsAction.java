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
import com.bytechef.component.gaurus.property.GaurusMyResponseProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GaurusGetAccountsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getAccounts")
        .title("Gets accounts for provided client id.")
        .description(null)
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/accounts"

            ))
        .properties()
        .output(outputSchema(object().properties(GaurusMyResponseProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))),
            sampleOutput(Map.<String, Object>ofEntries(Map.entry("code", "OK"), Map.entry("message", ""),
                Map.entry("hasMoreResults", false), Map.entry("data", "AccountResult[]"))));

    private GaurusGetAccountsAction() {
    }
}
