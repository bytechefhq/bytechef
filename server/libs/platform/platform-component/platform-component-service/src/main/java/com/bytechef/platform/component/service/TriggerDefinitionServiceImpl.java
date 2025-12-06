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
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookRefreshFunction;
import com.bytechef.component.definition.TriggerDefinition.ListenerDisableConsumer;
import com.bytechef.component.definition.TriggerDefinition.ListenerEnableConsumer;
import com.bytechef.component.definition.TriggerDefinition.PollFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookDisableConsumer;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableFunction;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookRequestFunction;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.definition.HttpHeadersImpl;
import com.bytechef.platform.component.definition.HttpParametersImpl;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.exception.TriggerDefinitionErrorType;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.util.WorkflowNodeDescriptionUtils;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("triggerDefinitionService")
public class TriggerDefinitionServiceImpl implements TriggerDefinitionService {

    private final ApplicationEventPublisher eventPublisher;
    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    public TriggerDefinitionServiceImpl(
        ApplicationEventPublisher eventPublisher, @Lazy ComponentDefinitionRegistry componentDefinitionRegistry) {

        this.eventPublisher = eventPublisher;
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public List<Property> executeDynamicProperties(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, List<String> lookupDependsOnPaths, @Nullable ComponentConnection componentConnection,
        TriggerContext context) {

        try {
            WrapResult wrapResult = wrap(inputParameters, lookupDependsOnPaths, componentConnection);

            com.bytechef.component.definition.TriggerDefinition.PropertiesFunction propertiesFunction =
                getComponentPropertiesFunction(
                    componentName, componentVersion, triggerName, propertyName, wrapResult.inputParameters,
                    wrapResult.connectionParameters, wrapResult.lookupDependsOnPathsMap, context);

            return CollectionUtils.map(
                propertiesFunction.apply(
                    wrapResult.inputParameters, wrapResult.connectionParameters, wrapResult.lookupDependsOnPathsMap,
                    context),
                valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));
        } catch (Exception e) {
            throw new ConfigurationException(
                e, inputParameters, TriggerDefinitionErrorType.DYNAMIC_PROPERTIES_FAILED);
        }
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, ComponentConnection componentConnection,
        Map<String, ?> outputParameters, TriggerContext context) {

        DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = getDynamicWebhookRefreshFunction(
            componentName, componentVersion, triggerName);

        return dynamicWebhookRefreshFunction.apply(
            ParametersFactory
                .createParameters(componentConnection == null ? Map.of() : componentConnection.parameters()),
            ParametersFactory.createParameters(outputParameters), context);
    }

