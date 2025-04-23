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

package com.bytechef.component.rocketchat.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.AUTH_TOKEN;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.DOMAIN;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.USER_ID;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

public class RocketchatConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(DOMAIN)
            + ".rocket.chat/api/v1")
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .properties(
                    string(DOMAIN)
                        .label("Domain")
                        .required(true),
                    string(AUTH_TOKEN)
                        .label("Auth Token")
                        .required(true),
                    string(USER_ID)
                        .label("User ID")
                        .required(true))
                .apply((connectionParameters, context) -> Authorization.ApplyResponse.ofHeaders(
                    Map.of(AUTH_TOKEN, List.of(connectionParameters.getRequiredString(AUTH_TOKEN)),
                        USER_ID, List.of(connectionParameters.getRequiredString(USER_ID))))));

    private RocketchatConnection() {
    }
}
