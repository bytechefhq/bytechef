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

package com.bytechef.platform.component;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Component.Registry;
import com.bytechef.platform.component.handler.ComponentHandlerFactory;
import com.bytechef.platform.component.handler.DynamicComponentHandlerFactory;
import com.bytechef.platform.registry.util.PropertyUtils;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
@Component
@SuppressFBWarnings({
    "CT", "EI"
})
public class ComponentDefinitionRegistry {

    private static final Logger log = LoggerFactory.getLogger(ComponentDefinitionRegistry.class);

    private static final ComponentDefinition MANUAL_COMPONENT_DEFINITION = component("manual")
        .title("Manual")
        .icon("path:assets/manual.svg")
        .triggers(trigger("manual").type(TriggerType.STATIC_WEBHOOK));

    private static final ComponentDefinition MISSING_COMPONENT_DEFINITION = component("missing")
        .title("Missing Component")
        .icon("path:assets/missing.svg")
        .version(1)
        .actions(ComponentDsl.action("missing")
            .title("Missing Action"));

    private final List<ComponentDefinition> componentDefinitions;
    private final List<DynamicComponentHandlerFactory> dynamicComponentHandlerListFactories;

    public ComponentDefinitionRegistry(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers,
        List<ComponentHandlerFactory> componentHandlerFactories,
        @Autowired(required = false) List<DynamicComponentHandlerFactory> dynamicComponentHandlerListFactories) {

        @SuppressWarnings("unchecked")
        List<ComponentHandler> mergedComponentHandlers = CollectionUtils.concat(
            componentHandlers,
            CollectionUtils.flatMap(
                componentHandlerFactories,
                componentHandlerListFactory -> (List<ComponentHandler>) componentHandlerListFactory
                    .getComponentHandlers()));

        List<ComponentDefinition> componentDefinitions = CollectionUtils.concat(
            CollectionUtils.map(mergedComponentHandlers, ComponentHandler::getDefinition),
            MANUAL_COMPONENT_DEFINITION, MISSING_COMPONENT_DEFINITION);

        ApplicationProperties.Component component = applicationProperties.getComponent();

        Registry registry = component.getRegistry();

        if (!CollectionUtils.isEmpty(registry.getExclude())) {
            componentDefinitions = componentDefinitions.stream()
                .filter(componentDefinition -> !CollectionUtils.contains(
                    registry.getExclude(), componentDefinition.getName()))
                .toList();
        }

        this.componentDefinitions = CollectionUtils.sort(componentDefinitions, this::compare);

        // Validate

        validate(componentDefinitions);

        this.dynamicComponentHandlerListFactories = dynamicComponentHandlerListFactories == null
            ? List.of() : dynamicComponentHandlerListFactories;
    }

