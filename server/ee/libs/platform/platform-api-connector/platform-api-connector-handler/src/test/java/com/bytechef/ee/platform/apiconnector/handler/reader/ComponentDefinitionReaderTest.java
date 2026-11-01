/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.handler.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentDefinitionReaderTest {

    private static final String LEGACY_DEFINITION = """
        {
          "name": "legacyConnector",
          "title": "Legacy Connector",
          "triggers": null,
          "actions": [ {
            "name": "getItem",
            "title": "Get Item",
            "batch": null,
            "deprecated": null,
            "metadata": null,
            "properties": [ {
              "type": "STRING",
              "name": "itemId",
              "advancedOption": null,
              "expressionEnabled": null,
              "hidden": null,
              "metadata": null,
              "options": null,
              "required": null
            } ]
          }, {
            "name": "listItems",
            "title": "List Items",
            "batch": null,
            "deprecated": null,
            "properties": null
          } ]
        }
        """;

    @Test
    void testReadComponentDefinitionAppliesDefaultsToLegacyNullValues() {
        ApiConnectorFileStorage apiConnectorFileStorage = mock(ApiConnectorFileStorage.class);

        when(apiConnectorFileStorage.readApiConnectorDefinition(any())).thenReturn(LEGACY_DEFINITION);

        ComponentDefinitionReader componentDefinitionReader = new ComponentDefinitionReader(
            apiConnectorFileStorage, JsonMapper.builder()
                .build());

        ComponentDefinitionWrapper componentDefinition = componentDefinitionReader.readComponentDefinition(
            new ApiConnector());

        assertThat(componentDefinition.getTriggers()).isEmpty();

        List<? extends ActionDefinition> actionDefinitions = componentDefinition.getActions();

        assertThat(actionDefinitions).hasSize(2);

        ActionDefinition getItemActionDefinition = actionDefinitions.getFirst();

        assertThat(getItemActionDefinition.getBatch()).isFalse();
        assertThat(getItemActionDefinition.getDeprecated()).isFalse();
        assertThat(getItemActionDefinition.getMetadata()).isEmpty();

        Property property = getItemActionDefinition.getProperties()
            .getFirst();

        assertThat(property.getAdvancedOption()).isFalse();
        assertThat(property.getExpressionEnabled()).isTrue();
        assertThat(property.getHidden()).isFalse();
        assertThat(property.getMetadata()).isEmpty();
        assertThat(property.getRequired()).isFalse();

        ActionDefinition listItemsActionDefinition = actionDefinitions.get(1);

        assertThat(listItemsActionDefinition.getBatch()).isFalse();
        assertThat(listItemsActionDefinition.getProperties()).isEmpty();
    }
}
