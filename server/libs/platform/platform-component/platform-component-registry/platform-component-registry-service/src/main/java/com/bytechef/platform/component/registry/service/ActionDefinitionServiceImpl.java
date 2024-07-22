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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionOutputFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.ActionWorkflowNodeDescriptionFunction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.OutputResponse;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.registry.ComponentDefinitionRegistry;
import com.bytechef.platform.component.registry.definition.ParametersImpl;
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.exception.ActionDefinitionErrorType;
import com.bytechef.platform.registry.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
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
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths,
        ComponentConnection connection, @NonNull ActionContext context) {

        ActionPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, actionName, propertyName);

        try {
            return propertiesFunction
                .apply(
                    new ParametersImpl(inputParameters),
                    new ParametersImpl(connection == null ? Map.of() : connection.parameters()),
                    getLookupDependsOnPathsMap(lookupDependsOnPaths), context)
                .stream()
                .map(valueProperty -> (Property) Property.toProperty(valueProperty))
                .toList();
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ComponentExecutionException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES);
        }
    }

    @Override
    public Output executeMultipleConnectionsOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, ComponentConnection> connections,
        @NonNull ActionContext context) {

        MultipleConnectionsOutputFunction multipleConnectionsOutputFunction =
            (MultipleConnectionsOutputFunction) getOutputFunction(componentName, componentVersion, actionName);

        try {
            OutputResponse outputResponse = multipleConnectionsOutputFunction.apply(
                new ParametersImpl(inputParameters), connections, context);

            if (outputResponse == null) {
                return null;
            }

            return SchemaUtils.toOutput(
                outputResponse,
                (property, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput));
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
        }
    }

    @Override
    public Object executeMultipleConnectionsPerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @NonNull Map<String, ComponentConnection> connections,
        @NonNull ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        MultipleConnectionsPerformFunction multipleConnectionsPerformFunction =
            (MultipleConnectionsPerformFunction) OptionalUtils.get(actionDefinition.getPerform());

        try {
            return multipleConnectionsPerformFunction.apply(
                new ParametersImpl(inputParameters), connections, context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, @NonNull List<String> lookupDependsOnPaths, String searchText,
        ComponentConnection connection, @NonNull ActionContext context) {

        ActionOptionsFunction<?> optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, actionName, propertyName);

        try {
            return optionsFunction
                .apply(
                    new ParametersImpl(inputParameters),
                    new ParametersImpl(connection == null ? Map.of() : connection.parameters()),
                    getLookupDependsOnPathsMap(lookupDependsOnPaths), searchText, context)
                .stream()
                .map(Option::new)
                .toList();
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ComponentExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OPTIONS);
        }
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode, Object body,
        Context actionContext) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        try {
            Optional<com.bytechef.component.definition.ActionDefinition.ProcessErrorResponseFunction> processErrorResponse =
                actionDefinition.getProcessErrorResponse();
            if (processErrorResponse.isPresent()) {
                return processErrorResponse.get()
                    .apply(statusCode, body, actionContext);
            } else {
                return ProviderException.getProviderException(statusCode, body);
            }
        } catch (Exception e) {
            throw new ComponentExecutionException(e, ActionDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
        }
    }

    @Override
    public Output executeSingleConnectionOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        SingleConnectionOutputFunction singleConnectionOutputFunction =
            (SingleConnectionOutputFunction) getOutputFunction(componentName, componentVersion, actionName);

        try {
            OutputResponse outputResponse = singleConnectionOutputFunction.apply(
                new ParametersImpl(inputParameters),
                new ParametersImpl(getConnectionParameters(connection)), context);

            if (outputResponse == null) {
                return null;
            }

            return SchemaUtils.toOutput(
                outputResponse,
                (property, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput));
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ComponentExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
        }
    }

    @Override
    public Object executeSingleConnectionPerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, @Nullable ComponentConnection connection,
        @NonNull ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        SingleConnectionPerformFunction singleConnectionPerformFunction =
            (SingleConnectionPerformFunction) OptionalUtils.get(actionDefinition.getPerform());

        try {
            return singleConnectionPerformFunction.apply(
                new ParametersImpl(inputParameters), new ParametersImpl(getConnectionParameters(connection)), context);
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ComponentExecutionException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
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
        if (componentDefinitionRegistry.hasComponentDefinition(componentName, componentVersion)) {
            return new ActionDefinition(
                componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName),
                componentName, componentVersion);
        }

        return new ActionDefinition(
            componentDefinitionRegistry.getActionDefinition("missing", 1, "missing"),
            componentName, componentVersion);

    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion) {
        return componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(actionDefinition -> new ActionDefinition(actionDefinition, componentName, componentVersion))
            .toList();
    }

    @Override
    public boolean isSingleConnectionPerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return OptionalUtils.get(actionDefinition.getPerform()) instanceof SingleConnectionPerformFunction;
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

        if (!componentDefinitionRegistry.hasComponentDefinition(componentName, componentVersion)) {
            componentName = "missing";
            componentVersion = 1;
            actionName = "missing";

        }

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);
        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition
            .getWorkflowNodeDescription()
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

    private static ParametersImpl getConnectionParameters(ComponentConnection componentConnection) {
        if (componentConnection == null) {
            return new ParametersImpl(Map.of());
        }

        return new ParametersImpl(
            MapUtils.concatDifferentTypes(
                componentConnection.getParameters(),
                Map.of(Authorization.AUTHORIZATION_TYPE, componentConnection.authorizationName())));
    }

    private static Map<String, String> getLookupDependsOnPathsMap(List<String> lookupDependsOnPaths) {
        return MapUtils.toMap(lookupDependsOnPaths, item -> item.substring(item.lastIndexOf(".") + 1), item -> item);
    }

    private OutputFunction getOutputFunction(String componentName, int componentVersion, String actionName) {
        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition
            .getOutput()
            .orElseGet(() -> {
                if (!actionDefinition.isDynamicOutput()) {
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
