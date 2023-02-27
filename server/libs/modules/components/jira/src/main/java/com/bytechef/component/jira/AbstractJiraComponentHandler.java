
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.jira;

import static com.bytechef.hermes.component.constant.ComponentConstants.CLIENT_ID;
import static com.bytechef.hermes.component.constant.ComponentConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.constant.ComponentConstants.PASSWORD;
import static com.bytechef.hermes.component.constant.ComponentConstants.USERNAME;
import static com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.jira.action.CreateIssueAction;
import com.bytechef.component.jira.action.GetIssueAction;
import com.bytechef.component.jira.action.SearchForIssuesUsingJqlAction;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.util.List;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractJiraComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = component("jira")
        .display(
            modifyDisplay(
                display("Jira")
                    .description("Jira Cloud platform REST API documentation")))
        .actions(modifyActions(CreateIssueAction.ACTION_DEFINITION, GetIssueAction.ACTION_DEFINITION,
            SearchForIssuesUsingJqlAction.ACTION_DEFINITION))
        .connection(modifyConnection(
            connection()
                .baseUri(connection -> "https://your-domain.atlassian.net")
                .authorizations(authorization(
                    AuthorizationType.BASIC_AUTH.name()
                        .toLowerCase(),
                    AuthorizationType.BASIC_AUTH)
                        .display(
                            display("Basic Auth"))
                        .properties(
                            string(USERNAME)
                                .label("Username")
                                .required(true),
                            string(PASSWORD)
                                .label("Password")
                                .required(true)),
                    authorization(
                        AuthorizationType.OAUTH2_AUTHORIZATION_CODE.name()
                            .toLowerCase(),
                        AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                            .display(
                                display("OAuth2 Authorization Code"))
                            .properties(
                                string(CLIENT_ID)
                                    .label("Client Id")
                                    .required(true),
                                string(CLIENT_SECRET)
                                    .label("Client Secret")
                                    .required(true))
                            .authorizationUrl(connection -> "https://auth.atlassian.com/authorize")
                            .scopes(connection -> List.of("read:jira-work", "write:jira-work"))
                            .tokenUrl(connection -> "https://auth.atlassian.com/oauth/token"))));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
