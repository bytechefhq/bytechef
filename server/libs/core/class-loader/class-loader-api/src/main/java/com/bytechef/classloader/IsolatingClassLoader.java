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
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author Ivica Cardic
 */
public abstract class IsolatingClassLoader<T> extends URLClassLoader {

    private static final String ISOLATING_CLASS_LOADER_CACHE =
        IsolatingClassLoader.class.getName() + ".isolatingClassLoader";
    private static final String[] PACKAGE_PREFIXES = {
        "java.", "jdk.", "com.bytechef."
    };
    private static final String[] RESOURCE_PREFIXES = Arrays.stream(PACKAGE_PREFIXES)
        .map(p -> p.replace('.', '/'))
        .toArray(String[]::new);

    private final CacheManager cacheManager;
    private final BiFunction<String, IsolatingClassLoader<T>, T> cacheMappingFunction;

    protected IsolatingClassLoader(
        URL[] jarUrls, ClassLoader parent, CacheManager cacheManager,
        BiFunction<String, IsolatingClassLoader<T>, T> cacheMappingFunction) {

        super(jarUrls, parent);

        this.cacheManager = cacheManager;
        this.cacheMappingFunction = cacheMappingFunction;
    }

    protected T get(String key) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(ISOLATING_CLASS_LOADER_CACHE));

        return cache.get(key, () -> cacheMappingFunction.apply(key, this));
    }

    @Override
    public URL getResource(String name) {
        Stream<String> stream = Arrays.stream(RESOURCE_PREFIXES);

        if (stream.anyMatch(name::startsWith)) {
            return getParent().getResource(name);
        } else {
            return findResource(name);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws java.io.IOException {
        Stream<String> stream = Arrays.stream(RESOURCE_PREFIXES);

        if (stream.anyMatch(name::startsWith)) {
            return getParent().getResources(name);
        } else {
            return findResources(name);
        }
    }

    @Override
    protected Package[] getPackages() {
        Package[] parentPackages = getParent().getDefinedPackages();
        Package[] definedPackages = getDefinedPackages();

        return Stream
            .concat(
                Arrays.stream(definedPackages),
                Arrays.stream(parentPackages)
                    .filter(packageName -> {
                        Stream<String> stream = Arrays.stream(PACKAGE_PREFIXES);

                        return stream.anyMatch(prefix -> {
                            String name = packageName.getName();

                            return name.startsWith(prefix);
                        });
                    }))
            .toArray(Package[]::new);
    }

    protected void invalidateCache(String componentName) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(ISOLATING_CLASS_LOADER_CACHE));

        cache.evict(componentName);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);

            if (clazz == null) {
                Stream<String> stream = Arrays.stream(PACKAGE_PREFIXES);

                if (stream.anyMatch(name::startsWith)) {
                    clazz = getParent().loadClass(name);
                } else {
                    clazz = findClass(name);
                }
            }

            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }
}
