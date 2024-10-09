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

package com.bytechef.component.github;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.github.connection.GithubConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.github.constant.GithubConstants.GITHUB;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.github.action.GithubAddAssigneesToIssueAction;
import com.bytechef.component.github.action.GithubCreateCommentOnIssueAction;
import com.bytechef.component.github.action.GithubCreateIssueAction;
import com.bytechef.component.github.action.GithubGetIssueAction;
import com.bytechef.component.github.trigger.GithubNewIssueTrigger;
import com.bytechef.component.github.trigger.GithubNewPullRequestTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 */
@AutoService(ComponentHandler.class)
public class GithubComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(GITHUB)
        .title("Github")
        .description("GitHub is a web-based platform for version control and collaboration using Git.")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GithubCreateIssueAction.ACTION_DEFINITION,
            GithubGetIssueAction.ACTION_DEFINITION,
            GithubCreateCommentOnIssueAction.ACTION_DEFINITION,
            GithubAddAssigneesToIssueAction.ACTION_DEFINITION)
        .icon("path:assets/github.svg")
        .triggers(
            GithubNewIssueTrigger.TRIGGER_DEFINITION,
            GithubNewPullRequestTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
