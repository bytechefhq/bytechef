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

package com.bytechef.component.vtiger.connection;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.vtiger.constant.VTigerConstants.INSTANCE_URL;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Luka Ljubić
 */
public class VTigerConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getRequiredString(INSTANCE_URL)
            + "/restapi/v1/vtiger/default")
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .description("Enter your username/email.")
                        .required(true),
                    string(PASSWORD)
                        .label("Access Key")
                        .required(true),
                    string(INSTANCE_URL)
                        .label("VTiger Instance URL")
                        .description("For the instance URL, add the url without the endpoint.")
                        .exampleValue("https://<instance>.od2.vtiger.com")
                        .required(true)));

    private VTigerConnection() {
    }
}
