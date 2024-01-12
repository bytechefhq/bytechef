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

package com.bytechef.platform.component.registry.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.EditorDescriptionDataSource;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OutputSchemaDataSource;
import com.bytechef.component.definition.OutputSchemaDataSource.ActionOutputSchemaFunction;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.Property.DynamicPropertiesProperty;
import com.bytechef.component.definition.SampleOutputDataSource;
import com.bytechef.component.definition.SampleOutputDataSource.ActionSampleOutputFunction;
import com.bytechef.platform.component.definition.ParametersImpl;
import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.platform.component.registry.ComponentDefinitionRegistry;
import com.bytechef.platform.component.registry.OperationType;
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.domain.Option;
import com.bytechef.platform.component.registry.domain.Property;
import com.bytechef.platform.component.registry.domain.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.lang.NonNull;
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
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        PropertiesDataSource.ActionPropertiesFunction propertiesFunction = getComponentPropertiesFunction(
            componentName, componentVersion, actionName, propertyName);

        try {
            return CollectionUtils.map(
                propertiesFunction.apply(
                    new ParametersImpl(inputParameters),
                    connection == null ? null : new ParametersImpl(connection.parameters()), context),
                valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }
    }

    @Override
    public String executeEditorDescription(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        EditorDescriptionDataSource.ActionEditorDescriptionFunction editorDescriptionFunction =
            getEditorDescriptionFunction(
                componentName, componentVersion, actionName);

        try {
            return editorDescriptionFunction.apply(new ParametersImpl(inputParameters), context);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }
    }

    @Override
    public List<Option> executeOptions(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @NonNull String propertyName,
        @NonNull Map<String, ?> inputParameters, String searchText, ComponentConnection connection,
        @NonNull ActionContext context) {

        OptionsDataSource.ActionOptionsFunction optionsFunction = getComponentOptionsFunction(
            componentName, componentVersion, actionName, propertyName);

        try {
            return CollectionUtils.map(
                optionsFunction.apply(
                    new ParametersImpl(inputParameters),
                    connection == null ? null : new ParametersImpl(connection.parameters()), searchText, context),
                Option::new);
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }
    }

    @Override
    public Property executeOutputSchema(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        ActionOutputSchemaFunction outputSchemaFunction = getOutputSchemaFunction(
            componentName, componentVersion, actionName);

        try {
            return Property.toProperty(
                outputSchemaFunction.apply(
                    new ParametersImpl(inputParameters),
                    connection == null ? null : new ParametersImpl(connection.parameters()), context));
        } catch (Exception e) {
            throw new ComponentExecutionException(e, inputParameters);
        }
    }

    @Override
    public Object executePerform(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            resolveActionDefinition(componentName, componentVersion, actionName);

        return OptionalUtils.mapOrElse(
            actionDefinition.getPerform(), performFunction -> {
                try {
                    return performFunction.apply(
                        new ParametersImpl(inputParameters),
                        connection == null ? null : new ParametersImpl(connection.parameters()), context);
                } catch (Exception e) {
                    throw new ComponentExecutionException(e, inputParameters);
                }
            },
            null);
    }

    @Override
    public Object executeSampleOutput(
        @NonNull String componentName, int componentVersion, @NonNull String actionName,
        @NonNull Map<String, ?> inputParameters, ComponentConnection connection, @NonNull ActionContext context) {

        ActionSampleOutputFunction sampleOutputFunction = getSampleOutputFunction(
            componentName, componentVersion, actionName);

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

        return sampleOutput;
    }

    @Override
    public ActionDefinition
        getActionDefinition(@NonNull String componentName, int componentVersion, @NonNull String actionName) {
        return new ActionDefinition(resolveActionDefinition(componentName, componentVersion, actionName));
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull String componentName, int componentVersion) {
        return componentDefinitionRegistry.getActionDefinitions(componentName, componentVersion)
            .stream()
            .map(ActionDefinition::new)
            .toList();
    }

    @Override
    public List<ActionDefinition> getActionDefinitions(@NonNull List<OperationType> operationTypes) {
        List<ActionDefinition> actionDefinitions;

        if (operationTypes.isEmpty()) {
            actionDefinitions = CollectionUtils.map(
                componentDefinitionRegistry.getActionDefinitions(), ActionDefinition::new);
        } else {
            actionDefinitions = CollectionUtils.map(
                operationTypes,
                componentOperation -> getActionDefinition(
                    componentOperation.componentName(), componentOperation.componentVersion(),
                    componentOperation.componentOperationName()));
        }

        return actionDefinitions;
    }

    private OptionsDataSource.ActionOptionsFunction getComponentOptionsFunction(
        String componentName, int componentVersion, String actionName, String propertyName) {

        DynamicOptionsProperty dynamicOptionsProperty = (DynamicOptionsProperty) componentDefinitionRegistry
            .getActionProperty(componentName, componentVersion, actionName, propertyName);

        OptionsDataSource optionsDataSource = OptionalUtils.get(dynamicOptionsProperty.getOptionsDataSource());

        return (OptionsDataSource.ActionOptionsFunction) optionsDataSource.getOptions();
    }

    private PropertiesDataSource.ActionPropertiesFunction getComponentPropertiesFunction(
        String componentName, int componentVersion, String actionName, String propertyName) {

        DynamicPropertiesProperty dynamicPropertiesProperty =
            (DynamicPropertiesProperty) componentDefinitionRegistry.getActionProperty(
                componentName, componentVersion, actionName, propertyName);

        PropertiesDataSource propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        return (PropertiesDataSource.ActionPropertiesFunction) propertiesDataSource.getProperties();

    }

    private EditorDescriptionDataSource.ActionEditorDescriptionFunction getEditorDescriptionFunction(
        String componentName, int componentVersion, String actionName) {

        ComponentDefinition componentDefinition = componentDefinitionRegistry.getComponentDefinition(
            componentName, componentVersion);

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        getActionDefinition(componentName, componentVersion, actionName);

        return OptionalUtils.mapOrElse(
            actionDefinition.getEditorDescriptionDataSource(),
            editorDescriptionDataSource -> (EditorDescriptionDataSource.ActionEditorDescriptionFunction) editorDescriptionDataSource
                .getEditorDescription(),
            (inputParameters, context) -> OptionalUtils.orElse(
                componentDefinition.getTitle(), componentDefinition.getName()) + ": " +
                OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName()));
    }

    private ActionOutputSchemaFunction getOutputSchemaFunction(
        String componentName, int componentVersion, String actionName) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        OutputSchemaDataSource outputSchemaDataSource = OptionalUtils.get(actionDefinition.getOutputSchemaDataSource());

        return (ActionOutputSchemaFunction) outputSchemaDataSource.getOutputSchema();
    }

    private ActionSampleOutputFunction getSampleOutputFunction(
        String componentName, int componentVersion, String actionName) {

        com.bytechef.component.definition.ActionDefinition actionDefinition =
            componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);

        SampleOutputDataSource sampleOutputDataSource = OptionalUtils.get(actionDefinition.getSampleOutputDataSource());

        return (ActionSampleOutputFunction) sampleOutputDataSource.getSampleOutput();
    }

    private com.bytechef.component.definition.ActionDefinition resolveActionDefinition(
        String componentName, int componentVersion, String actionName) {

        return componentDefinitionRegistry.getActionDefinition(componentName, componentVersion, actionName);
    }
}
