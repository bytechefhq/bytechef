/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.loader;

import com.bytechef.classloader.IsolatingClassLoader;
import java.net.URL;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import org.springframework.cache.CacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractHandlerClassLoader<H> extends IsolatingClassLoader<H> {

    private final String cacheKey;

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    protected AbstractHandlerClassLoader(
        URL jarUrl, String cacheKey, CacheManager cacheManager, Class<H> handlerClass,
        BiFunction<H, IsolatingClassLoader<?>, H> contextClassLoaderHandlerFactory) {

        super(
            new URL[] {
                jarUrl
            }, handlerClass.getClassLoader(),
            cacheManager,
            (key, classLoader) -> {
                ServiceLoader<H> serviceLoader = ServiceLoader.load(handlerClass, classLoader);

                for (H handler : serviceLoader) {
                    return contextClassLoaderHandlerFactory.apply(handler, classLoader);
                }

                return null;
            });

        this.cacheKey = cacheKey;
    }

    public H loadHandler() {
        return get(cacheKey);
    }
}
