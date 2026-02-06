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

package com.bytechef.component.gitlab;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.gitlab.trigger.GitlabNewIssueTrigger;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Monika Kušter
 */
@AutoService(OpenApiComponentHandler.class)
public class GitlabComponentHandler extends AbstractGitlabComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(GitlabNewIssueTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            switch (name) {
                case "createCommentOnIssue" ->
                    actionDefinition.help("",
                        "https://docs.bytechef.io/reference/components/gitlab_v1#create-comment-on-issue");
                case "createIssue" ->
                    actionDefinition.help("",
                        "https://docs.bytechef.io/reference/components/gitlab_v1#create-issue");
                default -> {
                }
            }
        }
        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customActionHelp("", "https://docs.gitlab.com/api/rest/")
            .customAction(true)
            .icon("path:assets/gitlab.svg")
            .categories(ComponentCategory.DEVELOPER_TOOLS)
            .version(1);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .help("", "https://docs.bytechef.io/reference/components/gitlab_v1#connection-setup")
            .version(1);
    }
}
