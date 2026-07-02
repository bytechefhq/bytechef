/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO.OAuth2;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentInputReferenceModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentPropertyGroupModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentPropertyModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputTypeModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpIntegrationInstanceToolModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpToolModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OAuth2Model;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OptionModel;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.BooleanProperty;
import com.bytechef.platform.component.domain.DateProperty;
import com.bytechef.platform.component.domain.DateTimeProperty;
import com.bytechef.platform.component.domain.IntegerProperty;
import com.bytechef.platform.component.domain.NumberProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.OptionsDataSourceAware;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.PropertyGroup;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.domain.TimeProperty;
import com.bytechef.platform.component.domain.ValueProperty;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserIntegrationMapper {

    @Mapper(config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    interface ConnectedUserIntegrationToIntegrationBasicMapper
        extends Converter<ConnectedUserIntegrationDTO, IntegrationBasicModel> {

        @Override
        @Mapping(target = "componentName", source = "integrationInstanceConfiguration.integration.componentName")
        @Mapping(target = "description", source = "integrationInstanceConfiguration.integration.description")
        @Mapping(target = "icon", source = "integrationInstanceConfiguration.integration.icon")
        @Mapping(target = "id", source = "integrationInstanceConfiguration.integrationId")
        @Mapping(target = "integrationVersion", source = "integrationInstanceConfiguration.integrationVersion")
        @Mapping(
            target = "multipleInstances", source = "integrationInstanceConfiguration.integration.multipleInstances")
        @Mapping(target = "name", source = "integrationInstanceConfiguration.integration.title")
        IntegrationBasicModel convert(ConnectedUserIntegrationDTO connectedUserIntegrationDTO);

        @Mapping(target = "enabled", source = "integrationInstance.enabled")
        @Mapping(target = "credentialStatus", source = "connection.credentialStatus")
        @Mapping(target = "id", source = "integrationInstance.id")
        @Mapping(target = "mcpTools", source = "mcpTools")
        @Mapping(target = "mcpWorkflows", source = "mcpWorkflows")
        IntegrationInstanceModel map(ConnectedUserIntegrationInstance integrationInstance);

        McpIntegrationInstanceToolModel map(ConnectedUserIntegrationDTO.McpInstanceToolInfo mcpInstanceToolInfo);

        @Mapping(target = "enabled", source = "integrationInstanceWorkflow.enabled")
        @Mapping(target = "inputs", source = "integrationInstanceWorkflow.inputs")
        @Mapping(target = "workflowUuid", source = "integrationInstanceWorkflow.workflowUuid")
        IntegrationInstanceWorkflowModel map(
            ConnectedUserIntegrationInstanceWorkflow integrationInstanceWorkflow);
    }

    @Mapper(
        config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    interface ConnectedUserIntegrationToIntegrationMapper
        extends Converter<ConnectedUserIntegrationDTO, IntegrationModel> {

        @Override
        @Mapping(target = "componentName", source = "integrationInstanceConfiguration.integration.componentName")
        @Mapping(target = "description", source = "integrationInstanceConfiguration.integration.description")
        @Mapping(target = "icon", source = "integrationInstanceConfiguration.integration.icon")
        @Mapping(target = "id", source = "integrationInstanceConfiguration.integrationId")
        @Mapping(target = "integrationVersion", source = "integrationInstanceConfiguration.integrationVersion")
        @Mapping(target = "mcpTools", source = "mcpTools")
        @Mapping(target = "mcpWorkflows", source = "mcpWorkflows")
        @Mapping(
            target = "multipleInstances", source = "integrationInstanceConfiguration.integration.multipleInstances")
        @Mapping(target = "name", source = "integrationInstanceConfiguration.integration.title")
        @Mapping(
            target = "workflows", source = "integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows")
        IntegrationModel convert(ConnectedUserIntegrationDTO connectedUserIntegrationDTO);

        @Mapping(target = "authorizationUrl", source = "oAuth2.oAuth2AuthorizationParameters.authorizationUrl")
        @Mapping(target = "clientId", source = "oAuth2.oAuth2AuthorizationParameters.clientId")
        @Mapping(target = "extraQueryParameters", source = "oAuth2.oAuth2AuthorizationParameters.extraQueryParameters")
        @Mapping(target = "scopes", source = "oAuth2.oAuth2AuthorizationParameters.scopes")
        OAuth2Model map(OAuth2 oAuth2);

        @Mapping(target = "description", source = "workflow.description")
        @Mapping(
            target = "inputs",
            expression = "java(mapInputs(integrationInstanceConfigurationWorkflowDTO))")
        @Mapping(target = "label", source = "workflow.label")
        IntegrationWorkflowModel map(
            IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO);

        @Mapping(target = "id", ignore = true)
        McpToolModel map(ConnectedUserIntegrationDTO.McpToolInfo mcpToolInfo);

        @Mapping(target = "componentReference", ignore = true)
        InputModel map(ConnectedUserIntegrationDTO.Input input);

        @Mapping(target = "description", source = "description")
        @Mapping(target = "inputs", source = "inputs")
        @Mapping(target = "label", source = "label")
        @Mapping(target = "workflowUuid", source = "workflowUuid")
        IntegrationWorkflowModel map(ConnectedUserIntegrationDTO.McpWorkflowInfo mcpWorkflowInfo);

        McpIntegrationInstanceToolModel map(ConnectedUserIntegrationDTO.McpInstanceToolInfo mcpInstanceToolInfo);

        @Mapping(target = "enabled", source = "integrationInstance.enabled")
        @Mapping(target = "credentialStatus", source = "connection.credentialStatus")
        @Mapping(target = "id", source = "integrationInstance.id")
        @Mapping(target = "mcpTools", source = "mcpTools")
        @Mapping(target = "mcpWorkflows", source = "mcpWorkflows")
        IntegrationInstanceModel map(ConnectedUserIntegrationInstance integrationInstance);

        @Mapping(target = "enabled", source = "integrationInstanceWorkflow.enabled")
        @Mapping(target = "inputs", source = "integrationInstanceWorkflow.inputs")
        @Mapping(target = "workflowUuid", source = "integrationInstanceWorkflow.workflowUuid")
        IntegrationInstanceWorkflowModel map(
            ConnectedUserIntegrationInstanceWorkflow integrationInstanceWorkflow);

        default List<InputModel> mapInputs(
            IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO) {

            Workflow workflow = integrationInstanceConfigurationWorkflowDTO.workflow();

            if (workflow == null) {
                return List.of();
            }

            Map<String, PropertyGroup> componentInputGroups =
                integrationInstanceConfigurationWorkflowDTO.componentInputGroups();

            Map<String, PropertyGroup> resolvedGroups =
                componentInputGroups == null ? Map.of() : componentInputGroups;

            return WorkflowInput.of(workflow)
                .stream()
                .map(input -> {
                    InputModel inputModel = map(input);

                    PropertyGroup propertyGroup = resolvedGroups.get(input.getName());

                    if (propertyGroup != null && inputModel.getComponentReference() != null) {
                        inputModel.getComponentReference()
                            .group(map(propertyGroup));
                    }

                    return inputModel;
                })
                .toList();
        }

        default InputModel map(WorkflowInput input) {
            InputModel inputModel = new InputModel()
                .internalOnly(input.isInternalOnly())
                .label(input.getLabel())
                .name(input.getName())
                .objectName(input.getObjectName())
                .required(input.isRequired())
                .type(InputTypeModel.valueOf(StringUtils.upperCase(input.getType())));

            WorkflowInput.ComponentInputReference componentReference = input.getComponentInputReference();

            if (componentReference != null) {
                inputModel.componentReference(
                    new ComponentInputReferenceModel(
                        componentReference.componentName(), componentReference.componentVersion(),
                        componentReference.groupName()));
            }

            return inputModel;
        }

        default InputModel mapWorkflowInput(ConnectedUserIntegrationDTO.WorkflowInputInfo input) {
            return new InputModel()
                .label(input.label())
                .name(input.name())
                .required(input.required())
                .type(InputTypeModel.valueOf(StringUtils.upperCase(input.type())));
        }

        default ComponentPropertyGroupModel map(PropertyGroup propertyGroup) {
            ComponentPropertyGroupModel componentPropertyGroupModel =
                new ComponentPropertyGroupModel(propertyGroup.getName())
                    .label(propertyGroup.getLabel());

            for (Property property : propertyGroup.getProperties()) {
                componentPropertyGroupModel.addPropertiesItem(map(property));
            }

            return componentPropertyGroupModel;
        }

        default ComponentPropertyModel map(Property property) {
            ComponentPropertyModel componentPropertyModel =
                new ComponentPropertyModel(property.getName(), toInputType(property.getType()))
                    .required(property.getRequired());

            if (property instanceof ValueProperty<?> valueProperty) {
                componentPropertyModel.label(valueProperty.getLabel());

                var controlType = valueProperty.getControlType();

                if (controlType != null) {
                    componentPropertyModel.controlType(controlType.name());
                }
            }

            for (Option option : getStaticOptions(property)) {
                componentPropertyModel.addOptionsItem(
                    new OptionModel(option.getLabel(), String.valueOf(option.getValue())));
            }

            if (property instanceof OptionsDataSourceAware optionsDataSourceAware) {
                OptionsDataSource optionsDataSource = optionsDataSourceAware.getOptionsDataSource();

                if (optionsDataSource != null) {
                    componentPropertyModel.dynamicOptions(true);

                    for (String optionsLookupDependsOn : optionsDataSource.getOptionsLookupDependsOn()) {
                        componentPropertyModel.addOptionsLookupDependsOnItem(optionsLookupDependsOn);
                    }
                }
            }

            return componentPropertyModel;
        }

        private static InputTypeModel toInputType(Type type) {
            return switch (type) {
                case BOOLEAN -> InputTypeModel.BOOLEAN;
                case DATE -> InputTypeModel.DATE;
                case DATE_TIME -> InputTypeModel.DATE_TIME;
                case INTEGER -> InputTypeModel.INTEGER;
                case NUMBER -> InputTypeModel.NUMBER;
                case TIME -> InputTypeModel.TIME;
                default -> InputTypeModel.STRING;
            };
        }

        private static List<Option> getStaticOptions(Property property) {
            List<Option> options = switch (property) {
                case StringProperty stringProperty -> stringProperty.getOptions();
                case IntegerProperty integerProperty -> integerProperty.getOptions();
                case NumberProperty numberProperty -> numberProperty.getOptions();
                case BooleanProperty booleanProperty -> booleanProperty.getOptions();
                case DateProperty dateProperty -> dateProperty.getOptions();
                case DateTimeProperty dateTimeProperty -> dateTimeProperty.getOptions();
                case TimeProperty timeProperty -> timeProperty.getOptions();
                case ObjectProperty objectProperty -> objectProperty.getOptions();
                case ArrayProperty arrayProperty -> arrayProperty.getOptions();
                default -> List.of();
            };

            return options == null ? List.of() : options;
        }
    }
}
