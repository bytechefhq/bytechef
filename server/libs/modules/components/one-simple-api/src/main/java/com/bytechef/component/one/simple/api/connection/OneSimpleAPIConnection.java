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

package com.bytechef.component.one.simple.api.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TOKEN;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;

/**
 * @author Luka Ljubić
 */
public class OneSimpleAPIConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://onesimpleapi.com/api")
        .authorizations(
            authorization(CUSTOM)
                .title("One Simple API Connection")
                .properties(
                    string(TOKEN)
                        .label("Access Token")
                        .description("Access Token that is given to you when you create a API Token in OneSimpleApi")
                        .required(true))
                .scopes((connection, context) -> List.of(
                    "shortener",
                    "exchange_rate",
                    "page_info")));

    private OneSimpleAPIConnection() {
    }
}
