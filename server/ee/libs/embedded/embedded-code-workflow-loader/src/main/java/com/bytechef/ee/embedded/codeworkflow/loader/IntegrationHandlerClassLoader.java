/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.classloader.IsolatingClassLoader;
import com.bytechef.classloader.util.ClassLoaderUtils;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import java.net.URL;
import java.util.ServiceLoader;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerClassLoader extends IsolatingClassLoader<IntegrationHandler> {

    private final String cacheKey;

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private IntegrationHandlerClassLoader(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        super(
            new URL[] {
                jarUrl
            }, IntegrationHandlerClassLoader.class.getClassLoader(),
            cacheManager,
            (key, classLoader) -> {
                ServiceLoader<IntegrationHandler> loader = ServiceLoader.load(IntegrationHandler.class, classLoader);

                for (IntegrationHandler integrationHandler : loader) {
                    return new ContextClassLoaderIntegrationHandler(integrationHandler, classLoader);
                }

                return null;
            });

        this.cacheKey = cacheKey;
    }

    static IntegrationHandlerClassLoader of(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        return new IntegrationHandlerClassLoader(jarUrl, cacheKey, cacheManager);
    }

    IntegrationHandler loadIntegrationHandler() {
        return get(cacheKey);
    }

    /**
     * An IntegrationHandler that wraps another IntegrationHandler, using the given class loader as the thread's context
     * class loader for all its invocations.
     *
     * @author Ivica Cardic
     */
    private record ContextClassLoaderIntegrationHandler(
        IntegrationHandler integrationHandler, IsolatingClassLoader<?> classLoader) implements IntegrationHandler {

        @Override
        public IntegrationDefinition getDefinition() {
            return ClassLoaderUtils.loadWithClassLoader(classLoader, integrationHandler::getDefinition);
        }
    }
}
