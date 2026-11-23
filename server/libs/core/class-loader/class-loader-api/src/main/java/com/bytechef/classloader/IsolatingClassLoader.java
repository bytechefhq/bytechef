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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author Ivica Cardic
 */
public abstract class IsolatingClassLoader<T> extends URLClassLoader {

    private static final String ISOLATING_CLASS_LOADER_CACHE =
        IsolatingClassLoader.class.getName() + ".isolatingClassLoader";
    private static final ClassLoader PLATFORM_CLASS_LOADER = getPlatformClassLoader();

    /**
     * Packages of the JDK's boot and platform modules. They are not all named {@code java.*}: {@code javax.net.ssl},
     * {@code javax.crypto} and {@code javax.xml} are JDK packages too, and a component JAR cannot supply them.
     */
    private static final Set<String> PLATFORM_PACKAGES = ModuleLayer.boot()
        .modules()
        .stream()
        .filter(module -> {
            ClassLoader classLoader = module.getClassLoader();

            return classLoader == null || classLoader == PLATFORM_CLASS_LOADER;
        })
        .flatMap(module -> module.getPackages()
            .stream())
        .collect(Collectors.toUnmodifiableSet());

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

        AtomicBoolean created = new AtomicBoolean();

        T value;

        try {
            value = cache.get(key, () -> {
                created.set(true);

                return cacheMappingFunction.apply(key, this);
            });
        } catch (RuntimeException exception) {
            closeClassLoader();

            throw exception;
        }

        if (!created.get() || value == null) {
            closeClassLoader();
        }

        return value;
    }

    protected <R> R applyUncached(String key, Function<T, R> function) {
        try {
            T value = cacheMappingFunction.apply(key, this);

            if (value == null) {
                return null;
            }

            return function.apply(value);
        } finally {
            closeClassLoader();
        }
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
    public Enumeration<URL> getResources(String name) throws IOException {
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

    private void closeClassLoader() {
        try {
            close();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    protected void invalidateCache(String key) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(ISOLATING_CLASS_LOADER_CACHE));

        Cache.ValueWrapper valueWrapper = cache.get(key);

        cache.evict(key);

        if (valueWrapper != null && valueWrapper.get() instanceof ClassLoaderOwner classLoaderOwner) {
            IsolatingClassLoader<?> classLoader = classLoaderOwner.classLoader();

            classLoader.closeClassLoader();
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);

            if (clazz == null) {
                Stream<String> stream = Arrays.stream(PACKAGE_PREFIXES);

                if (stream.anyMatch(name::startsWith)) {
                    clazz = getParent().loadClass(name);
                } else if (isPlatformClass(name)) {
                    clazz = PLATFORM_CLASS_LOADER.loadClass(name);
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

    private static boolean isPlatformClass(String className) {
        int lastDotIndex = className.lastIndexOf('.');

        return lastDotIndex > 0 && PLATFORM_PACKAGES.contains(className.substring(0, lastDotIndex));
    }

    public interface ClassLoaderOwner {

        IsolatingClassLoader<?> classLoader();
    }
}
