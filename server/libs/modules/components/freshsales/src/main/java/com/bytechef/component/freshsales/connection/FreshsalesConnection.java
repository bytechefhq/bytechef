/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.freshsales.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class FreshsalesConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(
                AuthorizationType.API_KEY.toLowerCase(), AuthorizationType.API_KEY)
                    .title("API Key")
                    .properties(
                        string(USERNAME)
                            .label("Bundle alias")
                            .description("Your Freshsales bundle alias (e.g. https://<alias>.myfreshworks.com)")
                            .required(true),
                        string(KEY)
                            .label("API Key")
                            .description("The API Key supplied by Freshsales")
                            .required(true))
                    .apply((connectionParameters, context) -> ofHeaders(
                        Map.of(AUTHORIZATION, List.of("Token token=" + connectionParameters.getRequiredString(KEY))))));

    private FreshsalesConnection() {
    }
}
