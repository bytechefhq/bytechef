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

package com.bytechef.platform.component.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionOutputFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.ActionWorkflowNodeDescriptionFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.registry.ComponentDefinitionRegistry;
import com.bytechef.platform.component.registry.constant.ActionDefinitionErrorType;
import com.bytechef.platform.component.registry.definition.ParametersImpl;
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.registry.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("actionDefinitionService")
public class ActionDefinitionServiceImpl implements ActionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public ActionDefinitionServiceImpl(ComponentDefinitionRegistry componentDefinitionRegistry) {
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> loadDependsOnPaths,
        ComponentConnection connection, @NonNull ActionContext context) {

        ActionPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, actionName, propertyName);

        try {
            return propertiesFunction
                .apply(
                    new ParametersImpl(inputParameters),
                    new ParametersImpl(connection == null ? Map.of() : connection.parameters()),
                    MapUtils.toMap(
                        loadDependsOnPaths,
                        item -> item.substring(item.lastIndexOf(".") + 1),
                        item -> item),
                    context)
                .stream()
                .map(valueProperty -> (Property) Property.toProperty(valueProperty))
                .toList();
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES);
        }
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> loadDependsOnPaths, String searchText,
        ComponentConnection connection, @NonNull ActionContext context) {

        ActionOptionsFunction<?> optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, actionName, propertyName);

        try {
            return optionsFunction
                .apply(
                    new ParametersImpl(inputParameters),
                    new ParametersImpl(connection == null ? Map.of() : connection.parameters()),
                    MapUtils.toMap(
                        loadDependsOnPaths,
                        item -> item.substring(item.lastIndexOf(".") + 1),
                        item -> item),
                    searchText, context)
                .stream()
                .map(Option::new)
                .toList();
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OPTIONS);
        }
    }

    @Override
    public Output executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, ComponentConnection> connections,
        @NonNull ActionContext context) {

        OutputFunction outputFunction = getOutputFunction(componentName, componentVersion, actionName);

        try {
            com.bytechef.component.definition.Output output = switch (outputFunction) {
                case SingleConnectionOutputFunction singleConnectionOutputFunction ->
                    singleConnectionOutputFunction.apply(
                        new ParametersImpl(inputParameters),
                        new ParametersImpl(
                            CollectionUtils.findFirstMapOrElse(
                                connections.values(), ComponentConnection::parameters, Map.of())),
                        context);
                case MultipleConnectionsOutputFunction multipleConnectionsOutputFunction ->
                    multipleConnectionsOutputFunction.apply(new ParametersImpl(inputParameters), connections, context);
                default -> throw new IllegalStateException();
            };

            if (output == null) {
                return null;
            }

            return SchemaUtils.toOutput(
                output,
                (property, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput));
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
        }
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, ComponentConnection> connections,
        @NonNull ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition.getPerform()
            .map(performFunction -> {
                try {
                    return switch (performFunction) {
                        case SingleConnectionPerformFunction singleConnectionPerformFunction ->
                            singleConnectionPerformFunction.apply(
                                new ParametersImpl(inputParameters),
                                new ParametersImpl(
                                    CollectionUtils.findFirstMapOrElse(
                                        connections.values(), ComponentConnection::parameters, Map.of())),
                                context);
                        case MultipleConnectionsPerformFunction multipleConnectionsPerformFunction ->
                            multipleConnectionsPerformFunction.apply(
                                new ParametersImpl(inputParameters), connections, context);
                        default -> throw new IllegalStateException();
                    };
                } catch (Exception e) {
                    throw new ComponentExecutionException(
                        e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
                }
            })
            .orElse(null);
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull ActionContext context) {

        ActionWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction = getWorkflowNodeDescriptionFunction(
            componentName, componentVersion, actionName);

        try {
            return workflowNodeDescriptionFunction.apply(new ParametersImpl(inputParameters), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_WORKFLOW_NODE_DESCRIPTION);
        }
    }

    @Override
    public ActionDefinition getActionDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String actionName) {

        return new ActionDefinition(
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName),
            componentName, componentVersion);
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion) {
        return componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(actionDefinition -> new ActionDefinition(actionDefinition, componentName, componentVersion))
            .toList();
    }

    private ActionOptionsFunction<?> getComponentOptionsFunction(
        String componentName, int componentVersion, String actionName, String propertyName) {

        DynamicOptionsProperty<?> dynamicOptionsProperty = (DynamicOptionsProperty<?>) componentDefinitionRegistry
            .getActionProperty(componentName, componentVersion, actionName, propertyName);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (ActionOptionsFunction<?>) optionsDataSource.getOptions();
    }

    private ActionPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String actionName, String propertyName) {

        DynamicPropertiesProperty dynamicPropertiesProperty = (DynamicPropertiesProperty) componentDefinitionRegistry
            .getActionProperty(componentName, componentVersion, actionName, propertyName);

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        return (ActionPropertiesFunction) propertiesDataSource.getProperties();

    }

    private ActionWorkflowNodeDescriptionFunction getWorkflowNodeDescriptionFunction(
        String componentName, int componentVersion, String actionName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition
            .getWorkflowNodeDescriptionFunction()
            .orElse((inputParameters, context) -> getComponentTitle(componentDefinition) + ": " +
                getActionTitle(actionDefinition));
    }

    private static String getActionTitle(com.bytechef.component.definition.ActionDefinition actionDefinition) {
        return actionDefinition
            .getTitle()
            .orElse(actionDefinition.getName());
    }

    private static String getComponentTitle(ComponentDefinition componentDefinition) {
        return componentDefinition
            .getTitle()
            .orElse(componentDefinition.getName());
    }

    private OutputFunction getOutputFunction(
        String componentName, int componentVersion, String actionName) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition
            .getOutputFunction()
            .orElseGet(() -> {
                if (!actionDefinition.isDefaultOutputFunction()) {
                    throw new IllegalStateException("Default output schema function not allowed");
                }

                PerformFunction performFunction = OptionalUtils.get(actionDefinition.getPerform());

                return switch (performFunction) {
                    case SingleConnectionPerformFunction singleConnectionPerformFunction ->
                        (SingleConnectionOutputFunction) (inputParameters, connectionParameters, context) -> context
                            .output(output -> output.get(
                                singleConnectionPerformFunction.apply(inputParameters, connectionParameters, context)));
                    case MultipleConnectionsPerformFunction multipleConnectionsPerformFunction ->
                        (MultipleConnectionsOutputFunction) (inputParameters, connectionParameters, context) -> context
                            .output(output -> output.get(
                                multipleConnectionsPerformFunction.apply(
                                    inputParameters, connectionParameters, context)));
                    default -> throw new IllegalStateException();
                };
            });
    }
}
