/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.component.definition.Property.Type;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentInputReferenceModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentPropertyGroupModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentPropertyModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputTypeModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpIntegrationInstanceToolModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpToolModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OAuth2Model;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.PropertyGroup;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectedUserIntegrationMapperTest {

    private final ConnectedUserIntegrationMapper.ConnectedUserIntegrationToIntegrationMapper mapper =
        new ConnectedUserIntegrationMapper.ConnectedUserIntegrationToIntegrationMapper() {

            @Override
            public IntegrationModel convert(ConnectedUserIntegrationDTO source) {
                throw new UnsupportedOperationException();
            }

            @Override
            public OAuth2Model map(ConnectedUserIntegrationDTO.OAuth2 oAuth2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IntegrationWorkflowModel map(
                IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO) {

                throw new UnsupportedOperationException();
            }

            @Override
            public McpToolModel map(ConnectedUserIntegrationDTO.McpToolInfo mcpToolInfo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputModel map(ConnectedUserIntegrationDTO.Input input) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IntegrationWorkflowModel map(ConnectedUserIntegrationDTO.McpWorkflowInfo mcpWorkflowInfo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public McpIntegrationInstanceToolModel map(
                ConnectedUserIntegrationDTO.McpInstanceToolInfo mcpInstanceToolInfo) {

                throw new UnsupportedOperationException();
            }

            @Override
            public IntegrationInstanceModel map(
                ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance integrationInstance) {

                throw new UnsupportedOperationException();
            }

            @Override
            public IntegrationInstanceWorkflowModel map(
                ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstanceWorkflow integrationInstanceWorkflow) {

                throw new UnsupportedOperationException();
            }
        };

    @Test
    void testFieldMappingInputMapsTypeAndObjectName() {
        WorkflowInput input = new WorkflowInput(
            new Workflow.Input(
                "contactMapping", "Contact Mapping", "field_mapping", false,
                Map.of("objectName", "Contacts")));

        InputModel model = mapper.map(input);

        assertEquals(InputTypeModel.FIELD_MAPPING, model.getType());
        assertEquals("Contacts", model.getObjectName());
    }

    @Test
    void testInputMapsInternalOnly() {
        WorkflowInput input = new WorkflowInput(
            new Workflow.Input(
                "apiKey", "API Key", "field_mapping", false,
                Map.of("internalOnly", true)));

        InputModel model = mapper.map(input);

        assertEquals(Boolean.TRUE, model.getInternalOnly());
    }

    @Test
    void testPlainInputHasNoComponentReference() {
        WorkflowInput input = new WorkflowInput(new Workflow.Input("text", "Text", "string", false));

        InputModel model = mapper.map(input);

        assertNull(model.getComponentReference());
    }

    @Test
    void testComponentReferenceInputMapsFlatReference() {
        WorkflowInput input = new WorkflowInput(
            new Workflow.Input(
                "channel", "Channel", "string", false,
                Map.of("componentName", "slack", "componentVersion", 1, "groupName", "channel")));

        InputModel model = mapper.map(input);

        ComponentInputReferenceModel componentReference = model.getComponentReference();

        assertEquals("slack", componentReference.getComponentName());
        assertEquals(1, componentReference.getComponentVersion());
        assertEquals("channel", componentReference.getGroupName());
        assertNull(componentReference.getGroup());
    }

    @Test
    void testPropertyGroupMapsToResolvedGroupWithDynamicOptions() {
        StringProperty channelProperty = mock(StringProperty.class);

        when(channelProperty.getName()).thenReturn("channel");
        when(channelProperty.getLabel()).thenReturn("Channel");
        when(channelProperty.getType()).thenReturn(Type.STRING);
        when(channelProperty.getRequired()).thenReturn(true);

        OptionsDataSource optionsDataSource = mock(OptionsDataSource.class);

        when(optionsDataSource.getOptionsLookupDependsOn()).thenReturn(List.of());
        when(channelProperty.getOptionsDataSource()).thenReturn(optionsDataSource);

        PropertyGroup propertyGroup = mock(PropertyGroup.class);

        when(propertyGroup.getName()).thenReturn("channel");
        doReturn(List.of(channelProperty)).when(propertyGroup)
            .getProperties();

        ComponentPropertyGroupModel groupModel = mapper.map(propertyGroup);

        assertEquals("channel", groupModel.getName());
        assertEquals(1, groupModel.getProperties()
            .size());

        ComponentPropertyModel propertyModel = groupModel.getProperties()
            .get(0);

        assertEquals("channel", propertyModel.getName());
        assertEquals("Channel", propertyModel.getLabel());
        assertEquals(InputTypeModel.STRING, propertyModel.getType());
        assertEquals(Boolean.TRUE, propertyModel.getRequired());
        assertTrue(propertyModel.getDynamicOptions());
    }
}