    @Override
    public void executeListenerDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection componentConnection, TriggerContext context) {

        ListenerDisableConsumer listenerDisableConsumer = getListenerDisableConsumer(
            componentName, componentVersion, triggerName);

        try {
            listenerDisableConsumer.accept(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(
                    componentConnection == null ? Map.of() : componentConnection.parameters()),
                workflowExecutionId,
                context);
        } catch (Exception e) {
            throw new ExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.LISTENER_DISABLE_FAILED);
        }
    }

    @Override
    public void executeListenerEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, ComponentConnection componentConnection, TriggerContext context) {

        ListenerEnableConsumer listenerEnableConsumer = getListenerEnableConsumer(
            componentName, componentVersion, triggerName);

        try {
            listenerEnableConsumer.accept(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory
                    .createParameters(componentConnection == null ? Map.of() : componentConnection.parameters()),
                workflowExecutionId,
                output -> eventPublisher.publishEvent(
                    new TriggerListenerEvent(
                        new TriggerListenerEvent.ListenerParameters(
                            WorkflowExecutionId.parse(workflowExecutionId), Instant.now(), output))),
                context);
        } catch (Exception e) {
            throw new ExecutionException(e, inputParameters,
                TriggerDefinitionErrorType.LISTENER_ENABLE_FAILED);
        }
    }

    @Override
    public List<Option> executeOptions(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String propertyName, List<String> lookupDependsOnPaths, String searchText,
        ComponentConnection componentConnection, TriggerContext context) {

        try {
            WrapResult wrapResult = wrap(inputParameters, lookupDependsOnPaths, componentConnection);

            com.bytechef.component.definition.TriggerDefinition.OptionsFunction<?> optionsFunction =
                getComponentOptionsFunction(
                    componentName, componentVersion, triggerName, propertyName, wrapResult.inputParameters(),
                    wrapResult.connectionParameters(), wrapResult.lookupDependsOnPathsMap(), context);

            return CollectionUtils.map(
                optionsFunction.apply(
                    wrapResult.inputParameters(), wrapResult.connectionParameters(),
                    wrapResult.lookupDependsOnPathsMap(), searchText, context),
                Option::new);
        } catch (Exception e) {
            throw new ConfigurationException(e, inputParameters, TriggerDefinitionErrorType.OPTIONS_FAILED);
        }
    }

    @Override
    public OutputResponse executeOutput(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        ComponentConnection componentConnection, TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition
            .getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .map(f -> (com.bytechef.component.definition.TriggerDefinition.OutputFunction) f)
            .map(outputFunction -> {
                try {
                    BaseOutputDefinition.OutputResponse outputResponse = outputFunction.apply(
                        ParametersFactory.createParameters(inputParameters),
                        ParametersFactory.createParameters(
                            componentConnection == null ? Map.of() : componentConnection.getConnectionParameters()),
                        context);

                    if (outputResponse == null) {
                        return null;
                    }

                    return SchemaUtils.toOutput(
                        outputResponse, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);
                } catch (Exception e) {
                    if (e instanceof ProviderException) {
                        throw (ProviderException) e;
                    }

                    throw new ConfigurationException(
                        e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
                }
            })
            .orElse(null);
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String triggerName, int statusCode, Object body,
        TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        try {
            return triggerDefinition.getProcessErrorResponse()
                .orElseGet(() -> (statusCode1, body1, context1) -> ProviderException.getProviderException(
                    statusCode1, body1))
                .apply(statusCode, body, context);
        } catch (Exception e) {
            throw new ExecutionException(e, ActionDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public TriggerOutput executeTrigger(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        Object triggerState, WebhookRequest webhookRequest, ComponentConnection componentConnection,
        TriggerContext context) {

        TriggerOutput triggerOutput;
        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        TriggerType triggerType = triggerDefinition.getType();

        if (TriggerType.DYNAMIC_WEBHOOK == triggerType || TriggerType.STATIC_WEBHOOK == triggerType) {
            WebhookValidateResponse response = executeWebhookValidate(
                triggerDefinition, ParametersFactory.createParameters(inputParameters), webhookRequest, context);

            if (response.status() != HttpStatus.OK.getValue()) {
                throw new IllegalStateException("Invalid trigger signature.");
            }
        }

        if (TriggerType.DYNAMIC_WEBHOOK == triggerType || TriggerType.STATIC_WEBHOOK == triggerType) {
            triggerOutput = triggerDefinition.getWebhookRequest()
                .map(webhookRequestFunction -> executeWebhookTrigger(
                    triggerDefinition, inputParameters, toDynamicWebhookEnableOutput((Map<?, ?>) triggerState),
                    webhookRequest, componentConnection, context, webhookRequestFunction))
                .orElseThrow();
        } else if (TriggerType.POLLING == triggerType || TriggerType.HYBRID == triggerType) {
            triggerOutput = triggerDefinition.getPoll()
                .map(pollFunction -> executePollingTrigger(
                    triggerDefinition, inputParameters, componentConnection,
                    triggerState == null ? Map.of() : (Map<String, ?>) triggerState, context, pollFunction))
                .orElseThrow();
        } else {
            throw new IllegalArgumentException("Unknown trigger type: " + triggerType);
        }

        return triggerOutput;
    }

    @Override
    public void executeWebhookDisable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        String workflowExecutionId, Map<String, ?> outputParameters, ComponentConnection componentConnection,
        TriggerContext context) {

        WebhookDisableConsumer webhookDisableConsumer = getWebhookDisableConsumer(
            componentName, componentVersion, triggerName);

        if (webhookDisableConsumer == null) {
            return;
        }

        try {
            webhookDisableConsumer.accept(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory
                    .createParameters(componentConnection == null ? Map.of() : componentConnection.parameters()),
                ParametersFactory.createParameters(outputParameters), workflowExecutionId, context);
        } catch (Exception e) {
            if (e instanceof ProviderException pe) {
                throw pe;
            }

            throw new ExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.DYNAMIC_WEBHOOK_DISABLE_FAILED);
        }
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, String webhookUrl, String workflowExecutionId,
        ComponentConnection componentConnection, TriggerContext context) {

        WebhookEnableFunction webhookEnableFunction = getWebhookEnableFunction(
            componentName, componentVersion, triggerName);

        if (webhookEnableFunction == null) {
            return null;
        }

        try {
            return webhookEnableFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(
                    componentConnection == null ? Map.of() : componentConnection.parameters()),
                webhookUrl, workflowExecutionId, context);
        } catch (Exception e) {
            if (e instanceof ProviderException pe) {
                throw pe;
            }

            throw new ExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.DYNAMIC_WEBHOOK_ENABLE_FAILED);
        }
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        String componentName, int componentVersion, String triggerName,
        Map<String, ?> inputParameters, WebhookRequest webhookRequest,
        ComponentConnection componentConnection, TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return executeWebhookValidate(triggerDefinition, ParametersFactory.createParameters(inputParameters),
            webhookRequest,
            context);
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        WebhookRequest webhookRequest, ComponentConnection componentConnection, TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return executeWebhookValidateOnEnable(
            triggerDefinition, ParametersFactory.createParameters(inputParameters), webhookRequest, context);
    }

    @Override
    public String executeWorkflowNodeDescription(
        String componentName, int componentVersion, String triggerName, Map<String, ?> inputParameters,
        TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition.WorkflowNodeDescriptionFunction workflowNodeDescriptionFunction =
            getWorkflowNodeDescriptionFunction(componentName, componentVersion, triggerName);

        try {
            return workflowNodeDescriptionFunction.apply(ParametersFactory.createParameters(inputParameters), context);
        } catch (Exception e) {
            throw new ConfigurationException(
                e, inputParameters, TriggerDefinitionErrorType.WORKFLOW_NODE_DESCRIPTION_FAILED);
        }
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        String componentName, int componentVersion, String triggerName) {

        return new TriggerDefinition(
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName),
            componentName, componentVersion);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(String componentName, int componentVersion) {
        return componentDefinitionRegistry.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(triggerDefinition -> new TriggerDefinition(triggerDefinition, componentName, componentVersion))
            .toList();
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        String componentName, int componentVersion, String triggerName) {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        return new WebhookTriggerFlags(
            triggerDefinition.isWebhookRawBody(), triggerDefinition.isWorkflowSyncExecution(),
            triggerDefinition.isWorkflowSyncValidation(), triggerDefinition.isWorkflowSyncOnEnableValidation());
    }

    @Override
    public boolean isDynamicOutputDefined(String componentName, int componentVersion, String actionName) {
        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, actionName);

        return triggerDefinition.isOutputFunctionDefined();
    }

    private static TriggerOutput executePollingTrigger(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition,
        Map<String, ?> inputParameters, ComponentConnection componentConnection, Map<String, ?> closureParameters,
        TriggerContext triggerContext, PollFunction pollFunction) {

        PollOutput pollOutput;

        Parameters connectionParameters =
            ParametersFactory.createParameters(
                componentConnection == null ? Map.of() : componentConnection.getConnectionParameters());

        try {
            pollOutput = pollFunction.apply(
                ParametersFactory.createParameters(inputParameters), connectionParameters,
                ParametersFactory.createParameters(closureParameters), triggerContext);
        } catch (Exception e) {
            if (e instanceof ProviderException pe) {
                throw pe;
            }

            throw new ExecutionException(e, inputParameters, TriggerDefinitionErrorType.POLLING_TRIGGER_FAILED);
        }

        List<Object> records = new ArrayList<>(
            pollOutput.records() == null ? Collections.emptyList() : pollOutput.records());

        while (pollOutput.pollImmediately()) {
            try {
                pollOutput = pollFunction.apply(
                    ParametersFactory.createParameters(inputParameters), connectionParameters,
                    ParametersFactory.createParameters(pollOutput.closureParameters()), triggerContext);
            } catch (Exception e) {
                if (e instanceof ProviderException pe) {
                    throw pe;
                }

                throw new ExecutionException(e, inputParameters, TriggerDefinitionErrorType.POLLING_TRIGGER_FAILED);
            }

            records.addAll(pollOutput.records());
        }

        Optional<Boolean> triggerDefinitionBatch = triggerDefinition.getBatch();

        return new TriggerOutput(records, pollOutput.closureParameters(), triggerDefinitionBatch.orElse(false));
    }

    private WebhookValidateResponse executeWebhookValidate(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition, Parameters inputParameters,
        WebhookRequest webhookRequest, TriggerContext context) {

        return triggerDefinition.getWebhookValidate()
            .map(webhookValidateFunction -> webhookValidateFunction.apply(
                inputParameters, new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                context))
            .orElse(WebhookValidateResponse.ok());
    }

    private WebhookValidateResponse executeWebhookValidateOnEnable(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition, Parameters inputParameters,
        WebhookRequest webhookRequest, TriggerContext context) {

        return triggerDefinition.getWebhookValidateOnEnable()
            .map(webhookValidateFunction -> webhookValidateFunction.apply(
                inputParameters, new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                context))
            .orElse(WebhookValidateResponse.ok());
    }

    private static TriggerOutput executeWebhookTrigger(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition,
        Map<String, ?> inputParameters, WebhookEnableOutput output, WebhookRequest webhookRequest,
        ComponentConnection componentConnection, TriggerContext triggerContext,
        WebhookRequestFunction webhookRequestFunction) {

        Object webhookOutput;

        try {
            webhookOutput = webhookRequestFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory
                    .createParameters(componentConnection == null ? Map.of() : componentConnection.parameters()),
                new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                output, triggerContext);
        } catch (Exception e) {
            if (e instanceof ProviderException pe) {
                throw pe;
            }

            throw new ExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.DYNAMIC_WEBHOOK_TRIGGER_FAILED);
        }

        Optional<Boolean> triggerDefinitionBatch = triggerDefinition.getBatch();

        return new TriggerOutput(webhookOutput, null, triggerDefinitionBatch.orElse(false));
    }

    private com.bytechef.component.definition.TriggerDefinition.OptionsFunction<?> getComponentOptionsFunction(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        TriggerContext context) throws Exception {

        DynamicOptionsProperty<?> dynamicOptionsProperty = (DynamicOptionsProperty<?>) componentDefinitionRegistry
            .getTriggerProperty(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        OptionsDataSource<?> optionsDataSource = dynamicOptionsProperty.getOptionsDataSource()
            .orElseThrow(() -> new IllegalArgumentException("Options data source is not defined."));

        return (com.bytechef.component.definition.TriggerDefinition.OptionsFunction<?>) optionsDataSource.getOptions();
    }

    private com.bytechef.component.definition.TriggerDefinition.PropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        TriggerContext context) throws Exception {

        DynamicPropertiesProperty property =
            (DynamicPropertiesProperty) componentDefinitionRegistry.getTriggerProperty(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        PropertiesDataSource<?> propertiesDataSource = property.getDynamicPropertiesDataSource();

        return (com.bytechef.component.definition.TriggerDefinition.PropertiesFunction) propertiesDataSource
            .getProperties();
    }

    private DynamicWebhookRefreshFunction getDynamicWebhookRefreshFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getDynamicWebhookRefresh()
            .orElseThrow(() -> new IllegalArgumentException("Dynamic webhook refresh function is not defined."));
    }

    private static Map<String, String> getLookupDependsOnPathsMap(List<String> lookupDependsOnPaths) {
        return MapUtils.toMap(lookupDependsOnPaths, item -> item.substring(item.lastIndexOf(".") + 1), item -> item);
    }

    private WebhookDisableConsumer getWebhookDisableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getWebhookDisable()
            .orElse(null);
    }

    private WebhookEnableFunction getWebhookEnableFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getWebhookEnable()
            .orElse(null);
    }

    private com.bytechef.component.definition.TriggerDefinition.WorkflowNodeDescriptionFunction
        getWorkflowNodeDescriptionFunction(
            String componentName, int componentVersion, String triggerName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getWorkflowNodeDescription()
            .orElse((inputParameters, context) -> {
                Optional<String> componentDefinitionTitle = componentDefinition.getTitle();
                Optional<String> triggerDefinitionTitle = triggerDefinition.getTitle();
                Optional<String> triggerDefinitionDescription = triggerDefinition.getDescription();

                return WorkflowNodeDescriptionUtils.renderComponentProperties(
                    inputParameters, componentDefinitionTitle.orElse(componentDefinition.getName()),
                    triggerDefinitionTitle.orElse(triggerDefinition.getName()),
                    triggerDefinitionDescription.orElse(null));
            });
    }

    private ListenerDisableConsumer getListenerDisableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getListenerDisable()
            .orElseThrow(() -> new IllegalArgumentException("Listener disable function is not defined."));
    }

    private ListenerEnableConsumer getListenerEnableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getListenerEnable()
            .orElseThrow(() -> new IllegalArgumentException("Listener enable function is not defined."));
    }

    @SuppressWarnings("unchecked")
    private WebhookEnableOutput toDynamicWebhookEnableOutput(Map<?, ?> triggerState) {
        if (triggerState == null) {
            return null;
        }

        return new WebhookEnableOutput(
            (Map<String, ?>) triggerState.get("parameters"), (LocalDateTime) triggerState.get("webhookExpirationDate"));
    }

    private static WrapResult wrap(
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, ComponentConnection componentConnection) {

        return new WrapResult(
            ParametersFactory.createParameters(inputParameters),
            ParametersFactory.createParameters(
                componentConnection == null ? Map.of() : componentConnection.parameters()),
            getLookupDependsOnPathsMap(lookupDependsOnPaths));
    }

    private record WrapResult(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPathsMap) {
    }
}
