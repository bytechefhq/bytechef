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

package com.bytechef.component.zoom;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.google.auto.service.AutoService;
import java.util.Objects;

/**
 * @author Nikolina Špehar
 */
@AutoService(OpenApiComponentHandler.class)
public class ZoomComponentHandler extends AbstractZoomComponentHandler {
    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/zoom.svg")
            .categories(ComponentCategory.COMMUNICATION)
            .customAction(true);
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "audio")) {
            ((ModifiableStringProperty) modifiableProperty)
                .options(
                    option("Both telephony and VoIP", "both"),
                    option("Telephony only", "telephony"),
                    option("VoIP only", "voip"),
                    option("Third party audio conference", "thirdParty"));
        }

        return modifiableProperty;
    }
}
