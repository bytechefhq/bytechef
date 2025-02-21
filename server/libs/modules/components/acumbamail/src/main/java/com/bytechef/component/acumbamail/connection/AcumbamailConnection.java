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

package com.bytechef.component.acumbamail.connection;

import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.AUTH_TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class AcumbamailConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://acumbamail.com/api/1")
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .title("Authorization token")
                .properties(
                    string(AUTH_TOKEN)
                        .label("Access token")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofQueryParameters(
                    Map.of(AUTH_TOKEN, List.of(connectionParameters.getRequiredString(AUTH_TOKEN))))));

    private AcumbamailConnection() {
    }
}
