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

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.AuthorizationType.BASIC_AUTH;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.twilio.constant.TwilioConstants.ACCOUNT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.AUTH_TOKEN;

import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Luka Ljubić
 */
public class TwilioConnection {

    private TwilioConnection() {
    }

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.twilio.com/2010-04-01/Accounts/"
            + connectionParameters.getRequiredString(ACCOUNT_SID))
        .authorizations(
            authorization(BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(ACCOUNT_SID)
                        .label("Account SID")
                        .required(true),
                    string(AUTH_TOKEN)
                        .label("Auth Token")
                        .required(true))
                .apply((connectionParameters, context) -> ofHeaders(
                    Map.of(AUTHORIZATION,
                        List.of("Basic " + connectionParameters.getRequiredString(ACCOUNT_SID)
                            + ":" + connectionParameters.getRequiredString(AUTH_TOKEN))))));
}
