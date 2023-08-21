
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

package com.bytechef.hermes.definition.registry.component;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.registry.component.factory.ComponentHandlerListFactory;
import com.bytechef.hermes.definition.registry.util.PropertyUtils;
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
        .triggers(trigger("trigger").type(TriggerType.STATIC_WEBHOOK));

    private final List<ComponentDefinition> componentDefinitions;
    private final List<ConnectionDefinition> connectionDefinitions;

    public ComponentDefinitionRegistryImpl(List<ComponentDefinitionFactory> componentDefinitionFactories) {
        this(componentDefinitionFactories, null);
    }

    public ComponentDefinitionRegistryImpl(
        List<ComponentDefinitionFactory> componentDefinitionFactories,
        ComponentHandlerListFactory componentHandlerListFactory) {

        List<ComponentDefinition> componentDefinitions = CollectionUtils.concat(
            CollectionUtils.concat(
                (List<ComponentDefinition>) CollectionUtils.map(
                    componentDefinitionFactories, ComponentDefinitionFactory::getDefinition),
                componentHandlerListFactory == null
                    ? List.of()
                    : CollectionUtils.map(
                        componentHandlerListFactory.getComponentHandlers(),
                        ComponentDefinitionFactory::getDefinition)),
            MANUAL_COMPONENT_DEFINITION);

        this.componentDefinitions = CollectionUtils.sorted(componentDefinitions, this::compare);

        // Validate

        validate(componentDefinitions);

        this.connectionDefinitions = componentDefinitions.stream()
            .map(componentDefinition -> OptionalUtils.orElse(componentDefinition.getConnection(), null))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    @Override
    public ActionDefinition getActionDefinition(
        String componentName, int componentVersion, String actionName) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getActions()
            .orElse(Collections.emptyList())
            .stream()
            .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' action.".formatted(componentName, actionName)));
    }

    @Override
    public List<? extends ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getActions()
            .orElse(Collections.emptyList());
    }

    @Override
    public Property getActionProperty(
        String componentName, int componentVersion, String actionName, String propertyName) {

        ActionDefinition actionDefinition = getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition.getProperties()
            .orElseThrow(IllegalStateException::new)
            .stream()
            .filter(property -> Objects.equals(propertyName, property.getName()))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Authorization getAuthorization(String componentName, int connectionVersion, String authorizationName) {
        ConnectionDefinition connectionDefinition = getComponentConnectionDefinition(componentName, connectionVersion);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    @Override
    public ConnectionDefinition getComponentConnectionDefinition(String componentName, int connectionVersion) {
        return componentDefinitions.stream()
            .filter(componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()) &&
                    connectionDefinition.getVersion() == connectionVersion)
                .orElse(false))
            .findFirst()
            .flatMap(ComponentDefinition::getConnection)
            .orElseThrow(IllegalStateException::new);
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
            componentDefinition = filteredComponentDefinitions.stream()
                .filter(curComponentDefinition -> version == curComponentDefinition.getVersion())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
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
    public List<ConnectionDefinition> getConnectionDefinitions(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return CollectionUtils.concatDistinct(
            applyFilterCompatibleConnectionDefinitions(componentDefinition, connectionDefinitions),
            List.of(OptionalUtils.get(componentDefinition.getConnection())));
    }

    @Override
    public TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getTriggers()
            .orElse(Collections.emptyList())
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
    public Property getTriggerProperty(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getProperties()
            .orElseThrow(IllegalStateException::new)
            .stream()
            .filter(property -> Objects.equals(propertyName, property.getName()))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    }

    private List<ConnectionDefinition> applyFilterCompatibleConnectionDefinitions(
        ComponentDefinition componentDefinition, List<ConnectionDefinition> connectionDefinitions) {

        return componentDefinition.getFilterCompatibleConnectionDefinitions()
            .map(filterCompatibleConnectionDefinitionsFunction -> filterCompatibleConnectionDefinitionsFunction
                .apply(componentDefinition, connectionDefinitions))
            .orElse(Collections.emptyList());
    }

    private int compare(ComponentDefinition o1, ComponentDefinition o2) {
        String o1Name = o1.getName();

        return o1Name.compareTo(o2.getName());
    }

    private void validate(List<ComponentDefinition> componentDefinitions) {
        for (ComponentDefinition componentDefinition : componentDefinitions) {
            List<? extends ActionDefinition> actionDefinitions = OptionalUtils.orElse(
                componentDefinition.getActions(), List.of());

            for (ActionDefinition actionDefinition : actionDefinitions) {
                PropertyUtils.checkInputProperties(OptionalUtils.orElse(actionDefinition.getProperties(), List.of()));
                PropertyUtils.checkOutputProperty(OptionalUtils.orElse(actionDefinition.getOutputSchema(), null));

                if (OptionalUtils.isPresent(actionDefinition.getOutputSchema()) &&
                    OptionalUtils.isPresent(actionDefinition.getOutputSchemaDataSource())) {

                    throw new IllegalStateException("Output schema can be define either as a property or function");
                }

                if (OptionalUtils.isPresent(actionDefinition.getOutputSchema()) &&
                    OptionalUtils.isPresent(actionDefinition.getOutputSchemaDataSource())) {

                    throw new IllegalStateException("Output schema can be define either as a property or function");
                }
            }

            List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.orElse(
                componentDefinition.getTriggers(), List.of());

            for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                PropertyUtils.checkInputProperties(OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()));
                PropertyUtils.checkOutputProperty(OptionalUtils.orElse(triggerDefinition.getOutputSchema(), null));

                if (triggerDefinition.getType() == null) {
                    throw new IllegalStateException(
                        "Trigger type for trigger=%s is not defined".formatted(triggerDefinition.getName()));
                }
            }
        }
    }
}