    public ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName) {
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
        String componentName, int componentVersion, String actionName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) throws Exception {

        ActionDefinition actionDefinition = getActionDefinition(componentName, componentVersion, actionName);

        return getProperty(
            propertyName, OptionalUtils.get(actionDefinition.getProperties()), inputParameters, connectionParameters,
            lookupDependsOnPaths, context);
    }

    public Authorization getAuthorization(String componentName, int connectionVersion, String authorizationName) {
        ConnectionDefinition connectionDefinition = getConnectionDefinition(componentName, connectionVersion);

        return OptionalUtils.orElse(connectionDefinition.getAuthorizations(), List.of())
            .stream()
            .filter(authorization -> {
                Authorization.AuthorizationType type = authorization.getType();

                return Objects.equals(type.getName(), authorizationName);
            })
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }

    public ComponentDefinition getComponentDefinition(@NonNull String name, Integer version) {
        ComponentDefinition componentDefinition;

        if (version == null) {
            List<ComponentDefinition> filteredComponentDefinitions = getComponentDefinitions(name);

            componentDefinition = filteredComponentDefinitions.getLast();
        } else {
            componentDefinition = componentDefinitions.stream()
                .filter(
                    curComponentDefinition -> name.equalsIgnoreCase(curComponentDefinition.getName()) &&
                        version == curComponentDefinition.getVersion())
                .findFirst()
                .orElse(null);

            if (componentDefinition == null) {
                componentDefinition = dynamicComponentHandlerListFactories.stream()
                    .map(
                        dynamicComponentHandlerFactory -> dynamicComponentHandlerFactory.fetchComponentHandler(
                            name, version))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "No component with name=" + name + ", version=" + version))
                    .getDefinition();
            }
        }

        return componentDefinition;
    }

    public boolean hasComponentDefinition(String name, Integer version) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(name);

        return componentDefinitions.stream()
            .anyMatch(curComponentDefinition -> (version == null) || (version == curComponentDefinition.getVersion()));
    }

    public List<ComponentDefinition> getComponentDefinitions() {
        return CollectionUtils.sort(
            CollectionUtils.concat(
                componentDefinitions,
                dynamicComponentHandlerListFactories.stream()
                    .flatMap(dynamicComponentHandlerFactory -> CollectionUtils.stream(
                        dynamicComponentHandlerFactory.getComponentHandlers()))
                    .map(ComponentHandler::getDefinition)
                    .toList()),
            this::compare);
    }

    public List<ComponentDefinition> getComponentDefinitions(String name) {
        List<ComponentDefinition> filteredComponentDefinitions = componentDefinitions.stream()
            .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
            .toList();

        if (filteredComponentDefinitions.isEmpty()) {
            filteredComponentDefinitions = dynamicComponentHandlerListFactories.stream()
                .flatMap(dynamicComponentHandlerFactory -> CollectionUtils.stream(
                    dynamicComponentHandlerFactory.getComponentHandlers()))
                .map(ComponentHandler::getDefinition)
                .filter(componentDefinition -> Objects.equals(componentDefinition.getName(), name))
                .toList();
        }

        return filteredComponentDefinitions;
    }

    public ConnectionDefinition getConnectionDefinition(String componentName, int connectionVersion) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(componentName);

        return CollectionUtils.getFirstFilter(
            componentDefinitions,
            componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> connectionDefinition.getVersion() == connectionVersion)
                .orElse(false),
            componentDefinition -> OptionalUtils.get(componentDefinition.getConnection()));
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

    public List<? extends TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return OptionalUtils.orElse(componentDefinition.getTriggers(), Collections.emptyList());
    }

    public Property getTriggerProperty(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        TriggerContext context) throws Exception {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        return getProperty(
            propertyName, OptionalUtils.get(triggerDefinition.getProperties()), inputParameters, connectionParameters,
            lookupDependsOnPaths, context);
    }

    private int compare(ComponentDefinition o1, ComponentDefinition o2) {
        String o1Name = o1.getName();

        return o1Name.compareTo(o2.getName());
    }

    private static Property getProperty(
        String propertyName, List<? extends Property> properties, Parameters inputParameters,
        Parameters connectionParameters, Map<String, String> lookupDependsOnPaths, Context context) throws Exception {

        List<String> subProperties = Arrays.asList(propertyName.split("\\."));

        if (subProperties.size() == 1) {
            if (propertyName.endsWith("]")) {
                Property.ArrayProperty arrayProperty = (Property.ArrayProperty) CollectionUtils.getFirst(
                    properties,
                    curProperty -> Objects.equals(
                        curProperty.getName(), propertyName.substring(0, propertyName.length() - 3)));

                List<? extends Property> items = OptionalUtils.get(arrayProperty.getItems());

                return items.getFirst();
            } else {
                return CollectionUtils.getFirst(properties,
                    property -> Objects.equals(propertyName, property.getName()));
            }
        } else {
            Property firstProperty = CollectionUtils.getFirst(
                properties, property -> Objects.equals(property.getName(), subProperties.getFirst()));

            if (firstProperty instanceof Property.DynamicPropertiesProperty dynamicPropertiesProperty) {
                PropertiesDataSource<?> dynamicPropertiesDataSource = dynamicPropertiesProperty
                    .getDynamicPropertiesDataSource();

                PropertiesDataSource.PropertiesFunction propertiesFunction = dynamicPropertiesDataSource
                    .getProperties();

                List<? extends Property.ValueProperty<?>> dynamicPropertyProperties;

                if (propertiesFunction instanceof PropertiesDataSource.ActionPropertiesFunction actionPropertiesFunction) {
                    dynamicPropertyProperties = actionPropertiesFunction.apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths, (ActionContext) context);
                } else {
                    dynamicPropertyProperties = ((PropertiesDataSource.TriggerPropertiesFunction) propertiesFunction)
                        .apply(inputParameters, connectionParameters, lookupDependsOnPaths, (TriggerContext) context);
                }

                return getProperty(
                    String.join(".", subProperties.subList(1, subProperties.size())),
                    dynamicPropertyProperties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
            } else {
                Property.ObjectProperty objectProperty = (Property.ObjectProperty) CollectionUtils.getFirst(
                    properties, property -> Objects.equals(property.getName(), subProperties.getFirst()));

                for (int i = 1; i < subProperties.size() - 1; i++) {
                    String subProperty = subProperties.get(i);

                    if (subProperty.endsWith("]")) {
                        Property.ArrayProperty arrayProperty = (Property.ArrayProperty) CollectionUtils.getFirst(
                            OptionalUtils.get(objectProperty.getProperties()),
                            curProperty -> Objects.equals(
                                curProperty.getName(), subProperty.substring(0, subProperty.length() - 3)));

                        List<? extends Property> items = OptionalUtils.get(arrayProperty.getItems());

                        objectProperty = (Property.ObjectProperty) items.getFirst();
                    } else {
                        objectProperty = (Property.ObjectProperty) CollectionUtils.getFirst(
                            OptionalUtils.get(objectProperty.getProperties()),
                            curProperty -> Objects.equals(curProperty.getName(), subProperty));
                    }
                }

                return CollectionUtils.getFirst(
                    OptionalUtils.get(objectProperty.getProperties()),
                    curProperty -> Objects.equals(curProperty.getName(), subProperties.getLast()));
            }
        }
    }

    private void validate(List<ComponentDefinition> componentDefinitions) {
        for (ComponentDefinition componentDefinition : componentDefinitions) {
            List<? extends ActionDefinition> actionDefinitions = OptionalUtils.orElse(
                componentDefinition.getActions(), List.of());

            for (ActionDefinition actionDefinition : actionDefinitions) {
                if (log.isTraceEnabled()) {
                    log.trace("Validating %s.%s".formatted(componentDefinition.getName(), actionDefinition.getName()));
                }

                PropertyUtils.checkInputProperties(
                    OptionalUtils.orElse(actionDefinition.getProperties(), List.of()));
                PropertyUtils.checkOutputProperty(
                    OptionalUtils.mapOrElse(actionDefinition.getOutputDefinition(), OutputDefinition::getOutputSchema,
                        null));
            }

            List<? extends TriggerDefinition> triggerDefinitions = OptionalUtils.orElse(
                componentDefinition.getTriggers(), List.of());

            for (TriggerDefinition triggerDefinition : triggerDefinitions) {
                if (log.isTraceEnabled()) {
                    log.trace("Validating %s.%s".formatted(componentDefinition.getName(), triggerDefinition.getName()));
                }

                PropertyUtils.checkInputProperties(OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()));
                PropertyUtils.checkOutputProperty(
                    OptionalUtils.mapOrElse(triggerDefinition.getOutputDefinition(), OutputDefinition::getOutputSchema,
                        null));

                if (triggerDefinition.getType() == null) {
                    throw new IllegalArgumentException(
                        "Trigger type for trigger=%s is not defined".formatted(triggerDefinition.getName()));
                }
            }
        }
    }
}
