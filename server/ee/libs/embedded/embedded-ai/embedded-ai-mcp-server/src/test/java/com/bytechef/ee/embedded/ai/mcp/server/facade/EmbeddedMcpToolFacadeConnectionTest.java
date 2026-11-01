/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class EmbeddedMcpToolFacadeConnectionTest {

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

    @Test
    void testConnectionRequiredWhenComponentDeclaresARequiredConnection() {
        stubComponentDefinition(
            "slack",
            ComponentDsl.component("slack")
                .connection(
                    ComponentDsl.connection()
                        .authorizationRequired(true)));

        assertTrue(EmbeddedMcpToolFacade.isConnectionRequired(componentDefinitionService, "slack", 1));
    }

    @Test
    void testConnectionNotRequiredWhenComponentDeclaresAnOptionalConnection() {
        stubComponentDefinition(
            "httpClient",
            ComponentDsl.component("httpClient")
                .connection(
                    ComponentDsl.connection()
                        .authorizationRequired(false)));

        assertFalse(EmbeddedMcpToolFacade.isConnectionRequired(componentDefinitionService, "httpClient", 1));
    }

    @Test
    void testConnectionNotRequiredForConnectionlessComponent() {
        stubComponentDefinition("logger", ComponentDsl.component("logger"));

        assertFalse(EmbeddedMcpToolFacade.isConnectionRequired(componentDefinitionService, "logger", 1));
    }

    private void stubComponentDefinition(String componentName, ModifiableComponentDefinition componentDefinition) {
        when(componentDefinitionService.getComponentDefinition(componentName, 1))
            .thenReturn(new ComponentDefinition(componentDefinition));
    }
}
