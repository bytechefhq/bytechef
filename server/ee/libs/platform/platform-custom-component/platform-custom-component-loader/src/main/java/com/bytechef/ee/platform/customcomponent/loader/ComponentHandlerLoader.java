/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.platform.component.polyglot.AbstractHandlerLoader;
import java.net.URL;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ComponentHandlerLoader extends AbstractHandlerLoader<ComponentHandler> {

    private static final ComponentHandlerLoader INSTANCE = new ComponentHandlerLoader();

    private ComponentHandlerLoader() {
    }

    public static ComponentHandler loadComponentHandler(
        URL url, Language language, String cacheKey, CacheManager cacheManager) {

        return INSTANCE.loadHandler(url, language, cacheKey, cacheManager);
    }

    public static ComponentDefinition loadComponentDefinition(URL url, Language language) {
        ComponentDefinition componentDefinition;

        if (language == Language.JAVA) {
            ComponentHandlerClassLoader componentHandlerClassLoader = ComponentHandlerClassLoader.of(
                url, url.toString(), null);

            componentDefinition = componentHandlerClassLoader.loadComponentDefinition();
        } else {
            ComponentHandler componentHandler = INSTANCE.loadHandler(url, language, url.toString(), null);

            componentDefinition = componentHandler.getDefinition();
        }

        if (componentDefinition == null) {
            throw new IllegalArgumentException("No component handler found in " + url);
        }

        return componentDefinition;
    }

    @Override
    protected ComponentHandler loadJavaHandler(URL url, String cacheKey, CacheManager cacheManager) {
        ComponentHandlerClassLoader componentHandlerClassLoader =
            ComponentHandlerClassLoader.of(url, cacheKey, cacheManager);

        return componentHandlerClassLoader.loadHandler();
    }

    @Override
    protected ComponentHandler loadPolyglotHandler(String languageId, String script) {
        return ComponentHandlerPolyglotEngine.load(languageId, script);
    }
}
