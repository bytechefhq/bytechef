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

package com.bytechef.component.slack.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class SlackConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://slack.com/api")
        .authorizations(
            authorization(OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://slack.com/oauth/authorize")
                .scopes((connection, context) -> {
                    Map<String, Boolean> scopeMap = new HashMap<>();

                    scopeMap.put("admin", false);
                    scopeMap.put("admin.analytics:read", false);
                    scopeMap.put("admin.app_activities:read", false);
                    scopeMap.put("admin.apps:read", false);
                    scopeMap.put("admin.apps:write", false);
                    scopeMap.put("admin.barriers:read", false);
                    scopeMap.put("admin.barriers:write", false);
                    scopeMap.put("admin.chat:read", false);
                    scopeMap.put("admin.chat:write", false);
                    scopeMap.put("admin.conversations:manage_objects", false);
                    scopeMap.put("admin.conversations:read", false);
                    scopeMap.put("admin.conversations:write", false);
                    scopeMap.put("admin.invites:read", false);
                    scopeMap.put("admin.invites:write", false);
                    scopeMap.put("admin.roles:read", false);
                    scopeMap.put("admin.roles:write", false);
                    scopeMap.put("admin.teams:read", false);
                    scopeMap.put("admin.teams:write", false);
                    scopeMap.put("admin.usergroups:read", false);
                    scopeMap.put("admin.usergroups:write", false);
                    scopeMap.put("admin.users:read", false);
                    scopeMap.put("admin.users:write", false);
                    scopeMap.put("admin.workflows:read", false);
                    scopeMap.put("admin.workflows:write", false);
                    scopeMap.put("app_configurations:read", false);
                    scopeMap.put("app_configurations:write", false);
                    scopeMap.put("app_mentions:read", false);
                    scopeMap.put("apps.requests:write", false);
                    scopeMap.put("assistant:write", false);
                    scopeMap.put("auditlogs:read", false);
                    scopeMap.put("authorizations:read", false);
                    scopeMap.put("bookmarks:read", false);
                    scopeMap.put("bookmarks:write", false);
                    scopeMap.put("bot", false);
                    scopeMap.put("calls:read", false);
                    scopeMap.put("calls:write", false);
                    scopeMap.put("canvases:read", false);
                    scopeMap.put("canvases:write", false);
                    scopeMap.put("channels:history", true);
                    scopeMap.put("channels:join", false);
                    scopeMap.put("channels:manage", false);
                    scopeMap.put("channels:read", true);
                    scopeMap.put("channels:write", true);
                    scopeMap.put("channels:write.invites", false);
                    scopeMap.put("channels:write.topic", false);
                    scopeMap.put("chat:write", false);
                    scopeMap.put("chat:write.customize", false);
                    scopeMap.put("chat:write.public", false);
                    scopeMap.put("client", false);
                    scopeMap.put("commands", false);
                    scopeMap.put("connections:write", false);
                    scopeMap.put("conversations:write.invites", false);
                    scopeMap.put("conversations:write.topic", false);
                    scopeMap.put("conversations.connect:manage", false);
                    scopeMap.put("conversations.connect:read", false);
                    scopeMap.put("conversations.connect:write", false);
                    scopeMap.put("datastore:read", false);
                    scopeMap.put("datastore:write", false);
                    scopeMap.put("dnd:read", false);
                    scopeMap.put("dnd:write", false);
                    scopeMap.put("email", false);
                    scopeMap.put("emoji:read", false);
                    scopeMap.put("files:read", false);
                    scopeMap.put("files:write", false);
                    scopeMap.put("files:write:user", false);
                    scopeMap.put("groups:history", false);
                    scopeMap.put("groups:read", true);
                    scopeMap.put("groups:write", false);
                    scopeMap.put("groups:write.invites", false);
                    scopeMap.put("groups:write.topic", false);
                    scopeMap.put("hosting:read", false);
                    scopeMap.put("hosting:write", false);
                    scopeMap.put("identify", false);
                    scopeMap.put("im:history", false);
                    scopeMap.put("im:read", false);
                    scopeMap.put("im:write", false);
                    scopeMap.put("im:write.topic", false);
                    scopeMap.put("incoming-webhook", true);
                    scopeMap.put("links:read", false);
                    scopeMap.put("links:write", false);
                    scopeMap.put("links.embed:write", false);
                    scopeMap.put("lists:read", false);
                    scopeMap.put("lists:write", false);
                    scopeMap.put("metadata.message:read", false);
                    scopeMap.put("mpim:history", false);
                    scopeMap.put("mpim:read", true);
                    scopeMap.put("mpim:write", false);
                    scopeMap.put("mpim:write.topic", false);
                    scopeMap.put("openid", false);
                    scopeMap.put("pins:read", false);
                    scopeMap.put("pins:write", false);
                    scopeMap.put("profile", false);
                    scopeMap.put("reactions:read", true);
                    scopeMap.put("reactions:write", true);
                    scopeMap.put("reminders:read", false);
                    scopeMap.put("reminders:write", false);
                    scopeMap.put("remote_files:read", false);
                    scopeMap.put("remote_files:share", false);
                    scopeMap.put("remote_files:write", false);
                    scopeMap.put("search:read", false);
                    scopeMap.put("search:read.enterprise", false);
                    scopeMap.put("search:read.files", false);
                    scopeMap.put("search:read.im", false);
                    scopeMap.put("search:read.mpim", false);
                    scopeMap.put("search:read.private", false);
                    scopeMap.put("search:read.public", false);
                    scopeMap.put("search:read.users", false);
                    scopeMap.put("stars:read", false);
                    scopeMap.put("stars:write", false);
                    scopeMap.put("team:read", false);
                    scopeMap.put("team.billing:read", false);
                    scopeMap.put("team.preferences:read", false);
                    scopeMap.put("tokens.basic", false);
                    scopeMap.put("triggers:read", false);
                    scopeMap.put("triggers:write", false);
                    scopeMap.put("usergroups:read", false);
                    scopeMap.put("usergroups:write", false);
                    scopeMap.put("users:read", true);
                    scopeMap.put("users:read.email", false);
                    scopeMap.put("users:write", false);
                    scopeMap.put("users.profile:read", false);
                    scopeMap.put("users.profile:write", false);
                    scopeMap.put("workflows.templates:read", false);
                    scopeMap.put("workflows.templates:write", false);

                    return scopeMap;
                })
                .tokenUrl((connection, context) -> "https://slack.com/api/oauth.access"));

    private SlackConnection() {
    }
}
