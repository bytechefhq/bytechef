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

package com.bytechef.component.twilio.connection;

import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.PASSWORD;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.USERNAME;

import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDSL;

/**
 * @author Monika Domiter
 */
public class TwilioConnection {

    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(authorization(
            Authorization.AuthorizationType.BASIC_AUTH.name()
                .toLowerCase(),
            Authorization.AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)));

    private TwilioConnection() {
    }
}
