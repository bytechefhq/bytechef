/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.handler;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.ee.platform.customcomponent.loader.ComponentHandlerLoader;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ComponentHandlerWrapper;
import com.bytechef.platform.component.handler.DynamicComponentHandlerRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CustomComponentDynamicComponentHandlerRegistry implements DynamicComponentHandlerRegistry {

    private final CacheManager cacheManager;
    private final CustomComponentFileStorage customComponentFileStorage;
    private final CustomComponentService customComponentService;
    private final ComponentHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public CustomComponentDynamicComponentHandlerRegistry(
        ApplicationProperties applicationProperties, CacheManager cacheManager,
        CustomComponentFileStorage customComponentFileStorage, CustomComponentService customComponentService) {

        this.cacheManager = cacheManager;
        this.customComponentFileStorage = customComponentFileStorage;
        this.customComponentService = customComponentService;
        this.javaLoader = toLoaderJavaLoader(applicationProperties);
    }

    private static ComponentHandlerLoader.JavaLoader toLoaderJavaLoader(ApplicationProperties applicationProperties) {
        ApplicationProperties.Component component = applicationProperties.getComponent();

        ApplicationProperties.Component.CustomComponent customComponent = component.getCustomComponent();

        return customComponent
            .getJavaLoader() == ApplicationProperties.Component.CustomComponent.JavaLoader.CLASS_LOADER
                ? ComponentHandlerLoader.JavaLoader.CLASS_LOADER
                : ComponentHandlerLoader.JavaLoader.ESPRESSO;
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
        URL url = customComponentFileStorage.getCustomComponentFileURL(customComponent.getComponent());

        ComponentHandler componentHandler = ComponentHandlerLoader.loadComponentHandler(
            url, customComponent.getLanguage(), javaLoader,
            EncodingUtils.base64EncodeToString(customComponent.toString()), cacheManager);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        return new ComponentHandlerWrapper(
            new ComponentDefinitionWrapper(
                componentDefinition,
                componentDefinition.getActions()
                    .stream()
                    .map(actionDefinition -> (ActionDefinition) actionDefinition)
                    .toList()) {

                @Override
                public int getVersion() {
                    return componentVersion;
                }
            });
    }
}
