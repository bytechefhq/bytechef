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

package com.bytechef.hermes.component.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.DynamicOptionsProperty;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource;
import com.bytechef.hermes.component.definition.HttpHeadersImpl;
import com.bytechef.hermes.component.definition.HttpParametersImpl;
import com.bytechef.hermes.component.definition.OptionsDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.Parameters;
import com.bytechef.hermes.component.definition.ParametersImpl;
import com.bytechef.hermes.component.definition.PropertiesDataSource;
import com.bytechef.hermes.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.TriggerSampleOutputFunction;
import com.bytechef.hermes.component.definition.TriggerContext;
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
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.definition.TriggerDefinition.WebhookOutput;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.registry.ComponentDefinitionRegistry;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.component.registry.domain.ComponentConnection;
import com.bytechef.hermes.component.registry.domain.EditorDescriptionResponse;
import com.bytechef.hermes.component.registry.domain.OptionsResponse;
import com.bytechef.hermes.component.registry.domain.OutputSchemaResponse;
import com.bytechef.hermes.component.registry.domain.PropertiesResponse;
import com.bytechef.hermes.component.registry.domain.Property;
import com.bytechef.hermes.component.registry.domain.SampleOutputResponse;
import com.bytechef.hermes.component.registry.domain.TriggerDefinition;
import com.bytechef.hermes.component.registry.domain.ValueProperty;
import com.bytechef.hermes.component.registry.domain.WebhookTriggerFlags;
import com.bytechef.hermes.component.registry.trigger.TriggerOutput;
import com.bytechef.hermes.component.registry.trigger.WebhookRequest;
import com.bytechef.hermes.coordinator.event.TriggerListenerEvent;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.registry.domain.Option;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    public PropertiesResponse executeDynamicProperties(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName,
        @Nullable ComponentConnection connection, @NonNull TriggerContext context) {

        PropertiesDataSource.TriggerPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, triggerName, propertyName);

        PropertiesDataSource.PropertiesResponse propertiesResponse;

        try {
            propertiesResponse = propertiesFunction.apply(
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        return new PropertiesResponse(
            CollectionUtils.map(
                propertiesResponse.properties(),
                valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty)),
            propertiesResponse.errorMessage());
    }

    @Override
    public void executeDynamicWebhookDisable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId,
        @NonNull Map<String, ?> outputParameters, ComponentConnection connection, @NonNull TriggerContext context) {

        DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer = getDynamicWebhookDisableConsumer(
            componentName, componentVersion, triggerName);

        dynamicWebhookDisableConsumer.accept(
            new ParametersImpl(inputParameters),
            connection == null ? null : new ParametersImpl(connection.parameters()),
            new ParametersImpl(outputParameters), workflowExecutionId, context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String webhookUrl, @NonNull String workflowExecutionId,
        ComponentConnection connection, @NonNull TriggerContext context) {

        DynamicWebhookEnableFunction dynamicWebhookEnableFunction = getDynamicWebhookEnableFunction(
            componentName, componentVersion, triggerName);

        return dynamicWebhookEnableFunction.apply(
            new ParametersImpl(inputParameters),
            connection == null ? null : new ParametersImpl(connection.parameters()),
            webhookUrl, workflowExecutionId, context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> outputParameters, @NonNull TriggerContext context) {

        DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = getDynamicWebhookRefreshFunction(
            componentName, componentVersion, triggerName);

        return dynamicWebhookRefreshFunction.apply(
            new ParametersImpl(outputParameters), context);
    }

    @Override
    public EditorDescriptionResponse executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull TriggerContext context) {

        EditorDescriptionDataSource.TriggerEditorDescriptionFunction editorDescriptionFunction =
            getEditorDescriptionFunction(
                componentName, componentVersion, triggerName);

        EditorDescriptionDataSource.EditorDescriptionResponse editorDescriptionResponse;

        try {
            editorDescriptionResponse = editorDescriptionFunction
                .apply(new ParametersImpl(inputParameters), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        return new EditorDescriptionResponse(
            editorDescriptionResponse.description(), editorDescriptionResponse.errorMessage());
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
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()), workflowExecutionId,
                context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }
    }

    @Override
    public void executeOnEnableListener(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String workflowExecutionId, ComponentConnection connection,
        @NonNull TriggerContext context) {

        ListenerEnableConsumer listenerEnableConsumer = getListenerEnableConsumer(
            componentName, componentVersion, triggerName);

        try {
            listenerEnableConsumer.accept(
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()),
                workflowExecutionId,
                output -> eventPublisher.publishEvent(
                    new TriggerListenerEvent(
                        new TriggerListenerEvent.ListenerParameters(
                            WorkflowExecutionId.parse(workflowExecutionId), LocalDateTime.now(), output))),
                context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }
    }

    @Override
    public OptionsResponse executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, @NonNull String propertyName, String searchText,
        ComponentConnection connection, @NonNull TriggerContext context) {

        OptionsDataSource.TriggerOptionsFunction optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, triggerName, propertyName);

        OptionsDataSource.OptionsResponse optionsResponse = null;
        try {
            optionsResponse = optionsFunction.apply(
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()), searchText, context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        return new OptionsResponse(
            CollectionUtils.map(optionsResponse.options(), Option::new), optionsResponse.errorMessage());
    }

    @Override
    public OutputSchemaResponse executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull TriggerContext context) {

        OutputSchemaDataSource.TriggerOutputSchemaFunction outputSchemaFunction = getOutputSchemaFunction(
            componentName, componentVersion, triggerName);

        OutputSchemaDataSource.OutputSchemaResponse outputSchemaResponse;

        try {
            outputSchemaResponse = outputSchemaFunction.apply(
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        return new OutputSchemaResponse(
            Property.toProperty(outputSchemaResponse.property()), outputSchemaResponse.errorMessage());
    }

    @Override
    public SampleOutputResponse executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull TriggerContext context) {

        TriggerSampleOutputFunction sampleOutputFunction = getSampleOutputFunction(
            componentName, componentVersion, triggerName);

        SampleOutputDataSource.SampleOutputResponse sampleOutputResponse;

        try {
            sampleOutputResponse = sampleOutputFunction.apply(
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        Object sampleOutput = sampleOutputResponse.sampleOutput();

        if (sampleOutput instanceof String string) {
            try {
                sampleOutput = JsonUtils.read(string);
            } catch (Exception e) {
                //
            }
        }

        return new SampleOutputResponse(sampleOutput, sampleOutputResponse.errorMessage());
    }

    @Override
    @SuppressWarnings("unchecked")
    public TriggerOutput executeTrigger(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName,
        @NonNull Map<String, ?> inputParameters, Object triggerState, WebhookRequest webhookRequest,
        ComponentConnection connection, @NonNull TriggerContext context) {

        TriggerOutput triggerOutput;
        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        TriggerType triggerType = triggerDefinition.getType();

        if ((TriggerType.DYNAMIC_WEBHOOK == triggerType || TriggerType.STATIC_WEBHOOK == triggerType) &&
            !executeWebhookValidate(
                triggerDefinition, new ParametersImpl(inputParameters), webhookRequest, context)) {

            throw new IllegalStateException("Invalid trigger signature.");
        }

        if (TriggerType.DYNAMIC_WEBHOOK == triggerType) {
            triggerOutput = triggerDefinition.getDynamicWebhookRequest()
                .map(dynamicWebhookRequestFunction -> executeDynamicWebhookTrigger(
                    inputParameters, (DynamicWebhookEnableOutput) triggerState, webhookRequest,
                    connection, context, dynamicWebhookRequestFunction))
                .orElseThrow();
        } else if (TriggerType.STATIC_WEBHOOK == triggerType) {
            triggerOutput = triggerDefinition.getStaticWebhookRequest()
                .map(staticWebhookRequestFunction -> executeStaticWebhookTrigger(
                    inputParameters, webhookRequest, context, staticWebhookRequestFunction))
                .orElseThrow();
        } else if (TriggerType.POLLING == triggerType || TriggerType.HYBRID == triggerType) {
            triggerOutput = triggerDefinition.getPoll()
                .map(pollFunction -> executePollingTrigger(
                    triggerDefinition, inputParameters, (Map<String, ?>) triggerState, context,
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
        @NonNull Map<String, ?> inputParameters, @NonNull WebhookRequest webhookRequest,
        ComponentConnection connection, @NonNull TriggerContext context) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return executeWebhookValidate(
            triggerDefinition, new ParametersImpl(inputParameters), webhookRequest, context);
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
    public List<TriggerDefinition> getTriggerDefinitions(@NonNull List<OperationType> operationTypes) {
        List<TriggerDefinition> triggerDefinitions;

        if (operationTypes.isEmpty()) {
            triggerDefinitions = CollectionUtils.map(
                componentDefinitionRegistry.getTriggerDefinitions(), TriggerDefinition::new);
        } else {
            triggerDefinitions = CollectionUtils.map(
                operationTypes,
                componentOperation -> getTriggerDefinition(
                    componentOperation.componentName(), componentOperation.componentVersion(),
                    componentOperation.componentOperationName()));
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
        ComponentConnection connection, TriggerContext triggerContext,
        DynamicWebhookRequestFunction dynamicWebhookRequestFunction) {

        WebhookOutput webhookOutput;

        try {
            webhookOutput = dynamicWebhookRequestFunction.apply(
                new ParametersImpl(inputParameters),
                connection == null ? null : new ParametersImpl(connection.parameters()),
                new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                output, triggerContext);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        return new TriggerOutput(webhookOutput.getValue(), null, false);
    }

    private static TriggerOutput executePollingTrigger(
        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition,
        Map<String, ?> inputParameters, Map<String, ?> closureParameters, TriggerContext triggerContext,
        PollFunction pollFunction) {

        PollOutput pollOutput;

        try {
            pollOutput = pollFunction.apply(
                new ParametersImpl(inputParameters), new ParametersImpl(closureParameters), triggerContext);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        List<Map<?, ?>> records = new ArrayList<>(
            pollOutput.records() == null ? Collections.emptyList() : pollOutput.records());

        while (pollOutput.pollImmediately()) {
            try {
                pollOutput = pollFunction.apply(
                    new ParametersImpl(inputParameters), new ParametersImpl(pollOutput.closureParameters()),
                    triggerContext);
            } catch (Exception e) {
                throw new ComponentExecutionException(e, inputParameters);
            }

            records.addAll(pollOutput.records());
        }

        return new TriggerOutput(
            records, pollOutput.closureParameters(),
            OptionalUtils.orElse(triggerDefinition.getBatch(), false));
    }

    private static TriggerOutput executeStaticWebhookTrigger(
        Map<String, ?> inputParameters, WebhookRequest webhookRequest, TriggerContext triggerContext,
        StaticWebhookRequestFunction staticWebhookRequestFunction) {

        WebhookOutput webhookOutput;

        try {
            webhookOutput = staticWebhookRequestFunction.apply(
                new ParametersImpl(inputParameters), new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                triggerContext);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }

        return new TriggerOutput(webhookOutput.getValue(), null, false);
    }

    private boolean executeWebhookValidate(
        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition, Parameters inputParameters,
        WebhookRequest webhookRequest, TriggerContext context) {

        return triggerDefinition.getWebhookValidate()
            .map(webhookValidateFunction -> webhookValidateFunction.apply(
                inputParameters, new HttpHeadersImpl(webhookRequest.headers()),
                new HttpParametersImpl(webhookRequest.parameters()), webhookRequest.body(), webhookRequest.method(),
                context))
            .orElse(true);
    }

    private OptionsDataSource.TriggerOptionsFunction getComponentOptionsFunction(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getTriggerProperty(componentName, componentVersion, triggerName, propertyName);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (OptionsDataSource.TriggerOptionsFunction) optionsDataSource.getOptions();
    }

    private PropertiesDataSource.TriggerPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String triggerName, String propertyName) {

        DynamicPropertiesProperty property = (DynamicPropertiesProperty) componentDefinitionRegistry.getTriggerProperty(
            componentName, componentVersion, triggerName, propertyName);

        PropertiesDataSource propertiesDataSource = property.getDynamicPropertiesDataSource();

        return (PropertiesDataSource.TriggerPropertiesFunction) propertiesDataSource.getProperties();
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

    private EditorDescriptionDataSource.TriggerEditorDescriptionFunction getEditorDescriptionFunction(
        String componentName, int componentVersion, String triggerName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        return OptionalUtils.mapOrElse(
            triggerDefinition.getEditorDescriptionDataSource(),
            editorDescriptionDataSource -> (EditorDescriptionDataSource.TriggerEditorDescriptionFunction) editorDescriptionDataSource
                .getEditorDescription(),
            (inputParameters, context) -> new EditorDescriptionDataSource.EditorDescriptionResponse(
                componentDefinition.getTitle() + ": " + triggerDefinition.getTitle()));
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

    private OutputSchemaDataSource.TriggerOutputSchemaFunction getOutputSchemaFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(
            triggerDefinition.getOutputSchemaDataSource());

        return (OutputSchemaDataSource.TriggerOutputSchemaFunction) outputSchemaDataSource.getOutputSchema();
    }

    private TriggerSampleOutputFunction getSampleOutputFunction(
        String componentName, int componentVersion, String triggerName) {

        com.bytechef.hermes.component.definition.TriggerDefinition triggerDefinition =
            componentDefinitionRegistry.getTriggerDefinition(componentName, componentVersion, triggerName);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            triggerDefinition.getSampleOutputDataSource());

        return (TriggerSampleOutputFunction) sampleOutputDataSource.getSampleOutput();
    }
}
