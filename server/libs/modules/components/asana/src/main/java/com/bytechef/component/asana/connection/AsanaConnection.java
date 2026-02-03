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

package com.bytechef.component.asana.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class AsanaConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://app.asana.com/api/1.0")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://app.asana.com/-/oauth_authorize")
            .scopes((connectionParameters, context) -> {
                Map<String, Boolean> scopeMap = new HashMap<>();

                scopeMap.put("attachments:delete", false);
                scopeMap.put("attachments:read", false);
                scopeMap.put("attachments:write", false);
                scopeMap.put("custom_fields:read", false);
                scopeMap.put("custom_fields:write", false);
                scopeMap.put("goals:read", false);
                scopeMap.put("portfolios:read", false);
                scopeMap.put("portfolios:write", false);
                scopeMap.put("project_templates:read", false);
                scopeMap.put("projects:delete", false);
                scopeMap.put("projects:read", true);
                scopeMap.put("projects:write", true);
                scopeMap.put("stories:read", false);
                scopeMap.put("stories:write", false);
                scopeMap.put("tags:read", true);
                scopeMap.put("tags:write", false);
                scopeMap.put("task_templates:read", false);
                scopeMap.put("tasks:delete", false);
                scopeMap.put("tasks:read", false);
                scopeMap.put("tasks:write", true);
                scopeMap.put("team_memberships:read", false);
                scopeMap.put("teams:read", true);
                scopeMap.put("time_tracking_entries:read", false);
                scopeMap.put("timesheet_approval_statuses:read", false);
                scopeMap.put("timesheet_approval_statuses:write", false);
                scopeMap.put("workspace.typehead:read", false);
                scopeMap.put("users:read", true);
                scopeMap.put("webhooks:delete", false);
                scopeMap.put("webhooks:read", false);
                scopeMap.put("webhooks:write", false);
                scopeMap.put("workspaces:read", true);
                scopeMap.put("default", false);
                scopeMap.put("openid", false);
                scopeMap.put("email", false);
                scopeMap.put("profile", false);

                return scopeMap;
            })
            .tokenUrl((connectionParameters, context) -> "https://app.asana.com/-/oauth_token")
            .refreshUrl((connectionParameters, context) -> "https://app.asana.com/-/oauth_token"));

    private AsanaConnection() {
    }
}
