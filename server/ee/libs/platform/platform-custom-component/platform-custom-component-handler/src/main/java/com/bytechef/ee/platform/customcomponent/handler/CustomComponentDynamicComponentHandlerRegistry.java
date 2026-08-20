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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CustomComponentDynamicComponentHandlerRegistry.class);

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

    /**
     * Loads a handler for every enabled, published custom component, skipping any single component that fails to load.
     *
     * <p>
     * This is the whole-registry path: its result is concatenated into the platform-wide component list, so an
     * exception thrown here would drop <em>every</em> custom component, not just the broken one, and the symptom a user
     * reports is "all my custom components vanished". A component can stop loading for reasons that have nothing to do
     * with the component itself — its guest language no longer resolving on the classpath, its stored file having gone
     * missing, a jar compiled against a since-changed API — so the guard is deliberately general rather than keyed to
     * any particular language or failure.
     */
    @Override
    public List<? extends ComponentHandler> getComponentHandlers() {
        List<ComponentHandler> componentHandlers = new ArrayList<>();

        for (CustomComponent customComponent : customComponentService.getCustomComponents()) {
            if (!customComponent.isEnabled() || customComponent.getStatus() != CustomComponent.Status.PUBLISHED) {
                continue;
            }

            try {
                componentHandlers.add(
                    loadComponentHandler(customComponent, customComponent.getComponentVersion()));
            } catch (RuntimeException exception) {
                // A single unloadable custom component must not strip the rest of the tenant's custom components.
                log.warn(
                    "Skipping custom component {} (version {}) — could not load it", customComponent.getName(),
                    customComponent.getComponentVersion(), exception);
            }
        }

        return componentHandlers;
    }

    /**
     * Loads the handler for one named, published custom component.
     *
     * <p>
     * Unlike {@link #getComponentHandlers()} this is a targeted, single-component lookup, so a load failure is left to
     * propagate: the caller asked about exactly this component, and swallowing the exception into an empty
     * {@link Optional} would make "this component is broken" indistinguishable from "no such component" — the very
     * silent-disappearance symptom the guard above exists to avoid. The blast radius is the one request that asked.
     */
    @Override
    public Optional<ComponentHandler> fetchComponentHandler(String name, int componentVersion) {
        return customComponentService.fetchCustomComponent(name, componentVersion)
            .filter(customComponent -> customComponent.getStatus() == CustomComponent.Status.PUBLISHED)
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
