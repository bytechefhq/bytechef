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

package com.bytechef.platform.component.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
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
import com.bytechef.component.definition.TriggerWorkflowNodeDescriptionFunction;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.definition.HttpHeadersImpl;
import com.bytechef.platform.component.definition.HttpParametersImpl;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.exception.ComponentConfigurationException;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.exception.TriggerDefinitionErrorType;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.registry.domain.OutputResponse;
import com.bytechef.platform.registry.util.SchemaUtils;
import com.bytechef.platform.util.WorkflowNodeDescriptionUtils;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service("triggerDefinitionService")
public class TriggerDefinitionServiceImpl implements TriggerDefinitionService {

    private final ApplicationEventPublisher eventPublisher;
    private final ComponentDefinitionRegistry componentDefinitionRegistry;

    @SuppressFBWarnings("EI2")
    public TriggerDefinitionServiceImpl(
        ApplicationEventPublisher eventPublisher,
        ComponentDefinitionRegistry componentDefinitionRegistry) {

        this.eventPublisher = eventPublisher;
        this.componentDefinitionRegistry = componentDefinitionRegistry;
    }

    @Override
    public List<Property> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName,
        List<String> lookupDependsOnPaths, @Nullable ComponentConnection connection, @NonNull TriggerContext context) {

        try {
            WrapResult wrapResult = wrap(inputParameters, lookupDependsOnPaths, connection);

            PropertiesDataSource.TriggerPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
                componentName, componentVersion, triggerName, propertyName, wrapResult.inputParameters,
                wrapResult.connectionParameters, wrapResult.lookupDependsOnPathsMap, context);

            return CollectionUtils.map(
                propertiesFunction.apply(
                    wrapResult.inputParameters, wrapResult.connectionParameters, wrapResult.lookupDependsOnPathsMap,
                    context),
                valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));
        } catch (Exception e) {
            throw new ComponentConfigurationException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_DYNAMIC_PROPERTIES);
        }
    }

    @Override
    public WebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        ComponentConnection connection, @NonNull Map<String, ?> outputParameters, @NonNull TriggerContext context) {

        DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = getDynamicWebhookRefreshFunction(
            componentName, componentVersion, triggerName);

        return dynamicWebhookRefreshFunction.apply(
            ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
            ParametersFactory.createParameters(outputParameters), context);
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, ComponentConnection connection,
        @NonNull TriggerContext context) {

        ListenerDisableConsumer listenerDisableConsumer = getListenerDisableConsumer(
            componentName, componentVersion, triggerName);

        try {
            listenerDisableConsumer.accept(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
                workflowExecutionId,
                context);
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_LISTENER_DISABLE);
        }
    }

    @Override
    public void executeListenerEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, ComponentConnection connection,
        @NonNull TriggerContext context) {

        ListenerEnableConsumer listenerEnableConsumer = getListenerEnableConsumer(
            componentName, componentVersion, triggerName);

        try {
            listenerEnableConsumer.accept(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
                workflowExecutionId,
                output -> eventPublisher.publishEvent(
                    new TriggerListenerEvent(
                        new TriggerListenerEvent.ListenerParameters(
                            WorkflowExecutionId.parse(workflowExecutionId), LocalDateTime.now(), output))),
                context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters,
                TriggerDefinitionErrorType.EXECUTE_LISTENER_ENABLE);
        }
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName,
        @NonNull List<String> lookupDependsOnPaths,
        String searchText, ComponentConnection connection, @NonNull TriggerContext context) {

        try {
            WrapResult wrapResult = wrap(inputParameters, lookupDependsOnPaths, connection);

            OptionsDataSource.TriggerOptionsFunction<?> optionsFunction = getComponentOptionsFunction(
                componentName, componentVersion, triggerName, propertyName, wrapResult.inputParameters(),
                wrapResult.connectionParameters(), wrapResult.lookupDependsOnPathsMap(), context);

            return CollectionUtils.map(
                optionsFunction.apply(
                    wrapResult.inputParameters(), wrapResult.connectionParameters(),
                    wrapResult.lookupDependsOnPathsMap(), searchText, context),
                Option::new);
        } catch (Exception e) {
            throw new ComponentConfigurationException(e, inputParameters, TriggerDefinitionErrorType.EXECUTE_OPTIONS);
        }
    }

    @Override
    public OutputResponse executeOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition
            .getOutputDefinition()
            .flatMap(OutputDefinition::getOutput)
            .map(f -> (com.bytechef.component.definition.TriggerDefinition.TriggerOutputFunction) f)
            .map(triggerOutputFunction -> {
                try {
                    BaseOutputDefinition.OutputResponse outputResponse = triggerOutputFunction.apply(
                        ParametersFactory.createParameters(inputParameters),
                        ParametersFactory.createParameters(
                            connection == null ? Map.of() : connection.getConnectionParameters()),
                        context);

                    if (outputResponse == null) {
                        return null;
                    }

                    return SchemaUtils.toOutput(
                        outputResponse,
                        (property, sampleOutput) -> new OutputResponse(
                            Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput),
                        PropertyFactory.PROPERTY_FACTORY);
                } catch (Exception e) {
                    if (e instanceof ProviderException) {
                        throw (ProviderException) e;
                    }

                    throw new ComponentConfigurationException(
                        e, inputParameters, ActionDefinitionErrorType.EXECUTE_OUTPUT);
                }
            })
            .orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        ComponentConnection connection, @NonNull TriggerContext context) {

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
                    webhookRequest, connection, context, webhookRequestFunction))
                .orElseThrow();
        } else if (TriggerType.POLLING == triggerType || TriggerType.HYBRID == triggerType) {
            triggerOutput = triggerDefinition.getPoll()
                .map(pollFunction -> executePollingTrigger(
                    triggerDefinition, inputParameters, connection,
                    triggerState == null ? Map.of() : (Map<String, ?>) triggerState, context, pollFunction))
                .orElseThrow();
        } else {
            throw new IllegalArgumentException("Unknown trigger type: " + triggerType);
        }

        return triggerOutput;
    }

    @Override
    public void executeWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, ComponentConnection connection, @NonNull TriggerContext context) {

        WebhookDisableConsumer webhookDisableConsumer = getWebhookDisableConsumer(
            componentName, componentVersion, triggerName);

        if (webhookDisableConsumer == null) {
            return;
        }

        try {
            webhookDisableConsumer.accept(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
                ParametersFactory.createParameters(outputParameters), workflowExecutionId, context);
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_DYNAMIC_WEBHOOK_DISABLE);
        }
    }

    @Override
    public WebhookEnableOutput executeWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String webhookUrl, @NonNull String workflowExecutionId,
        ComponentConnection connection, @NonNull TriggerContext context) {

        WebhookEnableFunction webhookEnableFunction = getWebhookEnableFunction(
            componentName, componentVersion, triggerName);

        if (webhookEnableFunction == null) {
            return null;
        }

        try {
            return webhookEnableFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
                webhookUrl, workflowExecutionId, context);
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_DYNAMIC_WEBHOOK_ENABLE);
        }
    }

    @Override
    public WebhookValidateResponse executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest,
        ComponentConnection connection, @NonNull TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return executeWebhookValidate(triggerDefinition, ParametersFactory.createParameters(inputParameters),
            webhookRequest,
            context);
    }

    @Override
    public WebhookValidateResponse executeWebhookValidateOnEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest,
        ComponentConnection connection, @NonNull TriggerContext context) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return executeWebhookValidateOnEnable(
            triggerDefinition, ParametersFactory.createParameters(inputParameters), webhookRequest, context);
    }

    @Override
    public String executeWorkflowNodeDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull TriggerContext context) {

        TriggerWorkflowNodeDescriptionFunction workflowNodeDescriptionFunction = getWorkflowNodeDescriptionFunction(
            componentName, componentVersion, triggerName);

        try {
            return workflowNodeDescriptionFunction.apply(ParametersFactory.createParameters(inputParameters), context);
        } catch (Exception e) {
            throw new ComponentConfigurationException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_WORKFLOW_NODE_DESCRIPTION);
        }
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return new TriggerDefinition(
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName),
            componentName, componentVersion);
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion) {
        return componentDefinitionRegistry.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(triggerDefinition -> new TriggerDefinition(triggerDefinition, componentName, componentVersion))
            .toList();
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        return new WebhookTriggerFlags(
            triggerDefinition.isWebhookRawBody(), triggerDefinition.isWorkflowSyncExecution(),
            triggerDefinition.isWorkflowSyncValidation(), triggerDefinition.isWorkflowSyncOnEnableValidation());
    }

    @Override
    public ProviderException executeProcessErrorResponse(
        String componentName, int componentVersion, String triggerName, int statusCode, Object body,
        Context triggerContext) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        try {
            Optional<com.bytechef.component.definition.TriggerDefinition.ProcessErrorResponseFunction> processErrorResponse =
                triggerDefinition.getProcessErrorResponse();
            if (processErrorResponse.isPresent()) {
                return processErrorResponse.get()
                    .apply(statusCode, body, triggerContext);
            } else {
                return ProviderException.getProviderException(statusCode, body);
            }
        } catch (Exception e) {
            throw new ComponentExecutionException(e, ActionDefinitionErrorType.EXECUTE_PROCESS_ERROR_RESPONSE);
        }
    }

    private static TriggerOutput executePollingTrigger(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition,
        Map<String, ?> inputParameters, ComponentConnection connection, Map<String, ?> closureParameters,
        TriggerContext triggerContext, PollFunction pollFunction) {

        PollOutput pollOutput;

        Parameters connectionParameters =
            ParametersFactory.createParameters(connection == null ? Map.of() : connection.getConnectionParameters());

        try {
            pollOutput = pollFunction.apply(
                ParametersFactory.createParameters(inputParameters), connectionParameters,
                ParametersFactory.createParameters(closureParameters), triggerContext);
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_POLLING_TRIGGER);
        }

        List<Object> records = new ArrayList<>(
            pollOutput.records() == null ? Collections.emptyList() : pollOutput.records());

        while (pollOutput.pollImmediately()) {
            try {
                pollOutput = pollFunction.apply(
                    ParametersFactory.createParameters(inputParameters), connectionParameters,
                    ParametersFactory.createParameters(pollOutput.closureParameters()), triggerContext);
            } catch (Exception e) {
                throw new ComponentExecutionException(
                    e, inputParameters, TriggerDefinitionErrorType.EXECUTE_POLLING_TRIGGER);
            }

            records.addAll(pollOutput.records());
        }

        return new TriggerOutput(
            records, pollOutput.closureParameters(), OptionalUtils.orElse(triggerDefinition.getBatch(), false));
    }

    private static TriggerOutput executeWebhookTrigger(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition,
        Map<String, ?> inputParameters, WebhookEnableOutput output, WebhookRequest webhookRequest,
        ComponentConnection connection, TriggerContext triggerContext,
        WebhookRequestFunction webhookRequestFunction) {

        Object webhookOutput;

        try {
            webhookOutput = webhookRequestFunction.apply(
                ParametersFactory.createParameters(inputParameters),
                ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
                new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                output, triggerContext);
        } catch (Exception e) {
            throw new ComponentExecutionException(
                e, inputParameters, TriggerDefinitionErrorType.EXECUTE_DYNAMIC_WEBHOOK_TRIGGER);
        }

        return new TriggerOutput(webhookOutput, null, OptionalUtils.orElse(triggerDefinition.getBatch(), false));
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

    private OptionsDataSource.TriggerOptionsFunction<?> getComponentOptionsFunction(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        TriggerContext context) throws Exception {

        DynamicOptionsProperty<?> dynamicOptionsProperty = (DynamicOptionsProperty<?>) componentDefinitionRegistry
            .getTriggerProperty(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (OptionsDataSource.TriggerOptionsFunction<?>) optionsDataSource.getOptions();
    }

    private PropertiesDataSource.TriggerPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String triggerName, String propertyName,
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        TriggerContext context) throws Exception {

        DynamicPropertiesProperty property =
            (DynamicPropertiesProperty) componentDefinitionRegistry.getTriggerProperty(
                componentName, componentVersion, triggerName, propertyName, inputParameters, connectionParameters,
                lookupDependsOnPaths, context);

        PropertiesDataSource<?> propertiesDataSource = property.getDynamicPropertiesDataSource();

        return (PropertiesDataSource.TriggerPropertiesFunction) propertiesDataSource.getProperties();
    }

    private DynamicWebhookRefreshFunction getDynamicWebhookRefreshFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getDynamicWebhookRefresh());
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

    private TriggerWorkflowNodeDescriptionFunction getWorkflowNodeDescriptionFunction(
        String componentName, int componentVersion, String triggerName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return triggerDefinition.getWorkflowNodeDescription()
            .orElse((inputParameters, context) -> WorkflowNodeDescriptionUtils.renderComponentProperties(
                inputParameters, OptionalUtils.orElse(componentDefinition.getTitle(), componentDefinition.getName()),
                OptionalUtils.orElse(triggerDefinition.getTitle(), triggerDefinition.getName())));
    }

    private ListenerDisableConsumer getListenerDisableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getListenerDisable());
    }

    private ListenerEnableConsumer getListenerEnableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getListenerEnable());
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
        Map<String, ?> inputParameters, List<String> lookupDependsOnPaths, ComponentConnection connection) {

        return new WrapResult(
            ParametersFactory.createParameters(inputParameters),
            ParametersFactory.createParameters(connection == null ? Map.of() : connection.parameters()),
            getLookupDependsOnPathsMap(lookupDependsOnPaths));
    }

    private record WrapResult(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPathsMap) {
    }
}
