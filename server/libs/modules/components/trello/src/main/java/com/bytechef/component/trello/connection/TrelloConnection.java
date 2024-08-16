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

package com.bytechef.component.trello.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TrelloConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.trello.com/1")
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(KEY)
                        .label("Key")
                        .required(true),
                    string(TOKEN)
                        .label("Token")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofQueryParameters(
                    Map.of(
                        KEY, List.of(connectionParameters.getRequiredString(KEY)),
                        TOKEN, List.of(connectionParameters.getRequiredString(TOKEN))))));

    private TrelloConnection() {
    }
}
