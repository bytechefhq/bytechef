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

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.HashMap;
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
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("read:jira-user", true);
                    map.put("read:jira-work", true);
                    map.put("write:jira-work", true);
                    map.put("manage:jira-project", false);
                    map.put("manage:jira-configuration", false);
                    map.put("manage:jira-webhook", true);
                    map.put("read:application-role:jira", false);
                    map.put("read:audit-log:jira", false);
                    map.put("read:avatar:jira", false);
                    map.put("write:avatar:jira", false);
                    map.put("delete:avatar:jira", false);
                    map.put("read:project.avatar:jira", false);
                    map.put("write:project.avatar:jira", false);
                    map.put("delete:project.avatar:jira", false);
                    map.put("read:dashboard:jira", false);
                    map.put("write:dashboard:jira", false);
                    map.put("delete:dashboard:jira", false);
                    map.put("read:dashboard.property:jira", false);
                    map.put("write:dashboard.property:jira", false);
                    map.put("delete:dashboard.property:jira", false);
                    map.put("read:filter:jira", false);
                    map.put("write:filter:jira", false);
                    map.put("delete:filter:jira", false);
                    map.put("read:filter.column:jira", false);
                    map.put("write:filter.column:jira", false);
                    map.put("delete:filter.column:jira", false);
                    map.put("read:filter.default-share-scope:jira", false);
                    map.put("write:filter.default-share-scope:jira", false);
                    map.put("read:group:jira", false);
                    map.put("write:group:jira", false);
                    map.put("delete:group:jira", false);
                    map.put("read:license:jira", false);
                    map.put("read:issue:jira", false);
                    map.put("write:issue:jira", false);
                    map.put("delete:issue:jira", false);
                    map.put("read:issue-meta:jira", false);
                    map.put("send:notification:jira", false);
                    map.put("read:attachment:jira", false);
                    map.put("write:attachment:jira", false);
                    map.put("delete:attachment:jira", false);
                    map.put("read:comment:jira", false);
                    map.put("write:comment:jira", false);
                    map.put("delete:comment:jira", false);
                    map.put("read:comment.property:jira", false);
                    map.put("write:comment.property:jira", false);
                    map.put("delete:comment.property:jira", false);
                    map.put("read:field:jira", false);
                    map.put("write:field:jira", false);
                    map.put("delete:field:jira", false);
                    map.put("read:field.default-value:jira", false);
                    map.put("write:field.default-value:jira", false);
                    map.put("read:field.option:jira", false);
                    map.put("write:field.option:jira", false);
                    map.put("delete:field.option:jira", false);
                    map.put("read:field-configuration-scheme:jira", false);
                    map.put("write:field-configuration-scheme:jira", false);
                    map.put("delete:field-configuration-scheme:jira", false);
                    map.put("read:custom-field-contextual-configuration:jira", false);
                    map.put("write:custom-field-contextual-configuration:jira", false);
                    map.put("read:field-configuration:jira", false);
                    map.put("write:field-configuration:jira", false);
                    map.put("delete:field-configuration:jira", false);
                    map.put("read:field.options:jira", false);
                    map.put("read:issue-link:jira", false);
                    map.put("write:issue-link:jira", false);
                    map.put("delete:issue-link:jira", false);
                    map.put("read:issue-link-type:jira", false);
                    map.put("write:issue-link-type:jira", false);
                    map.put("delete:issue-link-type:jira", false);
                    map.put("read:notification-scheme:jira", false);
                    map.put("read:priority:jira", false);
                    map.put("read:issue.property:jira", false);
                    map.put("write:issue.property:jira", false);
                    map.put("delete:issue.property:jira", false);
                    map.put("read:issue.remote-link:jira", false);
                    map.put("write:issue.remote-link:jira", false);
                    map.put("delete:issue.remote-link:jira", false);
                    map.put("read:resolution:jira", false);
                    map.put("read:issue-details:jira", false);
                    map.put("read:issue-security-scheme:jira", false);
                    map.put("read:issue-type:jira", false);
                    map.put("write:issue-type:jira", false);
                    map.put("delete:issue-type:jira", false);
                    map.put("read:issue-type-scheme:jira", false);
                    map.put("write:issue-type-scheme:jira", false);
                    map.put("delete:issue-type-scheme:jira", false);
                    map.put("read:issue-type-screen-scheme:jira", false);
                    map.put("write:issue-type-screen-scheme:jira", false);
                    map.put("delete:issue-type-screen-scheme:jira", false);
                    map.put("read:issue-type.property:jira", false);
                    map.put("write:issue-type.property:jira", false);
                    map.put("delete:issue-type.property:jira", false);
                    map.put("read:issue.watcher:jira", false);
                    map.put("write:issue.watcher:jira", false);
                    map.put("read:issue-worklog:jira", false);
                    map.put("write:issue-worklog:jira", false);
                    map.put("delete:issue-worklog:jira", false);
                    map.put("read:issue-worklog.property:jira", false);
                    map.put("write:issue-worklog.property:jira", false);
                    map.put("delete:issue-worklog.property:jira", false);
                    map.put("read:issue-field-values:jira", false);
                    map.put("read:issue-security-level:jira", false);
                    map.put("read:issue-status:jira", false);
                    map.put("read:issue-type-hierarchy:jira", false);
                    map.put("read:issue-type-transition:jira", false);
                    map.put("read:issue.changelog:jira", false);
                    map.put("read:issue.transition:jira", false);
                    map.put("write:issue.vote:jira", false);
                    map.put("read:issue-event:jira", false);
                    map.put("read:jira-expressions:jira", false);
                    map.put("read:user:jira", false);
                    map.put("read:user.columns:jira", false);
                    map.put("read:label:jira", false);
                    map.put("read:permission:jira", false);
                    map.put("write:permission:jira", false);
                    map.put("delete:permission:jira", false);
                    map.put("read:permission-scheme:jira", false);
                    map.put("write:permission-scheme:jira", false);
                    map.put("delete:permission-scheme:jira", false);
                    map.put("read:project:jira", false);
                    map.put("write:project:jira", false);
                    map.put("delete:project:jira", false);
                    map.put("read:project-category:jira", false);
                    map.put("write:project-category:jira", false);
                    map.put("delete:project-category:jira", false);
                    map.put("read:project.component:jira", false);
                    map.put("write:project.component:jira", false);
                    map.put("delete:project.component:jira", false);
                    map.put("read:project.property:jira", false);
                    map.put("write:project.property:jira", false);
                    map.put("delete:project.property:jira", false);
                    map.put("read:project-role:jira", false);
                    map.put("write:project-role:jira", false);
                    map.put("delete:project-role:jira", false);
                    map.put("read:project-version:jira", false);
                    map.put("write:project-version:jira", false);
                    map.put("delete:project-version:jira", false);
                    map.put("read:project.feature:jira", false);
                    map.put("write:project.feature:jira", false);
                    map.put("read:screen:jira", false);
                    map.put("write:screen:jira", false);
                    map.put("delete:screen:jira", false);
                    map.put("read:screen-scheme:jira", false);
                    map.put("write:screen-scheme:jira", false);
                    map.put("delete:screen-scheme:jira", false);
                    map.put("read:screen-field:jira", false);
                    map.put("read:screen-tab:jira", false);
                    map.put("write:screen-tab:jira", false);
                    map.put("delete:screen-tab:jira", false);
                    map.put("read:screenable-field:jira", false);
                    map.put("write:screenable-field:jira", false);
                    map.put("delete:screenable-field:jira", false);
                    map.put("read:issue.time-tracking:jira", false);
                    map.put("write:issue.time-tracking:jira", false);
                    map.put("read:user.property:jira", false);
                    map.put("write:user.property:jira", false);
                    map.put("delete:user.property:jira", false);
                    map.put("read:webhook:jira", false);
                    map.put("write:webhook:jira", false);
                    map.put("delete:webhook:jira", false);
                    map.put("read:workflow:jira", false);
                    map.put("write:workflow:jira", false);
                    map.put("delete:workflow:jira", false);
                    map.put("read:workflow-scheme:jira", false);
                    map.put("write:workflow-scheme:jira", false);
                    map.put("delete:workflow-scheme:jira", false);
                    map.put("read:status:jira", false);
                    map.put("read:workflow.property:jira", false);
                    map.put("write:workflow.property:jira", false);
                    map.put("delete:workflow.property:jira", false);
                    map.put("delete:async-task:jira", false);
                    map.put("read:instance-configuration:jira", false);
                    map.put("write:instance-configuration:jira", false);
                    map.put("read:jql:jira", false);
                    map.put("validate:jql:jira", false);
                    map.put("read:project-type:jira", false);
                    map.put("read:project.email:jira", false);
                    map.put("write:project.email:jira", false);
                    map.put("read:role:jira", false);
                    map.put("read:user-configuration:jira", false);
                    map.put("write:user-configuration:jira", false);
                    map.put("delete:user-configuration:jira", false);
                    map.put("read:email-address:jira", false);
                    map.put("offline_access", true);

                    return map;
                }))
        .version(1);

    private JiraConnection() {
    }
}
