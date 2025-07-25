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

package com.bytechef.component.pagerduty.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pagerduty.constant.PagerDutyConstants.API_KEY;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class PagerDutyConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.pagerduty.com")
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(API_KEY)
                        .label("API Key")
                        .description("Enter your API Key. Can be found in Integrations -> API Access Keys.")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        AUTHORIZATION, List.of("Token token=" + connectionParameters.getRequiredString(API_KEY))))));

    private PagerDutyConnection() {
    }
}
