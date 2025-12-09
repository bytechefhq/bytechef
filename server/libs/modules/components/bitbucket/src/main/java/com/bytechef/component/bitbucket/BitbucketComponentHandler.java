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

package com.bytechef.component.bitbucket;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.bitbucket.action.BitbucketListProjectsAction;
import com.bytechef.component.bitbucket.action.BitbucketListRepositoriesAction;
import com.bytechef.component.bitbucket.trigger.BitbucketRepositoryPushTrigger;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
@AutoService(OpenApiComponentHandler.class)
public class BitbucketComponentHandler extends AbstractBitbucketComponentHandler {

    @Override
    public List<ModifiableActionDefinition> getCustomActions() {
        return List.of(
            BitbucketListProjectsAction.ACTION_DEFINITION,
            BitbucketListRepositoriesAction.ACTION_DEFINITION);
    }

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(BitbucketRepositoryPushTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public List<ModifiableClusterElementDefinition<?>> modifyClusterElements(
        ModifiableClusterElementDefinition<?>... clusterElementDefinitions) {

        return List.of(
            tool(BitbucketListProjectsAction.ACTION_DEFINITION),
            tool(BitbucketListRepositoriesAction.ACTION_DEFINITION));
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/bitbucket.svg")
            .categories(ComponentCategory.PROJECT_MANAGEMENT)
            .customAction(true);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://api.bitbucket.org/2.0")
            .authorizations(
                authorization(AuthorizationType.BASIC_AUTH)
                    .title("API Key Authorization")
                    .properties(
                        string(USERNAME)
                            .label("Email Address")
                            .description("Email address of your Bitbucket account.")
                            .required(true),
                        string(PASSWORD)
                            .label("API Key")
                            .description("API key creation steps in documentation.")
                            .required(true)));
    }
}
