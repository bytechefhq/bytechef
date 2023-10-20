
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
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentDynamicPropertiesDataSource;
import com.bytechef.hermes.component.definition.ComponentOptionsDataSource;
import com.bytechef.hermes.component.definition.ComponentOptionsDataSource.OptionsFunction;
import com.bytechef.hermes.component.definition.EditorDescriptionFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.SampleOutputDataSource;
import com.bytechef.hermes.definition.DynamicOptionsProperty;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Property.DynamicPropertiesProperty;
import com.bytechef.hermes.definition.registry.component.ComponentDefinitionRegistry;
import com.bytechef.hermes.definition.registry.component.InputParametersImpl;
import com.bytechef.hermes.definition.registry.component.factory.ContextConnectionFactory;
import com.bytechef.hermes.definition.registry.dto.ActionDefinitionDTO;
import com.bytechef.hermes.definition.registry.component.action.CustomAction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
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
    public List<? extends Property<?>> executeDynamicProperties(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, String authorizationName, Map<String, Object> connectionParameters) {

        DynamicPropertiesProperty property = (DynamicPropertiesProperty) componentDefinitionRegistry.getActionProperty(
            propertyName, actionName, componentName, componentVersion);

        ComponentDynamicPropertiesDataSource dynamicPropertiesDataSource =
            (ComponentDynamicPropertiesDataSource) property.getDynamicPropertiesDataSource();

        ComponentDynamicPropertiesDataSource.DynamicPropertiesFunction dynamicPropertiesFunction =
            dynamicPropertiesDataSource.getDynamicProperties();

        return dynamicPropertiesFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(actionParameters));
    }

    @Override
    public String executeEditorDescription(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        ActionDefinition actionDefinition = componentDefinitionRegistry.getActionDefinition(
            actionName, componentName, componentVersion);

        EditorDescriptionFunction editorDescriptionFunction = actionDefinition.getEditorDescription();

        return editorDescriptionFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(actionParameters));
    }

    @Override
    public List<Option<?>> executeOptions(
        String propertyName, String actionName, String componentName, int componentVersion,
        Map<String, Object> actionParameters, String authorizationName, Map<String, Object> connectionParameters) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getActionProperty(propertyName, actionName, componentName, componentVersion);

        ComponentOptionsDataSource optionsDataSource = (ComponentOptionsDataSource) OptionalUtils.get(
            dynamicOptionsProperty.getOptionsDataSource());

        OptionsFunction optionsFunction = optionsDataSource.getOptions();

        return optionsFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(actionParameters));
    }

    @Override
    public List<? extends Property<?>> executeOutputSchema(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        ActionDefinition actionDefinition = componentDefinitionRegistry.getActionDefinition(
            actionName, componentName, componentVersion);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(actionDefinition.getOutputSchemaDataSource());

        OutputSchemaDataSource.OutputSchemaFunction outputSchemaFunction = outputSchemaDataSource.getOutputSchema();

        return outputSchemaFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(actionParameters));
    }

    @Override
    public Object executeSampleOutput(
        String actionName, String componentName, int componentVersion, Map<String, Object> actionParameters,
        String authorizationName, Map<String, Object> connectionParameters) {

        ActionDefinition actionDefinition = componentDefinitionRegistry.getActionDefinition(
            actionName, componentName, componentVersion);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(
            actionDefinition.getSampleOutputDataSource());

        SampleOutputDataSource.SampleOutputFunction sampleOutputFunction = sampleOutputDataSource.getSampleOutput();

        return sampleOutputFunction.apply(
            contextConnectionFactory.createConnection(
                componentName, componentVersion, connectionParameters, authorizationName),
            new InputParametersImpl(actionParameters));
    }

    @Override
    public ActionDefinitionDTO getComponentActionDefinition(
        String actionName, String componentName, int componentVersion) {

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
    public List<ActionDefinitionDTO> getComponentActionDefinitions(String componentName, int componentVersion) {
        List<ActionDefinitionDTO> actionDefinitionDTOs =
            componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
                .stream()
                .map(this::toActionDefinitionDTO)
                .toList();

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        if (OptionalUtils.orElse(componentDefinition.getCustomAction(), false)) {
            actionDefinitionDTOs = new ArrayList<>(actionDefinitionDTOs);

            actionDefinitionDTOs.add(
                toActionDefinitionDTO(CustomAction.getCustomActionDefinition(componentDefinition)));
        }

        return actionDefinitionDTOs;
    }

    private ActionDefinitionDTO toActionDefinitionDTO(ActionDefinition actionDefinition) {
        return new ActionDefinitionDTO(
            OptionalUtils.orElse(actionDefinition.getBatch(), false), actionDefinition.getDescription(),
            OptionalUtils.orElse(actionDefinition.getSampleOutput(), null),
            OptionalUtils.orElse(actionDefinition.getHelp(), null), actionDefinition.getName(),
            OptionalUtils.orElse(actionDefinition.getOutputSchema(), Collections.emptyList()),
            OptionalUtils.orElse(actionDefinition.getProperties(), Collections.emptyList()),
            actionDefinition.getTitle());
    }
}
