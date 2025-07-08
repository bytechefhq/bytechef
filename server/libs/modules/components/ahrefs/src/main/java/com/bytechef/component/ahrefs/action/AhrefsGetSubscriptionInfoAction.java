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

package com.bytechef.component.ahrefs.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AhrefsGetSubscriptionInfoAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getSubscriptionInfo")
        .title("Get Subscription Information")
        .description("Returns user subscription information.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/subscription-info/limits-and-usages"

            ))
        .properties()
        .output(outputSchema(object()
            .properties(object("limits_and_usage")
                .properties(string("subscription").description("Ahrefs subscription plan.")
                    .required(false),
                    string("usage_reset_date")
                        .description("Start date of the next billing period when the API units usage will be reset.")
                        .required(false),
                    integer("units_limit_workspace")
                        .description("Total number of API units available to the workspace.")
                        .required(false),
                    integer("units_usage_workspace")
                        .description("Number of API units consumed by the workspace in the current billing month.")
                        .required(false),
                    integer("units_limit_api_key").description(
                        "Limit for the number of API units that can be consumed via this API key per billing month (null = unlimited).")
                        .required(false),
                    integer("units_usage_api_key")
                        .description("Number of API units consumed by this API key in the current billing month.")
                        .required(false),
                    string("api_key_expiration_date")
                        .description("Date on which this API key will expire and stop working.")
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AhrefsGetSubscriptionInfoAction() {
    }
}
