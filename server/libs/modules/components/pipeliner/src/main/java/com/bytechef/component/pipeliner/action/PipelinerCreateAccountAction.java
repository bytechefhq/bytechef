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

package com.bytechef.component.pipeliner.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.pipeliner.util.PipelinerUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipelinerCreateAccountAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createAccount")
        .title("Create Account")
        .description("Creates new account.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/entities/Accounts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("owner_id").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Owner ID")
            .description(
                "Id of the user in Pipeliner Application that will become the owner of the newly created account.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) PipelinerUtils::getOwnerIdOptions),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("Account name")
                .required(true))
        .output(outputSchema(object()
            .properties(bool("success").description("True when response succeeded, false on error.")
                .required(false),
                object("data").properties(string("id").description("ID of the account.")
                    .required(false),
                    string("owner_id")
                        .description("ID of the user in Pipeliner Application that is the owner of the account.")
                        .required(false),
                    string("name").description("Account name.")
                        .required(false))
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipelinerCreateAccountAction() {
    }
}
