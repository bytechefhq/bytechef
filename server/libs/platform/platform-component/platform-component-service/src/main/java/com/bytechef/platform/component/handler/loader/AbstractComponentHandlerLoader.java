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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ComponentHandlerWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentHandlerLoader<T extends ComponentHandler> implements ComponentHandlerLoader {

    private final BiFunction<T, ActionDefinition, ActionDefinition> actionDefinitionMapperFunction;
    private final BiFunction<T, ClusterElementDefinition<?>, ClusterElementDefinition<?>> clusterElementDefinitionMapperFunction;
    private final Class<T> serviceClass;

    public AbstractComponentHandlerLoader(
        BiFunction<T, ActionDefinition, ActionDefinition> actionDefinitionMapperFunction,
        BiFunction<T, ClusterElementDefinition<?>, ClusterElementDefinition<?>> clusterElementDefinitionMapperFunction,
        Class<T> serviceClass) {

        this.actionDefinitionMapperFunction = actionDefinitionMapperFunction;
        this.clusterElementDefinitionMapperFunction = clusterElementDefinitionMapperFunction;
        this.serviceClass = serviceClass;
    }

    public List<ComponentHandlerEntry> loadComponentHandlers() {
        List<ComponentHandlerEntry> componentHandlerEntries = new ArrayList<>();

        for (T componentHandler : ServiceLoader.load(serviceClass)) {
            componentHandlerEntries.add(wrap(componentHandler));
        }

        return componentHandlerEntries;
    }

    @Override
    public Optional<ComponentHandlerEntry> loadComponentHandler(String providerClassName) {
        // ServiceLoader.Provider.type() loads the provider class WITHOUT initializing it, so scanning for the
        // requested class name is cheap; only the matching provider is instantiated (which triggers its static
        // initializer and definition construction).
        return ServiceLoader.load(serviceClass)
            .stream()
            .filter(provider -> {
                Class<? extends T> type = provider.type();

                return Objects.equals(type.getName(), providerClassName);
            })
            .findFirst()
            .map(provider -> wrap(provider.get()));
    }

    @Override
    public List<ProviderEntry> loadProviderEntries() {
        List<ProviderEntry> providerEntries = new ArrayList<>();

        for (ServiceLoader.Provider<T> provider : ServiceLoader.load(serviceClass)
            .stream()
            .toList()) {

            Class<? extends T> type = provider.type();

            providerEntries.add(new ProviderEntry(type.getName(), wrap(provider.get())));
        }

        return providerEntries;
    }

    protected abstract ComponentTaskHandlerFunction getComponentTaskHandlerFunction(T componentHandler);

    private ComponentHandlerEntry wrap(T componentHandler) {
        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        return new ComponentHandlerEntry(
            new ComponentHandlerWrapper(
                new ComponentDefinitionWrapper(
                    componentDefinition,
                    mapActionDefinitions(componentHandler, componentDefinition),
                    mapClusterElementDefinitions(componentHandler, componentDefinition))),
            getComponentTaskHandlerFunction(componentHandler));
    }

    private List<ActionDefinition> mapActionDefinitions(T componentHandler, ComponentDefinition componentDefinition) {
        return componentDefinition.getActions()
            .map(actionDefinitions -> CollectionUtils.map(
                actionDefinitions,
                actionDefinition -> actionDefinitionMapperFunction.apply(componentHandler, actionDefinition)))
            .orElse(List.of());
    }

    private List<ClusterElementDefinition<?>> mapClusterElementDefinitions(
        T componentHandler, ComponentDefinition componentDefinition) {

        return componentDefinition.getClusterElements()
            .map(clusterElementDefinitions -> CollectionUtils
                .<ClusterElementDefinition<?>, ClusterElementDefinition<?>>map(
                    clusterElementDefinitions,
                    clusterElementDefinition -> clusterElementDefinitionMapperFunction.apply(
                        componentHandler, clusterElementDefinition)))
            .orElse(List.of());
    }
}
