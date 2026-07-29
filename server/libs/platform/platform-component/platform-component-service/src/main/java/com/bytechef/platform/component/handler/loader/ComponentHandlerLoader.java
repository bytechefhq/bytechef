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

package com.bytechef.platform.component.handler.loader;

import com.bytechef.component.ComponentHandler;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.task.handler.ComponentTaskHandler;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ComponentHandlerLoader {

    /**
     * Stable identifier of this loader ({@code default}, {@code jdbc} or {@code openapi}), recorded per component in
     * the build-time component index so the registry knows which loader to route an on-demand single-component load
     * through.
     */
    String getKind();

    List<ComponentHandlerEntry> loadComponentHandlers();

    /**
     * Loads and wraps the single component handler provided by the given {@code ServiceLoader} provider class, without
     * instantiating any other provider. Returns empty when no provider with that class name exists on the classpath.
     * Applies the exact same wrapping as {@link #loadComponentHandlers()}, so the returned entry is safe for execution
     * paths, not only for definition reads.
     */
    Optional<ComponentHandlerEntry> loadComponentHandler(String providerClassName);

    /**
     * Loads every component handler like {@link #loadComponentHandlers()}, additionally exposing each handler's
     * {@code ServiceLoader} provider class name — the key the build-time index generator records so components can be
     * loaded individually later via {@link #loadComponentHandler(String)}.
     */
    List<ProviderEntry> loadProviderEntries();

    record ComponentHandlerEntry(
        ComponentHandler componentHandler, ComponentTaskHandlerFunction componentTaskHandlerFunction) {
    }

    record ProviderEntry(String providerClassName, ComponentHandlerEntry componentHandlerEntry) {
    }

    @FunctionalInterface
    interface ComponentTaskHandlerFunction {

        ComponentTaskHandler apply(String actionName, ActionDefinitionFacade actionDefinitionFacade);
    }
}
