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

package com.bytechef.component.stability;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.stability.constant.StabilityConstants.STABILITY;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.stability.action.StabilityCreateImageAction;
import com.bytechef.component.stability.connection.StabilityConnection;
import com.bytechef.component.stability.constant.StabilityConstants;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class StabilityComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(STABILITY)
        .title("Stability AI")
        .description(
            "Activating humanity's potential through generative AI. Open models in every modality, for everyone, everywhere.")
        .icon("path:assets/stability.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(StabilityConnection.CONNECTION_DEFINITION)
        .actions(StabilityCreateImageAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
