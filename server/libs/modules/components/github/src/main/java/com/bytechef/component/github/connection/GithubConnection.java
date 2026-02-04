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

package com.bytechef.component.github.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Luka Ljubić
 */
public class GithubConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.github.com")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://github.com/login/oauth/authorize")
                .tokenUrl((connection, context) -> "https://github.com/login/oauth/access_token")
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("repo", true);
                    map.put("repo:status", false);
                    map.put("repo_deployment", false);
                    map.put("public_repo", false);
                    map.put("repo:invite", false);
                    map.put("security_events", false);
                    map.put("admin:repo_hook", true);
                    map.put("write:repo_hook", false);
                    map.put("read:repo_hook", false);
                    map.put("admin:org", true);
                    map.put("write:org", false);
                    map.put("read:org", true);
                    map.put("admin:public_key", false);
                    map.put("write:public_key", false);
                    map.put("read:public_key", false);
                    map.put("admin:org_hook", false);
                    map.put("gist", false);
                    map.put("notifications", false);
                    map.put("user", false);
                    map.put("read:user", false);
                    map.put("user:email", false);
                    map.put("user:follow", false);
                    map.put("project", false);
                    map.put("read:project", false);
                    map.put("delete_repo", false);
                    map.put("write:packages", false);
                    map.put("read:packages", false);
                    map.put("delete:packages", false);
                    map.put("admin:gpg_key", false);
                    map.put("write:gpg_key", false);
                    map.put("read:gpg_key", false);
                    map.put("codespace", false);
                    map.put("workflow", false);
                    map.put("read:audit_log", false);

                    return map;
                }));

    private GithubConnection() {
    }
}
