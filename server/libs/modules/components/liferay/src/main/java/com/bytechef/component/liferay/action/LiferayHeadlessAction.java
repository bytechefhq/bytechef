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

package com.bytechef.component.liferay.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.time.Duration;

/**
 * @author Igor Beslic
 */
public class LiferayHeadlessAction {

    public static final String ENDPOINT = "endpoint";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("headless")
        .title("Headless Api")
        .description("The Headless endpoint to use.")
        .properties(
            string(ENDPOINT)
                .label("Endpoint")
                .required(true)
                .placeholder("headless-portal/v1.0)"))
        .output()
        .perform(LiferayHeadlessAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String baseUri = connectionParameters.getRequiredString(BASE_URI);
        String endpoint = inputParameters.getRequiredString(ENDPOINT);

        String endpointUri = baseUri + "/" + endpoint;

        Http.Response response = context.http(http -> http.get(endpointUri))
            .configuration(Http.timeout(Duration.ofMillis(inputParameters.getInteger("timeout", 10000))))
            .execute();

        int statusCode = response.getStatusCode();

        if ((statusCode >= 200) && (statusCode < 300)) {
            return response.getBody();
        }

        context.log(log -> log.warn("Received response code {}, from endpoint {}", statusCode, endpointUri));

        return endpoint;
    }

}
