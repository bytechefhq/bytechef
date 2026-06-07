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

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class EmbeddedMcpToolFacadeConnectionTest {

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);

    @Test
    void testConnectionRequiredWhenComponentDeclaresConnection() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getConnection()).thenReturn(mock(ConnectionDefinition.class));
        when(componentDefinitionService.getComponentDefinition("slack", 1)).thenReturn(componentDefinition);

        assertTrue(EmbeddedMcpToolFacade.isConnectionRequired(componentDefinitionService, "slack", 1));
    }

    @Test
    void testConnectionNotRequiredForConnectionlessComponent() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getConnection()).thenReturn(null);
        when(componentDefinitionService.getComponentDefinition("embeddedWorkflowBuilder", 1))
            .thenReturn(componentDefinition);

        assertFalse(
            EmbeddedMcpToolFacade.isConnectionRequired(componentDefinitionService, "embeddedWorkflowBuilder", 1));
    }
}
