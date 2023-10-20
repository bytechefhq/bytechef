
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.ComponentPropertiesFunction;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRefreshFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEnableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.StaticWebhookRequestFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.definition.factory.ContextFactory;
import com.bytechef.hermes.component.definition.HttpHeadersImpl;
import com.bytechef.hermes.component.definition.HttpParametersImpl;
import com.bytechef.hermes.component.registry.ComponentOperation;
import com.bytechef.hermes.component.registry.dto.WebhookTriggerFlags;
import com.bytechef.hermes.component.definition.ParameterMapImpl;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.definition.DynamicOptionsProperty;
import com.bytechef.hermes.definition.OptionsDataSource;
import com.bytechef.hermes.definition.PropertiesDataSource;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.component.registry.ComponentDefinitionRegistry;
import com.bytechef.hermes.execution.message.broker.ListenerParameters;
import com.bytechef.hermes.registry.domain.Property;
import com.bytechef.hermes.registry.domain.Option;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.registry.domain.ValueProperty;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.execution.message.broker.TriggerMessageRoute;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.message.broker.MessageBroker;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Service("triggerDefinitionService")
public class TriggerDefinitionServiceImpl implements TriggerDefinitionService, RemoteTriggerDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextFactory contextFactory;
    private final MessageBroker messageBroker;

    @SuppressFBWarnings("EI2")
    public TriggerDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextFactory contextFactory,
        MessageBroker messageBroker) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextFactory = contextFactory;
        this.messageBroker = messageBroker;
    }

    @Override
    public List<? extends ValueProperty<?>> executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName, @Nullable Connection connection) {

        ComponentPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, triggerName, propertyName);

        List<? extends com.bytechef.hermes.definition.Property.ValueProperty<?>> valueProperties =
            propertiesFunction.apply(
                new ParameterMapImpl(inputParameters),
                connection == null ? null : new ParameterMapImpl(connection.getParameters()),
                contextFactory.createContext(connection));

        return valueProperties.stream()
            .map(valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty))
            .toList();
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, Connection connection) {

        DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer = getDynamicWebhookDisableConsumer(
            componentName, componentVersion, triggerName);

        dynamicWebhookDisableConsumer.accept(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            new ParameterMapImpl(outputParameters), workflowExecutionId,
            contextFactory.createTriggerContext(connection));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String webhookUrl, @NonNull String workflowExecutionId,
        Connection connection) {

        DynamicWebhookEnableFunction dynamicWebhookEnableFunction = getDynamicWebhookEnableFunction(
            componentName, componentVersion, triggerName);

        return dynamicWebhookEnableFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            webhookUrl, workflowExecutionId, contextFactory.createTriggerContext(connection));
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters) {

        DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = getDynamicWebhookRefreshFunction(
            componentName, componentVersion, triggerName);

        return dynamicWebhookRefreshFunction.apply(new ParameterMapImpl(outputParameters));
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters,
        Connection connection) {

        EditorDescriptionFunction editorDescriptionFunction = getEditorDescriptionFunction(
            componentName, componentVersion, triggerName);

        return editorDescriptionFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()));
    }

    @Override
    public void executeListenerDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Connection connection) {

        ListenerDisableConsumer listenerDisableConsumer = getListenerDisableConsumer(
            componentName, componentVersion, triggerName);

        listenerDisableConsumer.accept(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            workflowExecutionId, contextFactory.createTriggerContext(connection));
    }

    @Override
    public void executeOnEnableListener(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, Connection connection) {

        ListenerEnableConsumer listenerEnableConsumer = getListenerEnableConsumer(
            componentName, componentVersion, triggerName);

        listenerEnableConsumer.accept(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            workflowExecutionId,
            output -> messageBroker.send(
                TriggerMessageRoute.LISTENERS,
                new ListenerParameters(WorkflowExecutionId.parse(workflowExecutionId), output)),
            contextFactory.createTriggerContext(connection));
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName, String searchText,
        Connection connection) {

        ComponentOptionsFunction optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, triggerName, propertyName);

        List<com.bytechef.hermes.definition.Option<?>> options = optionsFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            searchText, contextFactory.createTriggerContext(connection));

        return options.stream()
            .map(Option::new)
            .toList();
    }

    @Override
    public List<? extends ValueProperty<?>> executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Connection connection) {

        OutputSchemaFunction outputSchemaFunction = getOutputSchemaFunction(
            componentName, componentVersion, triggerName);

        return Property.toProperty(
            outputSchemaFunction.apply(
                new ParameterMapImpl(inputParameters),
                connection == null ? null : new ParameterMapImpl(connection.getParameters())));
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Connection connection) {

        SampleOutputFunction sampleOutputFunction = getSampleOutputFunction(
            componentName, componentVersion, triggerName);

        return sampleOutputFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, @NonNull WebhookRequest webhookRequest,
        Connection connection) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);
        TriggerContext triggerContext = contextFactory.createTriggerContext(connection);

        TriggerOutput triggerOutput;
        TriggerType triggerType = triggerDefinition.getType();

        if ((TriggerType.DYNAMIC_WEBHOOK == triggerType || TriggerType.STATIC_WEBHOOK == triggerType) &&
            !executeWebhookValidate(
                triggerDefinition, new ParameterMapImpl(inputParameters), webhookRequest, triggerContext)) {

            throw new IllegalStateException("Invalid trigger signature.");
        }

        if (TriggerType.DYNAMIC_WEBHOOK == triggerType) {
            triggerOutput = triggerDefinition.getDynamicWebhookRequest()
                .map(dynamicWebhookRequestFunction -> executeDynamicWebhookTrigger(
                    inputParameters, (DynamicWebhookEnableOutput) triggerState, webhookRequest,
                    connection, triggerContext, dynamicWebhookRequestFunction))
                .orElseThrow();
        } else if (TriggerType.STATIC_WEBHOOK == triggerType) {
            triggerOutput = triggerDefinition.getStaticWebhookRequest()
                .map(staticWebhookRequestFunction -> executeStaticWebhookTrigger(
                    inputParameters, webhookRequest, triggerContext, staticWebhookRequestFunction))
                .orElseThrow();
        } else if (TriggerType.POLLING == triggerType || TriggerType.HYBRID == triggerType) {
            triggerOutput = triggerDefinition.getPoll()
                .map(pollFunction -> executePollingTrigger(
                    triggerDefinition, inputParameters, (Map<String, ?>) triggerState, triggerContext,
                    pollFunction))
                .orElseThrow();
        } else {
            throw new IllegalArgumentException("Unknown trigger type: " + triggerType);
        }

        return triggerOutput;
    }

    @Override
    public boolean executeWebhookValidate(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest, Connection connection) {

        TriggerContext triggerContext = contextFactory.createTriggerContext(connection);
        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return executeWebhookValidate(
            triggerDefinition, new ParameterMapImpl(inputParameters), webhookRequest, triggerContext);
    }

    @Override
    public TriggerDefinition getTriggerDefinition(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        return new TriggerDefinition(
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName));
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull String componentName, int componentVersion) {
        return componentDefinitionRegistry.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(TriggerDefinition::new)
            .toList();
    }

    @Override
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull List<ComponentOperation> componentOperations) {
        List<TriggerDefinition> triggerDefinitions;

        if (componentOperations.isEmpty()) {
            triggerDefinitions = CollectionUtils.map(
                componentDefinitionRegistry.getTriggerDefinitions(), TriggerDefinition::new);
        } else {
            triggerDefinitions = CollectionUtils.map(
                componentOperations,
                componentOperation -> getTriggerDefinition(
                    componentOperation.componentName(), componentOperation.componentVersion(),
                    componentOperation.operationName()));
        }

        return triggerDefinitions;
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName) {

        TriggerDefinition triggerDefinition = getTriggerDefinition(componentName, componentVersion, triggerName);

        return new WebhookTriggerFlags(
            triggerDefinition.isWebhookRawBody(), triggerDefinition.isWorkflowSyncExecution(),
            triggerDefinition.isWorkflowSyncValidation());
    }

    private static TriggerOutput executeDynamicWebhookTrigger(
        Map<String, ?> inputParameters, DynamicWebhookEnableOutput output, WebhookRequest webhookRequest,
        Connection connection, TriggerContext triggerContext,
        DynamicWebhookRequestFunction dynamicWebhookRequestFunction) {

        WebhookOutput webhookOutput = dynamicWebhookRequestFunction.apply(
            new ParameterMapImpl(inputParameters),
            connection == null ? null : new ParameterMapImpl(connection.getParameters()),
            new HttpHeadersImpl(webhookRequest.headers()),
            new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
            output, triggerContext);

        return new TriggerOutput(webhookOutput.getValue(), null, false);
    }

    private static TriggerOutput executePollingTrigger(
        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition,
        Map<String, ?> inputParameters, Map<String, ?> closureParameters, TriggerContext triggerContext,
        PollFunction pollFunction) {

        PollOutput pollOutput = pollFunction.apply(
            new ParameterMapImpl(inputParameters), new ParameterMapImpl(closureParameters), triggerContext);

        List<Map<?, ?>> records = new ArrayList<>(
            pollOutput.records() == null ? Collections.emptyList() : pollOutput.records());

        while (pollOutput.pollImmediately()) {
            pollOutput = pollFunction.apply(
                new ParameterMapImpl(inputParameters), new ParameterMapImpl(pollOutput.closureParameters()),
                triggerContext);

            records.addAll(pollOutput.records());
        }

        return new TriggerOutput(
            records, pollOutput.closureParameters(),
            OptionalUtils.orElse(triggerDefinition.getBatch(), false));
    }

    private static TriggerOutput executeStaticWebhookTrigger(
        Map<String, ?> inputParameters, WebhookRequest webhookRequest, TriggerContext triggerContext,
        StaticWebhookRequestFunction staticWebhookRequestFunction) {

        WebhookOutput webhookOutput = staticWebhookRequestFunction.apply(
            new ParameterMapImpl(inputParameters), new HttpHeadersImpl(webhookRequest.headers()),
            new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
            triggerContext);

        return new TriggerOutput(webhookOutput.getValue(), null, false);
    }

    private boolean executeWebhookValidate(
        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition, ParameterMap inputParameters,
        WebhookRequest webhookRequest, TriggerContext triggerContext) {

        return triggerDefinition.getWebhookValidate()
            .map(webhookValidateFunction -> webhookValidateFunction.apply(
                inputParameters, new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                triggerContext))
            .orElse(true);
    }

    private ComponentOptionsFunction getComponentOptionsFunction(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getTriggerProperty(componentName, componentVersion, triggerName, propertyName);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (ComponentOptionsFunction) optionsDataSource.getOptions();
    }

    private ComponentPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        DynamicPropertiesProperty property = (DynamicPropertiesProperty) componentDefinitionRegistry.getTriggerProperty(
            componentName, componentVersion, triggerName, propertyName);

        PropertiesDataSource propertiesDataSource = property.getDynamicPropertiesDataSource();

        return (ComponentPropertiesFunction) propertiesDataSource.getProperties();
    }

    private DynamicWebhookRefreshFunction getDynamicWebhookRefreshFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getDynamicWebhookRefresh());
    }

    private DynamicWebhookDisableConsumer getDynamicWebhookDisableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getDynamicWebhookDisable());
    }

    private DynamicWebhookEnableFunction getDynamicWebhookEnableFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getDynamicWebhookEnable());
    }

    private EditorDescriptionFunction getEditorDescriptionFunction(
        String componentName, int componentVersion, String triggerName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.mapOrElse(
            triggerDefinition.getEditorDescriptionDataSource(),
            EditorDescriptionDataSource::getEditorDescription,
            (inputParameters, connectionParameters) -> componentDefinition.getTitle() + ": " +
                triggerDefinition.getTitle());
    }

    private ListenerDisableConsumer getListenerDisableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getListenerDisable());
    }

    private ListenerEnableConsumer getListenerEnableConsumer(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.get(triggerDefinition.getListenerEnable());
    }

    private OutputSchemaFunction getOutputSchemaFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(
            triggerDefinition.getOutputSchemaDataSource());

        return outputSchemaDataSource.getOutputSchema();
    }

    private SampleOutputFunction getSampleOutputFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            triggerDefinition.getSampleOutputDataSource());

        return sampleOutputDataSource.getSampleOutput();
    }
}
