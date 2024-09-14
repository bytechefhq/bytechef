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

package com.bytechef.component.gitlab;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.gitlab.action.GitlabCreateCommentOnIssueAction;
import com.bytechef.component.gitlab.action.GitlabCreateIssueAction;
import com.bytechef.component.gitlab.connection.GitlabConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractGitlabComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("gitlab")
            .title("GitLab")
            .description(
                "GitLab is a web-based DevOps lifecycle tool that provides a Git repository manager, CI/CD pipelines, issue tracking, and more in a single application."))
                    .actions(modifyActions(GitlabCreateIssueAction.ACTION_DEFINITION,
                        GitlabCreateCommentOnIssueAction.ACTION_DEFINITION))
                    .connection(modifyConnection(GitlabConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
