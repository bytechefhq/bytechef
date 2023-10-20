
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

package com.bytechef.hermes.definition.registry.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.ComponentPropertiesFunction;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputFunction;
import com.bytechef.hermes.definition.DynamicOptionsProperty;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.OptionsDataSource;
import com.bytechef.hermes.definition.PropertiesDataSource;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.InputParametersImpl;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableFunction;
import com.bytechef.hermes.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerDisableConsumer;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEnableConsumer;
import com.bytechef.hermes.definition.registry.dto.TriggerDefinitionDTO;
import com.bytechef.hermes.definition.registry.component.factory.ContextConnectionFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerDefinitionServiceImpl implements TriggerDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextConnectionFactory contextConnectionFactory;

    @SuppressFBWarnings("EI2")
    public TriggerDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextConnectionFactory = contextConnectionFactory;
    }

    @Override
    public void executeDynamicWebhookDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId,
        DynamicWebhookEnableOutput output) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        DynamicWebhookDisableConsumer dynamicWebhookDisableConsumer = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookDisable());

        TriggerDefinition.DynamicWebhookDisableContext context = new TriggerDefinition.DynamicWebhookDisableContext(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), output, workflowExecutionId);

        dynamicWebhookDisableConsumer.accept(context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String webhookUrl,
        String workflowExecutionId) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        DynamicWebhookEnableFunction dynamicWebhookEnableFunction = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookEnable());

        TriggerDefinition.DynamicWebhookEnableContext context = new TriggerDefinition.DynamicWebhookEnableContext(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), webhookUrl, workflowExecutionId);

        return dynamicWebhookEnableFunction.apply(context);
    }

    @Override
    public DynamicWebhookEnableOutput executeDynamicWebhookRefresh(
        String componentName, int componentVersion, String triggerName, DynamicWebhookEnableOutput output) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        TriggerDefinition.DynamicWebhookRefreshFunction dynamicWebhookRefreshFunction = OptionalUtils.get(
            triggerDefinition.getDynamicWebhookRefresh());

        return dynamicWebhookRefreshFunction.apply(output);
    }

    @Override
    public List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters) {

        DynamicPropertiesProperty property = (DynamicPropertiesProperty) componentDefinitionRegistry.getTriggerProperty(
            propertyName, triggerName, componentName, componentVersion);

        PropertiesDataSource propertiesDataSource = property.getDynamicPropertiesDataSource();

        ComponentPropertiesFunction propertiesFunction = (ComponentPropertiesFunction) propertiesDataSource
            .getProperties();

        return propertiesFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters));
    }

    @Override
    public String executeEditorDescription(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        EditorDescriptionFunction editorDescriptionFunction = OptionalUtils.mapOrElse(
            triggerDefinition.getEditorDescriptionDataSource(),
            EditorDescriptionDataSource::getEditorDescription,
            (Context.Connection connection, InputParameters inputParameters) -> componentDefinition.getTitle() + ": "
                + triggerDefinition.getTitle());

        return editorDescriptionFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters));
    }

    @Override
    public void executeListenerDisable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        ListenerDisableConsumer listenerDisableConsumer = OptionalUtils.get(triggerDefinition.getListenerDisable());

        listenerDisableConsumer.accept(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), workflowExecutionId);
    }

    @Override
    public void executeListenerEnable(
        String triggerName, String componentName, int componentVersion, Map<String, Object> connectionParameters,
        String authorizationName, Map<String, Object> triggerParameters, String workflowExecutionId) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        ListenerEnableConsumer listenerEnableConsumer = OptionalUtils.get(triggerDefinition.getListenerEnable());

        listenerEnableConsumer.accept(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters), workflowExecutionId);
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String triggerName, String componentName, int componentVersion,
        Map<String, Object> triggerParameters, String authorizationName, Map<String, Object> connectionParameters) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getTriggerProperty(propertyName, triggerName, componentName, componentVersion);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        ComponentOptionsFunction optionsFunction = (ComponentOptionsFunction) optionsDataSource.getOptions();

        return optionsFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters));
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(
            triggerDefinition.getOutputSchemaDataSource());

        OutputSchemaFunction outputSchemaFunction = outputSchemaDataSource.getOutputSchema();

        return outputSchemaFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters));
    }

    @Override
    public Object executeSampleOutput(
        String triggerName, String componentName, int componentVersion, Map<String, Object> triggerParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        TriggerDefinition triggerDefinition = componentDefinitionRegistry.getTriggerDefinition(
            triggerName, componentName, componentVersion);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            triggerDefinition.getSampleOutputDataSource());

        SampleOutputFunction sampleOutputFunction = sampleOutputDataSource.getSampleOutput();

        return sampleOutputFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(triggerParameters));
    }

    @Override
    public TriggerDefinitionDTO getTriggerDefinition(String triggerName, String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return toTriggerDefinitionDTO(
            componentDefinitionRegistry.getTriggerDefinition(triggerName, componentName, componentVersion),
            componentDefinition);
    }

    @Override
    public List<TriggerDefinitionDTO> getTriggerDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        return componentDefinitionRegistry.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(triggerDefinition -> toTriggerDefinitionDTO(triggerDefinition, componentDefinition))
            .toList();
    }

    private TriggerDefinitionDTO toTriggerDefinitionDTO(
        TriggerDefinition triggerDefinition, ComponentDefinition componentDefinition) {

        return new TriggerDefinitionDTO(triggerDefinition, componentDefinition);
    }
}
