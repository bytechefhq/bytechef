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

package com.bytechef.component.viber.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.viber.constant.ViberConstants.AUTH_TOKEN;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.viber.util.ViberUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ViberConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .help("", "https://docs.bytechef.io/reference/components/viber_v1#connection-setup")
        .baseUri((connectionParameters, context) -> "https://chatapi.viber.com/pa")
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(AUTH_TOKEN)
                        .label("Auth Token")
                        .required(true))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of("X-Viber-Auth-Token", List.of(connectionParameters.getRequiredString(AUTH_TOKEN))))))
        .test((connectionParameters, context) -> ViberUtils.getAccountInfo(context))
        .version(1);

    private ViberConnection() {
    }
}
