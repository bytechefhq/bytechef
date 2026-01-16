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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ClusterElementDefinition.OptionsFunction;
import com.bytechef.component.definition.ClusterElementDefinition.PropertiesFunction;
import com.bytechef.component.definition.ClusterElementDefinition.WorkflowNodeDescriptionFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ActionContextAdapater;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.ClusterElementDefinitionErrorType;
import com.bytechef.platform.component.util.TokenRefreshHelper;
import com.bytechef.platform.util.WorkflowNodeDescriptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("clusterElementDefinitionService")
public class ClusterElementDefinitionServiceImpl implements ClusterElementDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextFactory contextFactory;
    private final TokenRefreshHelper tokenRefreshHelper;

    @SuppressFBWarnings("EI")
    public ClusterElementDefinitionServiceImpl(
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        TokenRefreshHelper tokenRefreshHelper) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
        this.tokenRefreshHelper = tokenRefreshHelper;
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection) {

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, clusterElementContext,
            ClusterElementDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES,
            (componentConnection1, clusterElementContext1) -> executeDynamicProperties(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters,
                lookupDependsOnPaths, componentConnection1, clusterElementContext1),
            componentConnection1 -> contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, componentConnection1, true));
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection) {

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, true);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, clusterElementContext,
            ClusterElementDefinitionErrorType.EXECUTE_OPTIONS,
            (componentConnection1, clusterElementContext1) -> executeOptions(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters,
                lookupDependsOnPaths, searchText, componentConnection1, clusterElementContext1),
            componentConnection1 -> contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, componentConnection1, true));
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String componentOperationName, int statusCode, Object body) {

        return executeProcessErrorResponse(
            componentName, componentVersion, componentOperationName, statusCode, body,
            contextFactory.createClusterElementContext(
                componentName, componentVersion, componentOperationName, null, false));
    }

    @Override
    public Object executeTool(
        String componentName, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, null);

        return executeTool(
            componentName, componentDefinition.getVersion(), clusterElementName, inputParameters, componentConnection,
            editorEnvironment);
    }

    @Override
    public Object executeTool(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, editorEnvironment);

        return tokenRefreshHelper.executeSingleConnectionFunction(
            componentName, componentVersion, componentConnection, clusterElementContext,
            ClusterElementDefinitionErrorType.EXECUTE_PERFORM,
            (componentConnection1, clusterElementContext1) -> executeTool(
                componentName, componentVersion, clusterElementName, inputParameters, componentConnection1,
                clusterElementContext1),
            componentConnection1 -> contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, componentConnection1, editorEnvironment));
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters) {

        return executeWorkflowNodeDescription(
            componentName, componentVersion, clusterElementName, inputParameters,
            contextFactory.createClusterElementContext(
                componentName, componentVersion, clusterElementName, null, true));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getClusterElement(String componentName, int componentVersion, String clusterElementName) {
        ComponentClusterElementDefinitionResult result = getComponentClusterElementDefinition(
            componentName, componentVersion, clusterElementName);

        return (T) result.clusterElementDefinition.getElement();
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(String componentName, String clusterElementName) {
        ComponentClusterElementDefinitionResult result = getComponentClusterElementDefinition(
            componentName, null, clusterElementName);

        ComponentDefinition componentDefinition = result.componentDefinition;

        return new ClusterElementDefinition(
            result.clusterElementDefinition, componentDefinition.getName(), componentDefinition.getVersion(),
            getIcon(componentDefinition));
    }

    @Override
    public ClusterElementDefinition getClusterElementDefinition(
        String componentName, int componentVersion, String clusterElementName) {

        ComponentClusterElementDefinitionResult result = getComponentClusterElementDefinition(
            componentName, componentVersion, clusterElementName);

        ComponentDefinition componentDefinition = result.componentDefinition;

        return new ClusterElementDefinition(
            result.clusterElementDefinition, componentDefinition.getName(), componentVersion,
            getIcon(componentDefinition));
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(ClusterElementType clusterElementType) {
        return componentDefinitionRegistry.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> componentDefinition.getClusterElements()
                .isPresent())
            .flatMap(componentDefinition -> CollectionUtils.stream(
                componentDefinition.getClusterElements()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Cluster elements not found in component %s".formatted(componentDefinition.getName())))
                    .stream()
                    .filter(clusterElementDefinition -> clusterElementType.equals(clusterElementDefinition.getType()))
                    .map(clusterElementDefinition -> new ClusterElementDefinition(
                        clusterElementDefinition, componentDefinition.getName(), componentDefinition.getVersion(),
                        getIcon(componentDefinition)))
                    .toList()))
            .distinct()
            .toList();
    }

    @Override
    public List<ClusterElementDefinition> getClusterElementDefinitions(
        String componentName, int componentVersion, ClusterElementType clusterElementType) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        String icon = getIcon(componentDefinition);

        return componentDefinition.getClusterElements()
            .orElse(List.of())
            .stream()
            .filter(clusterElementDefinition -> clusterElementType == clusterElementDefinition.getType())
            .map(clusterElementDefinition -> new ClusterElementDefinition(
                clusterElementDefinition, componentDefinition.getName(), componentVersion, icon))
            .toList();
    }

    @Override
    public ClusterElementType getClusterElementType(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        ClusterRootComponentDefinition rootComponentDefinition =
            (ClusterRootComponentDefinition) componentDefinitionRegistry.getComponentDefinition(
                rootComponentName, rootComponentVersion);

        return rootComponentDefinition.getClusterElementTypes()
            .stream()
            .filter(curClusterElementType -> clusterElementTypeName.equalsIgnoreCase(curClusterElementType.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Cluster element type %s not found in root component %s".formatted(
                    clusterElementTypeName, rootComponentName)));
    }

    @Override
    public List<ClusterElementDefinition> getRootClusterElementDefinitions(
        String rootComponentName, int rootComponentVersion, String clusterElementTypeName) {

        ClusterElementType clusterElementType = getClusterElementType(
            rootComponentName, rootComponentVersion, clusterElementTypeName);

        return getClusterElementDefinitions(clusterElementType);
    }

    private static ConvertResult convert(
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection) {

        return new ConvertResult(
            ParametersFactory.createParameters(inputParameters),
            ParametersFactory.createParameters(
                componentConnection == null ? Map.of() : componentConnection.parameters()),
            getLookupDependsOnPathsMap(lookupDependsOnPaths));
    }

    private List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String clusterElementNameName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        ComponentConnection componentConnection, ClusterElementContext context) {

        ConvertResult convertResult = convert(inputParameters, lookupDependsOnPaths, componentConnection);

        try {
            PropertiesFunction propertiesFunction =
                getComponentPropertiesFunction(
                    componentName, componentVersion, clusterElementNameName, propertyName,
                    convertResult.inputParameters,
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
                e, inputParameters, ClusterElementDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES);
        }
    }

    private List<Option> executeOptions(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        ComponentConnection componentConnection, ClusterElementContext context) {

        try {
            ConvertResult convertResult = convert(inputParameters, lookupDependsOnPaths, componentConnection);

            OptionsFunction<?> optionsFunction = getComponentOptionsFunction(
                componentName, componentVersion, clusterElementName, propertyName, convertResult.inputParameters(),
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

            throw new ConfigurationException(e, inputParameters, ClusterElementDefinitionErrorType.EXECUTE_OPTIONS);
        }
    }

    private ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String clusterElementName, int statusCode, Object body,
        ClusterElementContext context) {

        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            componentDefinitionRegistry.getClusterElementDefinition(
                componentName, componentVersion, clusterElementName);

        try {
            return clusterElementDefinition.getProcessErrorResponse()
                .orElseGet(() -> (statusCode1, body1, context1) -> ProviderException.getProviderException(
                    statusCode1, body1))
                .apply(statusCode, body, context);
        } catch (Exception e) {
            throw new ExecutionException(e, ClusterElementDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
        }
    }

    private Object executeTool(
        String componentName, Integer componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ClusterElementContext context) {

        ToolFunction toolFunction = getClusterElement(
            componentName, componentVersion, clusterElementName);

        try {
            return toolFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(
                    componentConnection == null ? Map.of() : componentConnection.getParameters()),
                context);
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ExecutionException(e, inputParameters, ClusterElementDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String clusterElementName, Map<String, ?> inputParameters,
        ClusterElementContext context) {

        WorkflowNodeDescriptionFunction workflowNodeDescriptionFunction = getWorkflowNodeDescriptionFunction(
            componentName, componentVersion, clusterElementName);

        try {
            return workflowNodeDescriptionFunction.apply(ParametersFactory.createParameters(inputParameters), context);
        } catch (Exception e) {
            throw new ConfigurationException(
                e, inputParameters, ClusterElementDefinitionErrorType.EXECUTE_WORKFLOW_NODE_DESCRIPTION);
        }
    }

    private ComponentClusterElementDefinitionResult getComponentClusterElementDefinition(
        String componentName, @Nullable Integer componentVersion, String clusterElementName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        List<com.bytechef.component.definition.ClusterElementDefinition<?>> clusterElementDefinitions =
            componentDefinition.getClusterElements()
                .orElse(List.of());

        return new ComponentClusterElementDefinitionResult(
            componentDefinition,
            clusterElementDefinitions.stream()
                .filter(clusterElementDefinition -> clusterElementName.equals(clusterElementDefinition.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cluster element definition " + clusterElementName + " not found in component " + componentName)));
    }

    private PropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ClusterElementContext context) throws Exception {

        DynamicPropertiesProperty dynamicPropertiesProperty = (DynamicPropertiesProperty) componentDefinitionRegistry
            .getClusterElementProperty(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters,
                connectionParameters, lookupDependsOnPaths, context);

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        PropertiesDataSource.BasePropertiesFunction basePropertiesFunction = propertiesDataSource.getProperties();

        if (basePropertiesFunction instanceof ActionDefinition.PropertiesFunction propertiesFunction) {
            return (inputParameters1, connectionParameters1, lookupDependsOnPaths1, context1) -> propertiesFunction
                .apply(
                    inputParameters1, connectionParameters1, lookupDependsOnPaths1,
                    new ActionContextAdapater(context1));
        } else {
            return (PropertiesFunction) propertiesDataSource.getProperties();
        }
    }

    @SuppressWarnings("unchecked")
    private OptionsFunction<?> getComponentOptionsFunction(
        String componentName, int componentVersion, String clusterElementName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ClusterElementContext context) throws Exception {

        DynamicOptionsProperty<?> dynamicOptionsProperty = (DynamicOptionsProperty<?>) componentDefinitionRegistry
            .getClusterElementProperty(
                componentName, componentVersion, clusterElementName, propertyName, inputParameters,
                connectionParameters, lookupDependsOnPaths, context);

        OptionsDataSource<?> optionsDataSource = dynamicOptionsProperty.getOptionsDataSource()
            .orElseThrow(() -> new IllegalArgumentException(
                "Options data source not found for property " + propertyName + " in cluster element " +
                    clusterElementName));

        OptionsDataSource.BaseOptionsFunction baseOptionsFunction = optionsDataSource.getOptions();

        if (baseOptionsFunction instanceof ActionDefinition.OptionsFunction<?> optionsFunction) {
            return (
                inputParameters1, connectionParameters1, lookupDependsOnPaths1, searchText,
                context1) -> (List<? extends com.bytechef.component.definition.Option<Object>>) optionsFunction.apply(
                    inputParameters1, connectionParameters1, lookupDependsOnPaths1, searchText,
                    new ActionContextAdapater(context1));
        } else {
            return (OptionsFunction<?>) baseOptionsFunction;
        }
    }

    private static @Nullable String getIcon(ComponentDefinition componentDefinition) {
        return componentDefinition.getIcon()
            .orElse(null);
    }

    private static Map<String, String> getLookupDependsOnPathsMap(List<String> lookupDependsOnPaths) {
        return MapUtils.toMap(lookupDependsOnPaths, item -> item.substring(item.lastIndexOf(".") + 1), item -> item);
    }

    private WorkflowNodeDescriptionFunction getWorkflowNodeDescriptionFunction(
        String componentName, int componentVersion, String triggerName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition =
            componentDefinitionRegistry.getClusterElementDefinition(componentName, componentVersion, triggerName);

        String componentTitle = componentDefinition.getTitle()
            .orElse(componentDefinition.getName());
        String operationTitle = clusterElementDefinition.getTitle()
            .orElse(clusterElementDefinition.getName());
        String operationDescription = clusterElementDefinition
            .getDescription()
            .orElse(null);

        return clusterElementDefinition.getWorkflowNodeDescription()
            .orElse((inputParameters, context) -> WorkflowNodeDescriptionUtils.renderComponentProperties(
                inputParameters, componentTitle, operationTitle, operationDescription));
    }

    record ComponentClusterElementDefinitionResult(
        ComponentDefinition componentDefinition,
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition) {
    }

    private record ConvertResult(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPathsMap) {
    }
}
