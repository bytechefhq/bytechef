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

package com.bytechef.component.mautic.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mautic.constant.MauticConstants.BASE_URL;
import static com.bytechef.component.mautic.constant.MauticConstants.PASSWORD;
import static com.bytechef.component.mautic.constant.MauticConstants.USERNAME;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Nikolina Spehar
 */
public class MauticConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getRequiredString(BASE_URL)
            .endsWith("/") ? "api" : "/api")
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Mautic Basic Auth")
                .description(
                    "Support for basic authentication can be enabled through Mautic’s Configuration -> API Settings.")
                .properties(
                    string(BASE_URL)
                        .label("Base URL")
                        .description(
                            "Open your Mautic instance and copy the URL from the address bar. If your dashboard link is"
                                +
                                " \"https://mautic.ddev.site/s/dashboard\", set your base URL as \"https://mautic.ddev.site/\".")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)));

    private MauticConnection() {
    }
}
