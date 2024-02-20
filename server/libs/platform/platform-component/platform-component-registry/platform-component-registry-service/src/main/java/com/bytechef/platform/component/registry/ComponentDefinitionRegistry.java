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

package com.bytechef.platform.component.registry;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.definition.ComponentDSL.trigger;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentDefinitionFactory;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Output;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.definition.ScriptComponentDefinition;
import com.bytechef.platform.component.definition.ScriptComponentDefinition.FilterConnectionDefinitionPredicate;
import com.bytechef.platform.component.registry.factory.ComponentHandlerListFactory;
import com.bytechef.platform.registry.util.PropertyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings({
    "CT", "EI"
})
public class ComponentDefinitionRegistry {

    private static final ComponentDefinition MANUAL_COMPONENT_DEFINITION = component("manual")
        .title("Manual")
        .triggers(trigger("manual").type(TriggerType.STATIC_WEBHOOK));

    private final List<ComponentDefinition> componentDefinitions;

    public ComponentDefinitionRegistry(
        List<ComponentDefinitionFactory> componentDefinitionFactories,
        @Autowired(required = false) ComponentHandlerListFactory componentHandlerListFactory) {

        List<ComponentDefinitionFactory> mergedComponentDefinitionFactories = CollectionUtils.concat(
            componentDefinitionFactories,
            componentHandlerListFactory == null
                ? List.of()
                : CollectionUtils.map(
                    componentHandlerListFactory.getComponentHandlers(),
                    componentHandler -> (ComponentDefinitionFactory) componentHandler));

        List<ComponentDefinition> componentDefinitions = CollectionUtils.concat(
            CollectionUtils.map(mergedComponentDefinitionFactories, ComponentDefinitionFactory::getDefinition),
            MANUAL_COMPONENT_DEFINITION);

        this.componentDefinitions = CollectionUtils.sort(componentDefinitions, this::compare);

        // Validate

        validate(componentDefinitions);
    }

    public ActionDefinition getActionDefinition(
        String componentName, int componentVersion, String actionName) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition
            .getActions()
            .orElse(Collections.emptyList())
            .stream()
            .filter(actionDefinition -> actionName.equalsIgnoreCase(actionDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' action.".formatted(componentName, actionName)));
    }

    public List<? extends ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getActions(), Collections.emptyList());
    }

    public Property getActionProperty(
        String componentName, int componentVersion, String actionName, String propertyName) {

        ActionDefinition actionDefinition = getActionDefinition(componentName, componentVersion, actionName);

        return getProperty(propertyName, OptionalUtils.get(actionDefinition.getProperties()));
    }

    public Authorization getAuthorization(String componentName, String authorizationName) {
        ConnectionDefinition connectionDefinition = getConnectionDefinition(componentName);

        return connectionDefinition.getAuthorization(authorizationName);
    }

    public ComponentDefinition getComponentDefinition(String name, Integer version) {
        ComponentDefinition componentDefinition;

        List<ComponentDefinition> filteredComponentDefinitions = componentDefinitions
            .stream()
            .filter(curComponentDefinition -> name.equalsIgnoreCase(curComponentDefinition.getName()))
            .toList();

        if (version == null) {
            componentDefinition = filteredComponentDefinitions.getLast();
        } else {
            componentDefinition = filteredComponentDefinitions
                .stream()
                .filter(curComponentDefinition -> version == curComponentDefinition.getVersion())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        }

        return componentDefinition;
    }

    public List<? extends ComponentDefinition> getComponentDefinitions(String name) {
        return componentDefinitions
            .stream()
            .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
            .toList();
    }

    public List<ComponentDefinition> getComponentDefinitions() {
        return componentDefinitions;
    }

    public ConnectionDefinition getConnectionDefinition(String componentName) {
        return CollectionUtils.getFirst(
            componentDefinitions,
            componentDefinition -> componentName.equalsIgnoreCase(componentDefinition.getName()),
            componentDefinition -> OptionalUtils.get(componentDefinition.getConnection()));
    }

    public List<ComponentDefinition> getConnectionComponentDefinitions(
        String componentName, int componentVersion) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        if (componentDefinition instanceof ScriptComponentDefinition scriptComponentDefinition) {
            FilterConnectionDefinitionPredicate filterConnectionDefinitionPredicate = scriptComponentDefinition
                .getFilterConnectionDefinition();

            return this.componentDefinitions
                .stream()
                .filter(curComponentDefinition -> filterConnectionDefinitionPredicate.apply(componentDefinition))
                .toList();
        } else {
            return List.of(componentDefinition);
        }
    }

    public TriggerDefinition getTriggerDefinition(String componentName, int componentVersion, String triggerName) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition
            .getTriggers()
            .orElse(Collections.emptyList())
            .stream()
            .filter(triggerDefinition -> triggerName.equalsIgnoreCase(triggerDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' trigger.".formatted(
                        componentName, triggerName)));
    }

    public List<TriggerDefinition> getTriggerDefinitions() {
        return componentDefinitions
            .stream()
            .flatMap(componentDefinition -> CollectionUtils.stream(
                OptionalUtils.orElse(componentDefinition.getTriggers(), List.of())))
            .distinct()
            .map(triggerDefinition -> (TriggerDefinition) triggerDefinition)
            .toList();
    }

    public List<? extends TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getTriggers(), Collections.emptyList());
    }

    public Property getTriggerProperty(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        return getProperty(propertyName, OptionalUtils.get(triggerDefinition.getProperties()));
    }

    private int compare(ComponentDefinition o1, ComponentDefinition o2) {
        String o1Name = o1.getName();

        return o1Name.compareTo(o2.getName());
    }

    private static Property getProperty(String propertyName, List<? extends Property> properties) {
        String[] subProperties = propertyName.split("\\.");

        if (subProperties.length == 1) {
            return CollectionUtils.getFirst(properties, property -> Objects.equals(propertyName, property.getName()));
        } else {
            // TODO add recursion to fetch any level

            Property.ObjectProperty objectProperty = (Property.ObjectProperty) CollectionUtils.getFirst(
                properties, property -> Objects.equals(property.getName(), subProperties[0]));

            return CollectionUtils.getFirst(
                OptionalUtils.get(objectProperty.getProperties()),
                property -> Objects.equals(property.getName(), subProperties[1]));
        }
    }

    private void validate(List<ComponentDefinition> componentDefinitions) {
        for (ComponentDefinition componentDefinition : componentDefinitions) {
            List<? extends ActionDefinition> actionDefinitions = OptionalUtils.orElse(
                componentDefinition.getActions(), List.of());

            for (ActionDefinition actionDefinition : actionDefinitions) {
                PropertyUtils.checkInputProperties(
                    OptionalUtils.orElse(actionDefinition.getProperties(), List.of()));
                PropertyUtils.checkOutputProperty(
                    OptionalUtils.mapOrElse(actionDefinition.getOutput(), Output::getOutputSchema, null));
            }

            List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.orElse(
                componentDefinition.getTriggers(), List.of());

            for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                PropertyUtils.checkInputProperties(OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()));
                PropertyUtils.checkOutputProperty(
                    OptionalUtils.mapOrElse(triggerDefinition.getOutput(), Output::getOutputSchema, null));

                if (triggerDefinition.getType() == null) {
                    throw new IllegalArgumentException(
                        "Trigger type for trigger=%s is not defined".formatted(triggerDefinition.getName()));
                }
            }
        }
    }
}
