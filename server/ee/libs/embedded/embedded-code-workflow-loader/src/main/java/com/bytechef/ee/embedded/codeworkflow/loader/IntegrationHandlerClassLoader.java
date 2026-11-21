/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.classloader.AbstractHandlerClassLoader;
import com.bytechef.classloader.IsolatingClassLoader;
import com.bytechef.classloader.util.ClassLoaderUtils;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import java.net.URL;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerClassLoader extends AbstractHandlerClassLoader<IntegrationHandler> {

    private IntegrationHandlerClassLoader(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        super(jarUrl, cacheKey, cacheManager, IntegrationHandler.class, ContextClassLoaderIntegrationHandler::new);
    }

    static IntegrationHandlerClassLoader of(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        return new IntegrationHandlerClassLoader(jarUrl, cacheKey, cacheManager);
    }

    private record ContextClassLoaderIntegrationHandler(
        IntegrationHandler integrationHandler, IsolatingClassLoader<?> classLoader) implements IntegrationHandler {

        @Override
        public IntegrationDefinition getDefinition() {
            return ClassLoaderUtils.loadWithClassLoader(classLoader, integrationHandler::getDefinition);
        }
    }
}
