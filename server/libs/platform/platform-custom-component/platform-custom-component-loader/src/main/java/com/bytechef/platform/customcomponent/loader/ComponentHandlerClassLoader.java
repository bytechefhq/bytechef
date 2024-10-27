/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.customcomponent.loader;

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
import java.util.Optional;
import java.util.ServiceLoader;
import org.springframework.util.StreamUtils;

/**
 * @author Ivica Cardic
 */
class ComponentHandlerClassLoader extends IsolatingClassLoader<ComponentHandler> {

    private final String cacheKey;

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    private ComponentHandlerClassLoader(URL jarUrl, String cacheKey) {
        super(
            new URL[] {
                jarUrl
            }, ComponentHandlerClassLoader.class.getClassLoader(),
            (key, classLoader) -> {
                ServiceLoader<ComponentHandler> loader = ServiceLoader.load(ComponentHandler.class, classLoader);

                for (ComponentHandler componentHandler : loader) {
                    return new ContextClassLoaderComponentHandler(componentHandler, readIcon(classLoader), classLoader);
                }

                return null;
            });

        this.cacheKey = cacheKey;
    }

    @SuppressFBWarnings("DP")
    static ComponentHandlerClassLoader of(URL jarUrl, String cacheKey) {
        return new ComponentHandlerClassLoader(jarUrl, cacheKey);
    }

    ComponentHandler loadComponentHandler() {
        return get(cacheKey);
    }

    private static String readIcon(IsolatingClassLoader<ComponentHandler> classLoader) {
        try {
            try (InputStream inputStream = classLoader.getResourceAsStream("assets/sample.svg")) {
                return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            }
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
