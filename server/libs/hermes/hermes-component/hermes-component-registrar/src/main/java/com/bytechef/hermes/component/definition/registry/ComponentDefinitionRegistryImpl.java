
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.definition.registry;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ComponentDefinitionRegistryImpl implements ComponentDefinitionRegistry {

    private static final ComponentDefinition MANUAL_COMPONENT_DEFINITION = component("manual")
        .title("Manual")
        .triggers(trigger("trigger"));

    private final List<ComponentDefinition> componentDefinitions;
    private final List<ConnectionDefinition> connectionDefinitions;

    public ComponentDefinitionRegistryImpl(
        List<ComponentDefinition> componentDefinitions, List<ComponentDefinitionFactory> componentDefinitionFactories) {

        this.componentDefinitions = CollectionUtils.concat(
            CollectionUtils.concat(
                componentDefinitions,
                componentDefinitionFactories.stream()
                    .map(ComponentDefinitionFactory::getDefinition)
                    .toList()),
            MANUAL_COMPONENT_DEFINITION)
            .stream()
            .sorted((o1, o2) -> {
                String o1Name = o1.getName();

                return o1Name.compareTo(o2.getName());
            })
            .toList();

        this.connectionDefinitions = componentDefinitions.stream()
            .map(componentDefinition -> OptionalUtils.orElse(componentDefinition.getConnection(), null))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public ActionDefinition getActionDefinition(
        String actionName, String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getActions(), Collections.emptyList())
            .stream()
            .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' action.".formatted(
                        componentName, actionName)));
    }

    @Override
    public List<? extends ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getActions(), Collections.emptyList());
    }

    @Override
    public Property<?> getActionProperty(
        String propertyName, String actionName, String componentName, int componentVersion) {

        ActionDefinition actionDefinition = getActionDefinition(actionName, componentName, componentVersion);

        return CollectionUtils.getFirst(
            OptionalUtils.get(actionDefinition.getProperties()),
            property -> Objects.equals(propertyName, property.getName()));
    }

    @Override
    public Authorization getAuthorization(String componentName, int connectionVersion, String authorizationName) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    @Override
    public ConnectionDefinition getComponentConnectionDefinition(String componentName, int connectionVersion) {
        return CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion)
                .orElse(false),
            componentDefinition -> OptionalUtils.get(componentDefinition.getConnection()));
    }

    @Override
    public ComponentDefinition getComponentDefinition(String name, Integer version) {
        ComponentDefinition componentDefinition;

        List<ComponentDefinition> filteredComponentDefinitions = componentDefinitions.stream()
            .filter(curComponentDefinition -> name.equalsIgnoreCase(curComponentDefinition.getName()))
            .toList();

        if (version == null) {
            componentDefinition = filteredComponentDefinitions.get(filteredComponentDefinitions.size() - 1);
        } else {
            componentDefinition = CollectionUtils.getFirst(
                filteredComponentDefinitions,
                curComponentDefinition -> version == curComponentDefinition.getVersion());
        }

        return componentDefinition;
    }

    @Override
    public List<? extends ComponentDefinition> getComponentDefinitions(String name) {
        return componentDefinitions.stream()
            .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
            .toList();
    }

    @Override
    public List<ComponentDefinition> getComponentDefinitions() {
        return componentDefinitions;
    }

    @Override
    public ConnectionDefinition getConnectionDefinition(String componentName, int componentVersion) {
        return CollectionUtils.getFirst(
            getComponentDefinitions(),
            componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                componentDefinition.getVersion() == componentVersion,
            componentDefinition -> OptionalUtils.get(componentDefinition.getConnection()));
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions() {
        return connectionDefinitions;
    }

    @Override
    public List<ConnectionDefinition> getConnectionDefinitions(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = CollectionUtils.getFirst(
            componentDefinitions,
            curComponentDefinition -> Objects.equals(curComponentDefinition.getName(), componentName) &&
                curComponentDefinition.getVersion() == componentVersion);

        return CollectionUtils.concatDistinct(
            applyFilterCompatibleConnectionDefinitions(componentDefinition, connectionDefinitions),
            List.of(OptionalUtils.get(componentDefinition.getConnection())));
    }

    @Override
    public TriggerDefinition getTriggerDefinition(String triggerName, String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getTriggers(), Collections.emptyList())
            .stream()
            .filter(triggerDefinition -> triggerName.equalsIgnoreCase(triggerDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' trigger.".formatted(
                        componentName, triggerName)));
    }

    @Override
    public List<? extends TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getTriggers(), Collections.emptyList());
    }

    @Override
    public Property<?> getTriggerProperty(
        String propertyName, String actionName, String componentName, int componentVersion) {

        TriggerDefinition triggerDefinition = getTriggerDefinition(actionName, componentName, componentVersion);

        return CollectionUtils.getFirst(
            OptionalUtils.get(triggerDefinition.getProperties()),
            property -> Objects.equals(propertyName, property.getName()));
    }

    private List<ConnectionDefinition> applyFilterCompatibleConnectionDefinitions(
        ComponentDefinition componentDefinition, List<ConnectionDefinition> connectionDefinitions) {

        return componentDefinition.getFilterCompatibleConnectionDefinitions()
            .map(filterCompatibleConnectionDefinitionsFunction -> filterCompatibleConnectionDefinitionsFunction
                .apply(componentDefinition, connectionDefinitions))
            .orElse(Collections.emptyList());
    }
}
