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

package com.bytechef.component.jira.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class JiraConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((parameters, context) -> "https://auth.atlassian.com/authorize")
                .tokenUrl((parameters, context) -> "https://auth.atlassian.com/oauth/token")
                .refreshUrl((parameters, context) -> "https://auth.atlassian.com/oauth/token")
                .scopes((connection, context) -> List.of(
                    "manage:jira-webhook", "read:jira-work", "write:jira-work", "read:jira-user", "offline_access")))
        .baseUri((connectionParameters, context) -> getBaseUrl(context));

    private JiraConnection() {
    }

    private static String getBaseUrl(Context context) {
        List<?> body = context
            .http(http -> http.get("https://api.atlassian.com/oauth/token/accessible-resources"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Object object = body.getFirst();

        if (object instanceof Map<?, ?> map) {
            return "https://api.atlassian.com/ex/jira/" + map.get(ID) + "/rest/api/3";
        }

        return null;
    }
}
