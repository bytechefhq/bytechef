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

package com.bytechef.platform.component.service;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.ActionDefinition.ProcessErrorResponseFunction;
import com.bytechef.component.definition.ActionDefinition.SingleConnectionPerformFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.PropertiesDataSource.ActionPropertiesFunction;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.util.WorkflowNodeDescriptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        ComponentConnection connection, ActionContext context) {

        ConvertResult convertResult = convert(inputParameters, lookupDependsOnPaths, connection);

        try {
            ActionPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
                componentName, componentVersion, actionName, propertyName, convertResult.inputParameters,
                convertResult.connectionParameters, convertResult.lookupDependsOnPathsMap, context);

            return propertiesFunction
                .apply(
                    convertResult.inputParameters, convertResult.connectionParameters,
                    convertResult.lookupDependsOnPathsMap,
                    context)
                .stream()
                .map(valueProperty -> (Property) Property.toProperty(valueProperty))
                .toList();
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ConfigurationException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES);
        }
    }

    @Override
    public OutputResponse executeMultipleConnectionsOutput(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> connections, Map<String, ?> extensions, ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition.getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .map(f -> (MultipleConnectionsOutputFunction) f)
            .map(multipleConnectionsOutputFunction -> {
                try {
                    BaseOutputDefinition.OutputResponse outputResponse = multipleConnectionsOutputFunction.apply(
                        ParametersFactory.createParameters(inputParameters), connections,
                        ParametersFactory.createParameters(extensions), context);

                    return toOutputResponse(outputResponse);
                } catch (Exception e) {
                    throw new ConfigurationException(
                        e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
                }
            })
            .orElse(null);
    }

    @Override
    public Object executeMultipleConnectionsPerform(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> connections, Map<String, ?> extensions, ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        MultipleConnectionsPerformFunction multipleConnectionsPerformFunction =
            (MultipleConnectionsPerformFunction) OptionalUtils.get(actionDefinition.getPerform());

        try {
            return multipleConnectionsPerformFunction.apply(
                ParametersFactory.createParameters(inputParameters), connections,
                ParametersFactory.createParameters(extensions), context);
        } catch (Exception e) {
            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        ComponentConnection connection, ActionContext context) {

        try {
            ConvertResult convertResult = convert(inputParameters, lookupDependsOnPaths, connection);

            ActionOptionsFunction<?> optionsFunction = getComponentOptionsFunction(
                componentName, componentVersion, actionName, propertyName, convertResult.inputParameters(),
                convertResult.connectionParameters(), convertResult.lookupDependsOnPathsMap(), context);

            return optionsFunction
                .apply(
                    convertResult.inputParameters(), convertResult.connectionParameters(),
                    convertResult.lookupDependsOnPathsMap(), searchText, context)
                .stream()
                .map(Option::new)
                .toList();
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ConfigurationException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OPTIONS);
        }
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String actionName, int statusCode, Object body,
        Context actionContext) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        try {
            Optional<ProcessErrorResponseFunction> processErrorResponse = actionDefinition.getProcessErrorResponse();

            if (processErrorResponse.isPresent()) {
                return processErrorResponse.get()
                    .apply(statusCode, body, actionContext);
            } else {
                return ProviderException.getProviderException(statusCode, body);
            }
        } catch (Exception e) {
            throw new ExecutionException(e, ActionDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
        }
    }

    @Override
    public OutputResponse executeSingleConnectionOutput(
        String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters, ComponentConnection connection, ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition.getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .map(f -> (OutputFunction) f)
            .map(outputFunction -> {
                try {
                    BaseOutputDefinition.OutputResponse outputResponse = outputFunction.apply(
                        ParametersFactory.createParameters(inputParameters),
                        ParametersFactory.createParameters(
                            connection == null ? Map.of() : connection.getConnectionParameters()),
                        context);

                    return toOutputResponse(outputResponse);
                } catch (Exception e) {
                    if (e instanceof ProviderException) {
                        throw (ProviderException) e;
                    }

                    throw new ConfigurationException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
                }
            })
            .orElse(null);
    }

    @Override
    public Object executeSingleConnectionPerform(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection connection, ActionContext context) throws ExecutionException {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        SingleConnectionPerformFunction singleConnectionPerformFunction =
            (SingleConnectionPerformFunction) OptionalUtils.get(actionDefinition.getPerform());

        try {
            return singleConnectionPerformFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                connection == null
                    ? null : ParametersFactory.createParameters(connection.getConnectionParameters()),
                context);
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters,
        ActionContext context) {

        com.bytechef.component.definition.ActionDefinition.ActionWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction =
            getWorkflowNodeDescriptionFunction(
                componentName, componentVersion, actionName);

        try {
            return workflowNodeDescriptionFunction.apply(ParametersFactory.createParameters(inputParameters), context);
        } catch (Exception e) {
            throw new ConfigurationException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_WORKFLOW_NODE_DESCRIPTION);
        }
    }

    @Override
    public ActionDefinition getActionDefinition(String componentName, int componentVersion, String actionName) {
        if (componentDefinitionRegistry.hasComponentDefinition(componentName, componentVersion)) {
            return new ActionDefinition(
                componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName),
                componentName, componentVersion);
        }

        return new ActionDefinition(
            componentDefinitionRegistry.getActionDefinition("missing", 1, "missing"), componentName, componentVersion);
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(String componentName, int componentVersion) {
        return componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(actionDefinition -> new ActionDefinition(actionDefinition, componentName, componentVersion))
            .toList();
    }

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName) {
        ActionDefinition actionDefinition = getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition.isOutputFunctionDefined();
    }

    @Override
    public boolean isSingleConnectionPerform(
        String componentName, int componentVersion, String actionName) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return OptionalUtils.get(actionDefinition.getPerform()) instanceof SingleConnectionPerformFunction;
    }

    private static ConvertResult convert(
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, ComponentConnection connection) {

        return new ConvertResult(
            ParametersFactory.createParameters(inputParameters),
            ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
            getLookupDependsOnPathsMap(lookupDependsOnPaths));
    }

    private ActionOptionsFunction<?> getComponentOptionsFunction(
        String componentName, int componentVersion, String actionName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) throws Exception {

        DynamicOptionsProperty<?> dynamicOptionsProperty = (DynamicOptionsProperty<?>) componentDefinitionRegistry
            .getActionProperty(
                componentName, componentVersion, actionName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (ActionOptionsFunction<?>) optionsDataSource.getOptions();
    }

    private ActionPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String actionName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) throws Exception {

        DynamicPropertiesProperty dynamicPropertiesProperty = (DynamicPropertiesProperty) componentDefinitionRegistry
            .getActionProperty(
                componentName, componentVersion, actionName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        return (ActionPropertiesFunction) propertiesDataSource.getProperties();
    }

    private com.bytechef.component.definition.ActionDefinition.ActionWorkflowNodeDescriptionFunction
        getWorkflowNodeDescriptionFunction(String componentName, int componentVersion, String actionName) {

        if (!componentDefinitionRegistry.hasComponentDefinition(componentName, componentVersion)) {
            componentName = "missing";
            componentVersion = 1;
            actionName = "missing";
        }

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);
        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        return actionDefinition.getWorkflowNodeDescription()
            .orElse((inputParameters, context) -> WorkflowNodeDescriptionUtils.renderComponentProperties(
                inputParameters, OptionalUtils.orElse(componentDefinition.getTitle(), componentDefinition.getName()),
                OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName()),
                OptionalUtils.orElse(actionDefinition.getDescription(), null)));
    }

    private static Map<String, String> getLookupDependsOnPathsMap(List<String> lookupDependsOnPaths) {
        return MapUtils.toMap(lookupDependsOnPaths, item -> item.substring(item.lastIndexOf(".") + 1), item -> item);
    }

    private static OutputResponse toOutputResponse(BaseOutputDefinition.OutputResponse outputResponse) {
        if (outputResponse == null) {
            return null;
        }

        return SchemaUtils.toOutput(
            outputResponse, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);
    }

    private record ConvertResult(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPathsMap) {
    }
}
