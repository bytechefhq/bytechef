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

package com.bytechef.component.hubspot;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.hubspot.trigger.HubspotSubscribeTrigger;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@AutoService(OpenApiComponentHandler.class)
public class HubspotComponentHandler extends AbstractHubspotComponentHandler {

    @Override
    public List<ComponentDSL.ModifiableTriggerDefinition> getTriggers() {
        return List.of(HubspotSubscribeTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/hubspot.svg")
            .categories(ComponentCategory.MARKETING_AUTOMATION);
    }
}
