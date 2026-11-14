/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import com.bytechef.classloader.IsolatingClassLoader;
import com.bytechef.classloader.util.ClassLoaderUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.springframework.cache.CacheManager;
import org.springframework.util.StreamUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerClassLoader extends IsolatingClassLoader<ComponentHandler> {

    private static final String DEFAULT_ICON_PATH = "assets/sample.svg";
    private static final String PATH_PREFIX = "path:";

    private final String cacheKey;

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private ComponentHandlerClassLoader(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        super(
            new URL[] {
                jarUrl
            }, ComponentHandlerClassLoader.class.getClassLoader(),
            cacheManager,
            (key, classLoader) -> {
                ServiceLoader<ComponentHandler> loader = ServiceLoader.load(ComponentHandler.class, classLoader);

                for (ComponentHandler componentHandler : loader) {
                    ComponentDefinition componentDefinition = ClassLoaderUtils.loadWithClassLoader(
                        classLoader, componentHandler::getDefinition);

                    return new ContextClassLoaderComponentHandler(
                        componentHandler, readIcon(componentDefinition, classLoader), classLoader);
                }

                return null;
            });

        this.cacheKey = cacheKey;
    }

    @SuppressFBWarnings("DP")
    static ComponentHandlerClassLoader of(URL jarUrl, String cacheKey, CacheManager cacheManager) {
        return new ComponentHandlerClassLoader(jarUrl, cacheKey, cacheManager);
    }

    ComponentHandler loadComponentHandler() {
        return get(cacheKey);
    }

    static String readIcon(ComponentDefinition componentDefinition, ClassLoader classLoader) {
        String icon = componentDefinition.getIcon()
            .orElse(null);

        if (icon != null && !icon.startsWith(PATH_PREFIX)) {
            return icon;
        }

        List<String> iconPaths = new ArrayList<>();

        if (icon != null) {
            iconPaths.add(icon.substring(PATH_PREFIX.length()));
        }

        iconPaths.add("assets/" + componentDefinition.getName() + ".svg");
        iconPaths.add(DEFAULT_ICON_PATH);

        for (String iconPath : iconPaths) {
            String iconContent = readResource(classLoader, iconPath);

            if (iconContent != null) {
                return iconContent;
            }
        }

        return null;
    }

    private static String readResource(ClassLoader classLoader, String path) {
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                return null;
            }

            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A ComponentHandler that wraps another ComponentHandler, using the given class loader as the thread's context
     * class loader for all its invocations.
     *
     * @author Ivica Cardic
     */
    private record ContextClassLoaderComponentHandler(
        ComponentHandler componentHandler, String icon, IsolatingClassLoader<?> classLoader)
        implements ComponentHandler {

        @Override
        public ComponentDefinition getDefinition() {
            return new ComponentDefinitionWrapper(
                ClassLoaderUtils.loadWithClassLoader(classLoader, componentHandler::getDefinition), icon);
        }

        private static class ComponentDefinitionWrapper extends AbstractComponentDefinitionWrapper {

            private final String icon;

            private ComponentDefinitionWrapper(ComponentDefinition componentDefinition, String icon) {
                super(componentDefinition);

                this.icon = icon;
            }

            @Override
            public Optional<String> getIcon() {
                return Optional.ofNullable(icon);
            }
        }
    }
}
