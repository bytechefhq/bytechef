
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
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentOptionsFunction;
import com.bytechef.hermes.component.definition.ComponentPropertiesFunction;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource;
import com.bytechef.hermes.component.definition.EditorDescriptionDataSource.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.definition.DynamicOptionsProperty;
import com.bytechef.hermes.definition.OptionsDataSource;
import com.bytechef.hermes.definition.PropertiesDataSource;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.factory.ContextConnectionFactory;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.component.action.CustomAction;
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.OptionDTO;
import com.bytechef.hermes.definition.registry.dto.PropertyDTO;
import com.bytechef.hermes.definition.registry.dto.ValuePropertyDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionServiceImpl implements ActionDefinitionService {

    private final ComponentDefinitionRegistry componentDefinitionRegistry;
    private final ContextConnectionFactory contextConnectionFactory;

    @SuppressFBWarnings("EI2")
    public ActionDefinitionServiceImpl(
        ComponentDefinitionRegistry componentDefinitionRegistry, ContextConnectionFactory contextConnectionFactory) {

        this.componentDefinitionRegistry = componentDefinitionRegistry;
        this.contextConnectionFactory = contextConnectionFactory;
    }

    @Override
    public String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        ActionDefinition actionDefinition = componentDefinitionRegistry.getActionDefinition(
            actionName, componentName, componentVersion);

        EditorDescriptionFunction editorDescriptionFunction = OptionalUtils.mapOrElse(
            actionDefinition.getEditorDescriptionDataSource(),
            EditorDescriptionDataSource::getEditorDescription,
            (Context.Connection connection, Map<String, ?> inputParameters) -> ComponentDefinitionDTO.getTitle(
                componentDefinition.getName(), OptionalUtils.orElse(componentDefinition.getTitle(), null)) + ": " +
                ActionDefinitionDTO.getTitle(actionDefinition));

        return editorDescriptionFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            actionParameters);
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, ?> actionParameters, String authorizationName, Map<String, ?> connectionParameters) {

        DynamicPropertiesProperty property = (DynamicPropertiesProperty) componentDefinitionRegistry.getActionProperty(
            propertyName, actionName, componentName, componentVersion);

        PropertiesDataSource propertiesDataSource = property.getDynamicPropertiesDataSource();

        ComponentPropertiesFunction propertiesFunction = (ComponentPropertiesFunction) propertiesDataSource
            .getProperties();

        return propertiesFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            actionParameters)
            .stream()
            .map(valueProperty -> (ValuePropertyDTO<?>) PropertyDTO.toPropertyDTO(valueProperty))
            .toList();
    }

    @Override
    public List<OptionDTO> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, ?> actionParameters, String authorizationName, Map<String, ?> connectionParameters) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getActionProperty(propertyName, actionName, componentName, componentVersion);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        ComponentOptionsFunction optionsFunction = (ComponentOptionsFunction) optionsDataSource.getOptions();

        return optionsFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            actionParameters)
            .stream()
            .map(OptionDTO::new)
            .toList();
    }

    @Override
    public List<? extends ValuePropertyDTO<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        ActionDefinition actionDefinition = componentDefinitionRegistry.getActionDefinition(
            actionName, componentName, componentVersion);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(actionDefinition.getOutputSchemaDataSource());

        OutputSchemaFunction outputSchemaFunction = outputSchemaDataSource.getOutputSchema();

        return outputSchemaFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            actionParameters)
            .stream()
            .map(outputProperty -> (ValuePropertyDTO<?>) PropertyDTO.toPropertyDTO(outputProperty))
            .toList();
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, ?> actionParameters,
        String authorizationName, Map<String, ?> connectionParameters) {

        ActionDefinition actionDefinition = componentDefinitionRegistry.getActionDefinition(
            actionName, componentName, componentVersion);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            actionDefinition.getSampleOutputDataSource());

        SampleOutputDataSource.SampleOutputFunction sampleOutputFunction = sampleOutputDataSource.getSampleOutput();

        return sampleOutputFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            actionParameters);
    }

    @Override
    public ActionDefinitionDTO getActionDefinition(String actionName, String componentName, int componentVersion) {
        ActionDefinition actionDefinition;

        if (Objects.equals(actionName, CustomAction.CUSTOM)) {
            ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
                componentName, componentVersion);

            actionDefinition = CustomAction.getCustomActionDefinition(componentDefinition);
        } else {
            actionDefinition = componentDefinitionRegistry.getActionDefinition(
                actionName, componentName, componentVersion);
        }

        return toActionDefinitionDTO(actionDefinition);
    }

    @Override
    public List<ActionDefinitionDTO> getActionDefinitions(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        List<ActionDefinitionDTO> actionDefinitionDTOs =
            componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
                .stream()
                .map(this::toActionDefinitionDTO)
                .toList();

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actionDefinitionDTOs = new ArrayList<>(actionDefinitionDTOs);

            actionDefinitionDTOs.add(
                toActionDefinitionDTO(CustomAction.getCustomActionDefinition(componentDefinition)));
        }

        return actionDefinitionDTOs;
    }

    private ActionDefinitionDTO toActionDefinitionDTO(ActionDefinition actionDefinition) {
        return new ActionDefinitionDTO(actionDefinition);
    }
}
