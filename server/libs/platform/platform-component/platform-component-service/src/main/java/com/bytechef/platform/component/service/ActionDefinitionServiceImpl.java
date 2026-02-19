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
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.BaseOutputFunction;
import com.bytechef.component.definition.ActionDefinition.BasePerformFunction;
import com.bytechef.component.definition.ActionDefinition.BeforeResumeFunction;
import com.bytechef.component.definition.ActionDefinition.BeforeSuspendConsumer;
import com.bytechef.component.definition.ActionDefinition.BeforeTimeoutResumeFunction;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ActionDefinition.SuspendPerformFunction;
import com.bytechef.component.definition.ActionDefinition.WorkflowNodeDescriptionFunction;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.annotation.WithTokenRefresh;
import com.bytechef.platform.component.annotation.WithTokenRefresh.ComponentNameParam;
import com.bytechef.platform.component.annotation.WithTokenRefresh.ConnectionParam;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsSseStreamResponsePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.util.WorkflowNodeDescriptionUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("actionDefinitionService")
public class ActionDefinitionServiceImpl implements ActionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextFactory contextFactory;

    public ActionDefinitionServiceImpl(
        @Lazy ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
    }

    @Override
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_DYNAMIC_PROPERTIES")
    public List<Property> executeDynamicProperties(
        @ComponentNameParam String componentName, int componentVersion, String actionName,
        String propertyName, Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String workflowId,
        @ConnectionParam @Nullable ComponentConnection componentConnection) {

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, workflowId, componentConnection, null, null,
            true);

        return doExecuteDynamicProperties(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            componentConnection, actionContext);
    }

    @Override
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_OPTIONS")
    public List<Option> executeOptions(
        @ComponentNameParam String componentName, int componentVersion, String actionName,
        String propertyName, Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @ConnectionParam @Nullable ComponentConnection componentConnection) {

        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null, componentConnection, null, null, true);

        return doExecuteOptions(
            componentName, componentVersion, actionName, propertyName, inputParameters, lookupDependsOnPaths,
            searchText, componentConnection, actionContext);
    }

    @Override
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_OUTPUT")
    public @Nullable OutputResponse executeOutput(
        @ComponentNameParam String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters, @ConnectionParam Map<String, ComponentConnection> componentConnections) {

        BaseOutputFunction baseOutputFunction = (BaseOutputFunction) componentDefinitionRegistry
            .getActionDefinition(componentName, componentVersion, actionName)
            .getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .orElse(null);

        if (baseOutputFunction == null) {
            return null;
        }

        if (baseOutputFunction instanceof OutputFunction outputFunction) {
            ComponentConnection firstComponentConnection = getFirstComponentConnection(componentConnections);

            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, firstComponentConnection, null,
                null, true);

            return executeSingleConnectionOutput(
                outputFunction, inputParameters, firstComponentConnection, actionContext);
        } else {
            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, null, null, null, null, null, null, null, true);

            return executeMultipleConnectionsOutput(
                (MultipleConnectionsOutputFunction) baseOutputFunction, inputParameters, componentConnections, Map.of(),
                actionContext);
        }
    }

    @Override
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_PERFORM")
    public Object executePerform(
        @ComponentNameParam String componentName, int componentVersion, String actionName,
        Long jobPrincipalId,
        Long jobPrincipalWorkflowId, Long jobId, String workflowId, Map<String, ?> inputParameters,
        @ConnectionParam Map<String, ComponentConnection> componentConnections,
        Map<String, ?> extensions, Long environmentId, boolean editorEnvironment, PlatformType type,
        @Nullable Map<String, ?> continueParameters, @Nullable Instant suspendExpiresAt) {

        com.bytechef.component.definition.ActionDefinition actionDefinition = componentDefinitionRegistry
            .getActionDefinition(componentName, componentVersion, actionName);

        if (continueParameters != null) {
            Optional<ResumePerformFunction> resumePerformOptional = actionDefinition.getResumePerform();

            if (resumePerformOptional.isPresent()) {
                ComponentConnection firstComponentConnection = getFirstComponentConnection(componentConnections);

                ActionContext actionContext = contextFactory.createActionContext(
                    componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId,
                    workflowId, firstComponentConnection, environmentId, type, editorEnvironment);

                return executeResumePerform(
                    actionDefinition, resumePerformOptional.get(), inputParameters, continueParameters,
                    suspendExpiresAt, firstComponentConnection, actionContext);
            }
        }

        Optional<SuspendPerformFunction> suspendPerformOptional = actionDefinition.getSuspendPerform();

        if (suspendPerformOptional.isPresent()) {
            ComponentConnection firstComponentConnection = getFirstComponentConnection(componentConnections);

            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId, workflowId,
                firstComponentConnection, environmentId, type, editorEnvironment);

            return executeSuspendPerform(
                actionDefinition, suspendPerformOptional.get(), inputParameters, firstComponentConnection,
                actionContext);
        }

        BasePerformFunction basePerformFunction = actionDefinition
            .getPerform()
            .orElseThrow(() -> new IllegalArgumentException("Perform function is not defined."));

        if (basePerformFunction instanceof PerformFunction performFunction) {
            ComponentConnection firstComponentConnection = getFirstComponentConnection(componentConnections);

            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId, workflowId,
                firstComponentConnection, environmentId, type, editorEnvironment);

            return executeSingleConnectionPerform(
                performFunction, inputParameters, firstComponentConnection, actionContext);
        } else {
            ActionContext actionContext = contextFactory.createActionContext(
                componentName, componentVersion, actionName, jobPrincipalId, jobPrincipalWorkflowId, jobId, workflowId,
                null, environmentId, type, editorEnvironment);

            if (basePerformFunction instanceof MultipleConnectionsPerformFunction performFunction) {
                return executeMultipleConnectionsPerform(
                    performFunction, inputParameters, componentConnections, extensions, actionContext);
            } else if (basePerformFunction instanceof MultipleConnectionsStreamPerformFunction performFunction) {
                return executeMultipleConnectionsStreamPerform(
                    performFunction, inputParameters, componentConnections, extensions, actionContext);
            } else {
                return executeMultipleConnectionsSseStreamResponsePerform(
                    (MultipleConnectionsSseStreamResponsePerformFunction) basePerformFunction, inputParameters,
                    componentConnections, extensions, actionContext);
            }
        }
    }

    @Override
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_PERFORM")
    public Object executePerformForPolyglot(
        @ComponentNameParam String componentName, int componentVersion, String actionName,
        Map<String, ?> inputParameters, @ConnectionParam @Nullable ComponentConnection componentConnection,
        @Nullable Long environmentId, ActionContext context) {

        PerformFunction performFunction =
            (PerformFunction) componentDefinitionRegistry
                .getActionDefinition(componentName, componentVersion, actionName)
                .getPerform()
                .orElseThrow(() -> new IllegalArgumentException("Perform function is not defined."));

        return executeSingleConnectionPerform(performFunction, inputParameters, componentConnection, context);
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, int connectionVersion, @Nullable String componentOperationName,
        int statusCode, Object body) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, componentOperationName);
        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, componentOperationName, null, null, null, null, null, null, null, false);

        try {
            return actionDefinition.getProcessErrorResponse()
                .orElseGet(() -> (statusCode1, body1, context1) -> ProviderException.getProviderException(
                    statusCode1, body1))
                .apply(statusCode, body, actionContext);
        } catch (Exception e) {
            throw new ExecutionException(e, ActionDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
        }
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String actionName, Map<String, ?> inputParameters) {

        WorkflowNodeDescriptionFunction workflowNodeDescriptionFunction =
            getWorkflowNodeDescriptionFunction(componentName, componentVersion, actionName);
        ActionContext actionContext = contextFactory.createActionContext(
            componentName, componentVersion, actionName, null, null, null, null, null, null, null, true);

        try {
            return workflowNodeDescriptionFunction.apply(
                ParametersFactory.create(inputParameters), actionContext);
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

    private static ConvertResult convert(
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection) {

        return new ConvertResult(
            ParametersFactory.create(inputParameters),
            ParametersFactory.create(
                componentConnection == null ? Map.of() : componentConnection.parameters()),
            getLookupDependsOnPathsMap(lookupDependsOnPaths));
    }

    private List<Property> doExecuteDynamicProperties(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths,
        @Nullable ComponentConnection componentConnection, ActionContext context) {

        ConvertResult convertResult = convert(inputParameters, lookupDependsOnPaths, componentConnection);

        try {
            PropertiesFunction propertiesFunction = getComponentPropertiesFunction(
                componentName, componentVersion, actionName, propertyName, convertResult.inputParameters,
                convertResult.connectionParameters, convertResult.lookupDependsOnPathsMap, context);

            return propertiesFunction
                .apply(
                    convertResult.inputParameters, convertResult.connectionParameters,
                    convertResult.lookupDependsOnPathsMap, context)
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

    private @Nullable OutputResponse executeMultipleConnectionsOutput(
        MultipleConnectionsOutputFunction outputFunction, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> connections, Map<String, ?> extensions, ActionContext context) {

        try {
            BaseOutputDefinition.OutputResponse outputResponse = outputFunction.apply(
                ParametersFactory.create(inputParameters), connections,
                ParametersFactory.create(extensions), context);

            return toOutputResponse(outputResponse);
        } catch (Exception e) {
            throw new ConfigurationException(
                e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
        }
    }

    private Object executeMultipleConnectionsPerform(
        MultipleConnectionsPerformFunction performFunction, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions, ActionContext context) {

        try {
            return performFunction.apply(
                ParametersFactory.create(inputParameters), componentConnections,
                ParametersFactory.create(extensions), context);
        } catch (Exception e) {
            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private Object executeMultipleConnectionsStreamPerform(
        MultipleConnectionsStreamPerformFunction performFunction, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions, ActionContext context) {

        try {
            return performFunction.apply(
                ParametersFactory.create(inputParameters), componentConnections,
                ParametersFactory.create(extensions), context);
        } catch (Exception e) {
            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private Object executeMultipleConnectionsSseStreamResponsePerform(
        MultipleConnectionsSseStreamResponsePerformFunction performFunction, Map<String, ?> inputParameters,
        Map<String, ComponentConnection> componentConnections, Map<String, ?> extensions, ActionContext context) {

        try {
            return performFunction.apply(
                ParametersFactory.create(inputParameters), componentConnections,
                ParametersFactory.create(extensions), context);
        } catch (Exception e) {
            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private List<Option> doExecuteOptions(
        String componentName, int componentVersion, String actionName, String propertyName,
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, String searchText,
        @Nullable ComponentConnection componentConnection, ActionContext context) {

        try {
            ConvertResult convertResult = convert(inputParameters, lookupDependsOnPaths, componentConnection);

            OptionsFunction<?> optionsFunction = getComponentOptionsFunction(
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

    private @Nullable OutputResponse executeSingleConnectionOutput(
        OutputFunction outputFunction, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ActionContext context) {

        try {
            BaseOutputDefinition.OutputResponse outputResponse = outputFunction.apply(
                ParametersFactory.create(inputParameters),
                ParametersFactory.create(
                    componentConnection == null ? Map.of() : componentConnection.getConnectionParameters()),
                context);

            return toOutputResponse(outputResponse);
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ConfigurationException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
        }
    }

    private Object executeResumePerform(
        com.bytechef.component.definition.ActionDefinition actionDefinition,
        ResumePerformFunction resumePerformFunction, Map<String, ?> inputParameters,
        Map<String, ?> continueParameters, @Nullable Instant suspendExpiresAt,
        @Nullable ComponentConnection componentConnection, ActionContext context) {

        try {
            Map<String, Object> mergedInputParameters = new HashMap<>(inputParameters);

            if (suspendExpiresAt != null) {
                Optional<BeforeTimeoutResumeFunction> beforeTimeoutResumeOptional =
                    actionDefinition.getBeforeTimeoutResume();

                if (beforeTimeoutResumeOptional.isPresent()) {
                    Optional<Map<String, ?>> additionalParameters = beforeTimeoutResumeOptional.get()
                        .apply(
                            ParametersFactory.create(inputParameters), ParametersFactory.create(continueParameters),
                            context);

                    if (additionalParameters.isPresent()) {
                        mergedInputParameters.putAll(additionalParameters.get());
                    }
                }
            }

            Optional<BeforeResumeFunction> beforeResumeOptional = actionDefinition.getBeforeResume();

            if (beforeResumeOptional.isPresent()) {
                Optional<Map<String, ?>> additionalParameters = beforeResumeOptional.get()
                    .apply(
                        null, ParametersFactory.create(mergedInputParameters),
                        ParametersFactory.create(continueParameters), context);

                if (additionalParameters.isPresent()) {
                    mergedInputParameters.putAll(additionalParameters.get());
                }
            }

            return resumePerformFunction.apply(
                ParametersFactory.create(mergedInputParameters),
                componentConnection == null
                    ? null : ParametersFactory.create(componentConnection.getConnectionParameters()),
                ParametersFactory.create(continueParameters), context);
        } catch (Exception exception) {
            if (exception instanceof ProviderException) {
                throw (ProviderException) exception;
            }

            throw new ExecutionException(exception, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private Suspend executeSuspendPerform(
        com.bytechef.component.definition.ActionDefinition actionDefinition,
        SuspendPerformFunction suspendPerformFunction, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ActionContext context) {

        try {
            Suspend suspend = suspendPerformFunction.apply(
                ParametersFactory.create(inputParameters),
                componentConnection == null
                    ? null : ParametersFactory.create(componentConnection.getConnectionParameters()),
                context);

            Optional<BeforeSuspendConsumer> beforeSuspendOptional = actionDefinition.getBeforeSuspend();

            if (beforeSuspendOptional.isPresent()) {
                beforeSuspendOptional.get()
                    .apply(
                        null, suspend.expiresAt(), ParametersFactory.create(suspend.continueParameters()), context);
            }

            return suspend;
        } catch (Exception exception) {
            if (exception instanceof ProviderException) {
                throw (ProviderException) exception;
            }

            throw new ExecutionException(exception, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private Object executeSingleConnectionPerform(
        PerformFunction performFunction, Map<String, ?> inputParameters,
        @Nullable ComponentConnection componentConnection, ActionContext context) throws ExecutionException {

        try {
            return performFunction.apply(
                ParametersFactory.create(inputParameters),
                componentConnection == null
                    ? null : ParametersFactory.create(componentConnection.getConnectionParameters()),
                context);
        } catch (Exception e) {
            if (e instanceof ProviderException) {
                throw (ProviderException) e;
            }

            throw new ExecutionException(e, inputParameters, ActionDefinitionErrorType.EXECUTE_PERFORM);
        }
    }

    private OptionsFunction<?> getComponentOptionsFunction(
        String componentName, int componentVersion, String actionName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) throws Exception {

        DynamicOptionsProperty<?> dynamicOptionsProperty = (DynamicOptionsProperty<?>) componentDefinitionRegistry
            .getActionProperty(
                componentName, componentVersion, actionName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        OptionsDataSource<?> optionsDataSource = dynamicOptionsProperty.getOptionsDataSource()
            .orElseThrow(() -> new IllegalArgumentException("Options data source is not defined."));

        return (OptionsFunction<?>) optionsDataSource.getOptions();
    }

    private PropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String actionName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        ActionContext context) throws Exception {

        DynamicPropertiesProperty dynamicPropertiesProperty = (DynamicPropertiesProperty) componentDefinitionRegistry
            .getActionProperty(
                componentName, componentVersion, actionName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        return (PropertiesFunction) propertiesDataSource.getProperties();
    }

    private WorkflowNodeDescriptionFunction
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

        Optional<String> componentDefinitionTitle = componentDefinition.getTitle();
        Optional<String> actionDefinitionTitle = actionDefinition.getTitle();
        Optional<String> actionDefinitionDescription = actionDefinition.getDescription();

        return actionDefinition.getWorkflowNodeDescription()
            .orElse((inputParameters, context) -> WorkflowNodeDescriptionUtils.renderComponentProperties(
                inputParameters, componentDefinitionTitle.orElse(componentDefinition.getName()),
                actionDefinitionTitle.orElse(actionDefinition.getName()),
                actionDefinitionDescription.orElse(null)));
    }

    private static Map<String, String> getLookupDependsOnPathsMap(List<String> lookupDependsOnPaths) {
        return MapUtils.toMap(lookupDependsOnPaths, item -> item.substring(item.lastIndexOf(".") + 1), item -> item);
    }

    private @Nullable ComponentConnection
        getFirstComponentConnection(Map<String, ComponentConnection> componentConnections) {
        Set<Map.Entry<String, ComponentConnection>> entries = componentConnections.entrySet();

        return !entries.isEmpty() ? CollectionUtils.getFirstMap(entries, Map.Entry::getValue) : null;
    }

    private static @Nullable OutputResponse toOutputResponse(
        BaseOutputDefinition.@Nullable OutputResponse outputResponse) {

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
