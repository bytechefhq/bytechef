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

package com.bytechef.component.ai.image;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.platform.component.definition.AiComponentDefinition.AI_IMAGE;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.image.action.GenerateImageAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.AiComponentDefinition;
import com.bytechef.platform.configuration.service.PropertyService;
import org.springframework.stereotype.Component;

/**
 * @author Marko Kriskovic
 */
@Component(AI_IMAGE + "_v1_ComponentHandler")
public class AiImageComponentHandler implements ComponentHandler {

    private final AiComponentDefinition componentDefinition;

    public AiImageComponentHandler(ApplicationProperties applicationProperties, PropertyService propertyService) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        this.componentDefinition = new AiImageComponentDefinitionImpl(ai.getProvider(), propertyService);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AiImageComponentDefinitionImpl
        extends AbstractComponentDefinitionWrapper implements AiComponentDefinition {

        private AiImageComponentDefinitionImpl(
            ApplicationProperties.Ai.Provider provider, PropertyService propertyService) {

            super(
                component(AI_IMAGE)
                    .title("AI Image")
                    .description("AI Helper component for image analysis and generation.")
                    .icon("path:assets/ai-image.svg")
                    .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                    .actions(
                        new GenerateImageAction(provider, propertyService).actionDefinition));
        }
    }
}
