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

package com.bytechef.classloader;

import java.net.URL;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import org.springframework.cache.CacheManager;

/**
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

    protected String getCacheKey() {
        return cacheKey;
    }
}
