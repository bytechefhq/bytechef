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

package com.bytechef.platform.customcomponent.handler;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ComponentHandlerWrapper;
import com.bytechef.platform.component.handler.DynamicComponentHandlerFactory;
import com.bytechef.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.platform.customcomponent.loader.ComponentHandlerLoader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class CustomComponentDynamicComponentHandlerFactory implements DynamicComponentHandlerFactory {

    private final CustomComponentFileStorage customComponentFileStorage;
    private final CustomComponentService customComponentService;

    @SuppressFBWarnings("EI")
    public CustomComponentDynamicComponentHandlerFactory(
        CustomComponentFileStorage customComponentFileStorage, CustomComponentService customComponentService) {

        this.customComponentFileStorage = customComponentFileStorage;
        this.customComponentService = customComponentService;
    }

    @Override
    public List<? extends ComponentHandler> getComponentHandlers() {
        return customComponentService.getCustomComponents()
            .stream()
            .filter(CustomComponent::isEnabled)
            .map(customComponent -> loadComponentHandler(customComponent, customComponent.getComponentVersion()))
            .toList();
    }

    @Override
    public Optional<ComponentHandler> fetchComponentHandler(String name, int componentVersion) {
        return customComponentService.fetchCustomComponent(name, componentVersion)
            .map(customComponent -> loadComponentHandler(customComponent, componentVersion));
    }

    private ComponentHandler loadComponentHandler(CustomComponent customComponent, int componentVersion) {
        URL url = customComponentFileStorage.getCustomComponentFileURL(customComponent.getComponentFile());

        ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
            url, customComponent.getLanguage(), EncodingUtils.base64EncodeToString(customComponent.toString()));

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        return new ComponentHandlerWrapper(
            new ComponentDefinitionWrapper(
                componentDefinition,
                OptionalUtils.orElse(
                    componentDefinition.getActions()
                        .map(actionDefinitions -> actionDefinitions.stream()
                            .map(actionDefinition -> (ActionDefinition) actionDefinition)
                            .toList()),
                    List.of())) {

                @Override
                public int getVersion() {
                    return componentVersion;
                }
            });
    }
}
