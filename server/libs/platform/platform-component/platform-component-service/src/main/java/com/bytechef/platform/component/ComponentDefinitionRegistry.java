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

package com.bytechef.platform.component;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ArrayProperty;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.PropertiesFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Component.Registry;
import com.bytechef.platform.component.handler.DynamicComponentHandlerRegistry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.util.PropertyUtils;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
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

    private final Map<String, Map<Integer, ComponentDefinition>> componentDefinitionsMap = new HashMap<>();
    private final List<DynamicComponentHandlerRegistry> dynamicComponentHandlerRegistries;

    public ComponentDefinitionRegistry(
        ApplicationProperties applicationProperties, List<ComponentHandler> componentHandlers,
        Supplier<List<ComponentHandlerEntry>> componentHandlerEntriesSupplier,
        List<DynamicComponentHandlerRegistry> dynamicComponentHandlerRegistries) {

        List<ComponentHandler> mergedComponentHandlers = CollectionUtils.concat(
            componentHandlers,
            CollectionUtils.map(componentHandlerEntriesSupplier.get(), ComponentHandlerEntry::componentHandler));

        List<ComponentDefinition> componentDefinitions = CollectionUtils.concat(
            CollectionUtils.map(mergedComponentHandlers, ComponentHandler::getDefinition),
            MANUAL_COMPONENT_DEFINITION, MISSING_COMPONENT_DEFINITION);

        ApplicationProperties.Component component = applicationProperties.getComponent();

        Registry registry = component.getRegistry();

        List<String> exclude = registry.getExclude();

        if (!CollectionUtils.isEmpty(exclude)) {
            componentDefinitions = componentDefinitions.stream()
                .filter(componentDefinition -> !CollectionUtils.contains(exclude, componentDefinition.getName()))
                .toList();
        }

        // Validate

        validate(componentDefinitions);

        for (ComponentDefinition componentDefinition : componentDefinitions) {
            this.componentDefinitionsMap
                .computeIfAbsent(StringUtils.upperCase(componentDefinition.getName()), key -> new HashMap<>())
                .put(componentDefinition.getVersion(), componentDefinition);
        }

        this.dynamicComponentHandlerRegistries = dynamicComponentHandlerRegistries;
    }

    public Optional<Authorization> fetchAuthorization(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        return fetchConnectionDefinition(componentName, connectionVersion)
            .flatMap(ConnectionDefinition::getAuthorizations)
            .orElse(List.of())
            .stream()
            .filter(authorization -> authorization.getType() == authorizationType)
            .map(authorization -> (Authorization) authorization)
            .findFirst();
    }

    public Optional<ComponentDefinition> fetchComponentDefinition(String name, @Nullable Integer version) {
        ComponentDefinition componentDefinition = null;

        if (version == null || version == -1) {
            List<ComponentDefinition> filteredComponentDefinitions = getComponentDefinitions(name);

            if (!filteredComponentDefinitions.isEmpty()) {
                componentDefinition = filteredComponentDefinitions.getLast();
            }
        } else {
            Map<Integer, ComponentDefinition> componentDefinitionMap = componentDefinitionsMap.get(
                StringUtils.upperCase(name));

            if (componentDefinitionMap != null) {
                componentDefinition = componentDefinitionMap.get(version);
            }

            if (componentDefinition == null) {
                componentDefinition = dynamicComponentHandlerRegistries.stream()
                    .map(dynamicComponentHandlerRegistry -> dynamicComponentHandlerRegistry.fetchComponentHandler(
                        name, version))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map(ComponentHandler::getDefinition)
                    .orElse(null);
            }
        }

        return Optional.ofNullable(componentDefinition);
    }

    public Optional<ConnectionDefinition> fetchConnectionDefinition(String componentName, int connectionVersion) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(componentName);

        return componentDefinitions.stream()
            .filter(componentDefinition -> componentDefinition.getConnection()
                .map(connectionDefinition -> connectionDefinition.getVersion() == connectionVersion)
                .orElse(false))
            .findFirst()
            .flatMap(ComponentDefinition::getConnection);
    }

    public List<ComponentDefinition> getComponentDefinitions() {
        return CollectionUtils.sort(
            CollectionUtils.concat(
                componentDefinitionsMap.values()
                    .stream()
                    .flatMap(map -> CollectionUtils.stream(map.values()))
                    .toList(),
                dynamicComponentHandlerRegistries.stream()
                    .flatMap(dynamicComponentHandlerRegistry -> CollectionUtils.stream(
                        dynamicComponentHandlerRegistry.getComponentHandlers()))
                    .map(ComponentHandler::getDefinition)
                    .toList()),
            this::compare);
    }

    public ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName) {
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

    public Authorization getAuthorization(
        String componentName, int connectionVersion, AuthorizationType authorizationType) {

        ConnectionDefinition connectionDefinition = getConnectionDefinition(componentName, connectionVersion);

        return OptionalUtils.orElse(connectionDefinition.getAuthorizations(), List.of())
            .stream()
            .filter(authorization -> {
                AuthorizationType curAuthorizationType = authorization.getType();

                return curAuthorizationType == authorizationType;
            })
            .findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }

    public ClusterElementDefinition<?> getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        ComponentDefinition componentDefinition = getComponentDefinition(componentName, componentVersion);

        return componentDefinition.getClusterElements()
            .orElse(Collections.emptyList())
            .stream()
            .filter(clusterElementDefinition -> clusterElementName.equalsIgnoreCase(clusterElementDefinition.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "The component '%s' does not contain the '%s' cluster element.".formatted(
                        componentName, clusterElementName)));
    }

    public Property getClusterElementProperty(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ClusterElementContext context) throws Exception {

        ClusterElementDefinition<?> clusterElementDefinition = getClusterElementDefinition(
            componentName, componentVersion, clusterElementName);

        List<? extends Property> properties = clusterElementDefinition.getProperties()
            .orElseThrow(() -> new IllegalArgumentException(
                "The cluster element '%s' in component '%s' does not have any properties defined.".formatted(
                    clusterElementName, componentName)));

        return getProperty(
            propertyName, properties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
    }

    public ComponentDefinition getComponentDefinition(String name, @Nullable Integer version) {
        return fetchComponentDefinition(name, version)
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Component definition with name '%s' and version '%s' not found", name, version)));
    }

    public List<ComponentDefinition> getComponentDefinitions(String name) {
        Map<Integer, ComponentDefinition> integerComponentDefinitionMap = componentDefinitionsMap.get(
            StringUtils.upperCase(name));

        List<ComponentDefinition> filteredComponentDefinitions = List.of();

        if (integerComponentDefinitionMap != null) {
            filteredComponentDefinitions = integerComponentDefinitionMap.values()
                .stream()
                .toList();
        }

        if (filteredComponentDefinitions.isEmpty()) {
            filteredComponentDefinitions = dynamicComponentHandlerRegistries.stream()
                .flatMap(dynamicComponentHandlerRegistry -> CollectionUtils.stream(
                    dynamicComponentHandlerRegistry.getComponentHandlers()))
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

    public boolean hasComponentDefinition(String name, @Nullable Integer version) {
        List<ComponentDefinition> componentDefinitions = getComponentDefinitions(name);

        return componentDefinitions.stream()
            .anyMatch(curComponentDefinition -> (version == null) || (version == curComponentDefinition.getVersion()));
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
                ArrayProperty arrayProperty = (ArrayProperty) CollectionUtils.getFirst(
                    properties,
                    curProperty -> Objects.equals(
                        curProperty.getName(), propertyName.substring(0, propertyName.length() - 3)));

                List<? extends Property> items = OptionalUtils.get(arrayProperty.getItems());

                return items.getFirst();
            } else {
                return CollectionUtils.getFirst(
                    properties, property -> Objects.equals(propertyName, property.getName()),
                    "Property '%s' not found in component properties".formatted(propertyName));
            }
        } else {
            String firstSubPropertyName = getFirstSubPropertyName(subProperties);

            Property firstProperty = CollectionUtils.getFirst(
                properties, property -> Objects.equals(property.getName(), firstSubPropertyName));

            if (firstProperty instanceof Property.DynamicPropertiesProperty dynamicPropertiesProperty) {
                PropertiesDataSource<?> dynamicPropertiesDataSource = dynamicPropertiesProperty
                    .getDynamicPropertiesDataSource();

                PropertiesDataSource.BasePropertiesFunction propertiesFunction = dynamicPropertiesDataSource
                    .getProperties();

                List<? extends Property.ValueProperty<?>> dynamicPropertyProperties;

                if (propertiesFunction instanceof ActionDefinition.PropertiesFunction actionPropertiesFunction) {
                    dynamicPropertyProperties = actionPropertiesFunction.apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths, (ActionContext) context);
                } else if (propertiesFunction instanceof ClusterElementDefinition.PropertiesFunction clusterElementPropertiesFunction) {

                    dynamicPropertyProperties = clusterElementPropertiesFunction.apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths,
                        (ClusterElementContext) context);
                } else {
                    dynamicPropertyProperties = ((PropertiesFunction) propertiesFunction).apply(
                        inputParameters, connectionParameters, lookupDependsOnPaths, (TriggerContext) context);
                }

                return getProperty(
                    String.join(".", subProperties.subList(1, subProperties.size())),
                    dynamicPropertyProperties, inputParameters, connectionParameters, lookupDependsOnPaths, context);
            } else if (firstProperty instanceof ArrayProperty arrayProperty) {
                List<? extends Property.ValueProperty<?>> items = OptionalUtils.get(arrayProperty.getItems());

                if (items.getFirst() instanceof ObjectProperty objectProperty) {
                    items = OptionalUtils.get(objectProperty.getProperties());
                }

                return getProperty(
                    String.join(".", subProperties.subList(1, subProperties.size())), items, inputParameters,
                    connectionParameters, lookupDependsOnPaths, context);
            } else {
                ObjectProperty objectProperty = (ObjectProperty) CollectionUtils.getFirst(
                    properties, property -> Objects.equals(property.getName(), subProperties.getFirst()));

                for (int i = 1; i < subProperties.size() - 1; i++) {
                    String subProperty = subProperties.get(i);

                    if (subProperty.endsWith("]")) {
                        ArrayProperty arrayProperty = (ArrayProperty) CollectionUtils.getFirst(
                            OptionalUtils.get(objectProperty.getProperties()),
                            curProperty -> Objects.equals(
                                curProperty.getName(), subProperty.substring(0, subProperty.length() - 3)));

                        List<? extends Property> items = OptionalUtils.get(arrayProperty.getItems());

                        objectProperty = (ObjectProperty) items.getFirst();
                    } else {
                        objectProperty = (ObjectProperty) CollectionUtils.getFirst(
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

    private static String getFirstSubPropertyName(List<String> subProperties) {
        String firstSubPropertyName = subProperties.getFirst();

        if (firstSubPropertyName.endsWith("]")) {
            firstSubPropertyName = firstSubPropertyName.substring(0, firstSubPropertyName.indexOf("["));
        }
        return firstSubPropertyName;
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
