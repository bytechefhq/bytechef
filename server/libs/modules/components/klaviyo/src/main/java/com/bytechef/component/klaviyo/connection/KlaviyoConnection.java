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

package com.bytechef.component.klaviyo.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class KlaviyoConnection {

    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://a.klaviyo.com")
        .authorizations(
            authorization(Authorization.AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(KEY)
                        .label("Key")
                        .required(true))
                .apply((connectionParameters, context) -> Authorization.ApplyResponse.ofHeaders(
                    Map.of(AUTHORIZATION, List.of("Klaviyo-API-Key " + connectionParameters.getRequiredString(KEY))))));

    private KlaviyoConnection() {
    }
}
