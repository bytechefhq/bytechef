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

package com.bytechef.component.apify;

import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.apify.action.ApifyStartActorAction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
@AutoService(OpenApiComponentHandler.class)
public class ApifyComponentHandler extends AbstractApifyComponentHandler {

    @Override
    public List<? extends ModifiableActionDefinition> getCustomActions() {
        return List.of(ApifyStartActorAction.ACTION_DEFINITION);
    }

    @Override
    public List<ClusterElementDefinition<?>> getCustomClusterElements() {
        return List.of(tool(ApifyStartActorAction.ACTION_DEFINITION));
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/apify.svg")
            .categories(ComponentCategory.MARKETING_AUTOMATION)
            .customAction(true);
    }
}
