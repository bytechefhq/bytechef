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

import static com.bytechef.component.gitlab.constant.GitlabConstants.PROJECT_ID;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.gitlab.trigger.GitlabNewIssueTrigger;
import com.bytechef.component.gitlab.util.GitlabUtils;
import com.google.auto.service.AutoService;
import java.util.List;
import java.util.Objects;

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
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/gitlab.svg")
            .categories(ComponentCategory.DEVELOPER_TOOLS);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), PROJECT_ID)) {
            ((ModifiableStringProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) GitlabUtils::getProjectOptions);
        } else if (Objects.equals(modifiableProperty.getName(), "issueId")) {
            ((ModifiableIntegerProperty) modifiableProperty)
                .options((ActionOptionsFunction<String>) GitlabUtils::getIssueOptions)
                .optionsLookupDependsOn(PROJECT_ID);
        }

        return modifiableProperty;
    }
}
