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

package com.bytechef.component.dhl;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Špehar
 */
@AutoService(OpenApiComponentHandler.class)
public class DhlComponentHandler extends AbstractDhlComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/dhl.svg")
            .categories(ComponentCategory.CUSTOMER_SUPPORT)
            .customActionHelp("DHL Developer documentation", "https://developer.dhl.com/")
            .version(1);
    }

    @Override
    public ModifiableActionDefinition modifyAction(ModifiableActionDefinition modifiableActionDefinition) {
        return modifiableActionDefinition
            .help("", "https://docs.bytechef.io/reference/components/dhl_v1#track-shipment");
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .help("", "https://docs.bytechef.io/reference/components/dhl_v1#connection-setup")
            .version(1);
    }
}
