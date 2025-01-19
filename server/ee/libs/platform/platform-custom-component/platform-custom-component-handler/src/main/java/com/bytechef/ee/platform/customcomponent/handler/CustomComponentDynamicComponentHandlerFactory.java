/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.handler;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ComponentHandlerWrapper;
import com.bytechef.platform.component.handler.DynamicComponentHandlerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
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
