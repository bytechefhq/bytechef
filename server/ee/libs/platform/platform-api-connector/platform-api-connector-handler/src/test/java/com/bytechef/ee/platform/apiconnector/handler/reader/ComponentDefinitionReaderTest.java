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
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.apiconnector.file.storage.ApiConnectorFileStorage;
import com.bytechef.platform.component.definition.ComponentDefinitionWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins backward compatibility of {@link ComponentDefinitionReader} with API connector definitions stored before the SDK
 * definition getters were migrated from {@code Optional}-wrapped to plain types. Those legacy JSON files carry explicit
 * {@code null} values for fields like {@code batch}, {@code deprecated} and {@code customAction} (serialized from
 * {@code Optional.empty()}), which must not clobber the non-null field defaults of the Modifiable* DSL classes during
 * deserialization.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentDefinitionReaderTest {

    private final ApiConnectorFileStorage apiConnectorFileStorage = mock(ApiConnectorFileStorage.class);

    @Test
    void testReadComponentDefinitionWithLegacyNullFields() {
        when(apiConnectorFileStorage.readApiConnectorDefinition(any())).thenReturn(
            """
                {
                    "actions": [
                        {
                            "batch": null,
                            "deprecated": null,
                            "description": "Gets a pet.",
                            "metadata": null,
                            "name": "getPet",
                            "properties": null,
                            "title": "Get Pet"
                        }
                    ],
                    "customAction": null,
                    "name": "petstore",
                    "version": 1
                }
                """);

        ComponentDefinitionReader componentDefinitionReader = new ComponentDefinitionReader(
            apiConnectorFileStorage, JsonMapper.builder()
                .build());

        ApiConnector apiConnector = new ApiConnector();

        apiConnector.setDefinition(new FileEntry("definition.json", "base64:ZGVmaW5pdGlvbg=="));

        ComponentDefinitionWrapper componentDefinitionWrapper = componentDefinitionReader.readComponentDefinition(
            apiConnector);

        assertThat(componentDefinitionWrapper.getName()).isEqualTo("petstore");

        List<ActionDefinition> actionDefinitions = componentDefinitionWrapper.getActions();

        assertThat(actionDefinitions).hasSize(1);

        ActionDefinition actionDefinition = actionDefinitions.getFirst();

        assertThat(actionDefinition.getBatch()).isFalse();
        assertThat(actionDefinition.getDeprecated()).isFalse();
        assertThat(actionDefinition.getProperties()).isEmpty();
    }
}
