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

package com.bytechef.component.scrapingbee.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeGetUsageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getUsage")
        .title("Get Usage")
        .description(
            "Get the API credit consumption and concurrency usage of the ScrapingBee account. Limited to 6 calls " +
                "per minute.")
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("max_api_credit")
                            .description("Total API credits available in the current billing period."),
                        integer("used_api_credit")
                            .description("API credits used in the current billing period."),
                        integer("max_concurrency")
                            .description("Maximum number of concurrent requests allowed."),
                        integer("current_concurrency")
                            .description("Number of requests currently running."),
                        string("renewal_subscription_date")
                            .description("Date the subscription renews."))))
        .help("", "https://docs.bytechef.io/reference/components/scrapingbee_v1#get-usage")
        .perform(ScrapingBeeGetUsageAction::perform);

    private ScrapingBeeGetUsageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.get("/usage"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
