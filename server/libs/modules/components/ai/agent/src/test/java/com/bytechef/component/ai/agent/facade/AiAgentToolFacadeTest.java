/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.agent.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class AiAgentToolFacadeTest {

    private static final String CLUSTER_ELEMENT_DESCRIPTION = "Sends an email through Google Mail.";

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);

    private final AiAgentToolFacade aiAgentToolFacade =
        new AiAgentToolFacade(clusterElementDefinitionService, mock(Evaluator.class));

    @Test
    void testToolNameFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals("GOOGLEMAIL_SEND_EMAIL", toolDefinition.name());
    }

    @Test
    void testToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "mailTheCustomer"));

        assertEquals("mailTheCustomer", toolDefinition.name());
    }

    @Test
    void testToolNameFallsBackWhenTheConfiguredNameIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "   "));

        assertEquals("GOOGLEMAIL_SEND_EMAIL", toolDefinition.name());
    }

    @Test
    void testToolDescriptionFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    @Test
    void testToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "Emails the customer"));

        assertEquals("Emails the customer", toolDefinition.description());
    }

    @Test
    void testToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "   "));

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    @Test
    void testToolNameFallsBackToTheClusterElementForAMultipleConnectionsTool() {
        ToolDefinition toolDefinition = getMultipleConnectionsToolDefinition(Map.of());

        assertEquals("GOOGLEMAIL_SEND_EMAIL", toolDefinition.name());
    }

    @Test
    void testToolDescriptionFallsBackToTheClusterElementForAMultipleConnectionsTool() {
        ToolDefinition toolDefinition = getMultipleConnectionsToolDefinition(Map.of());

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    @Test
    void testAMultipleConnectionsToolUsesTheConfiguredNameAndDescription() {
        ToolDefinition toolDefinition = getMultipleConnectionsToolDefinition(
            Map.of("toolDescription", "Emails the customer", "toolName", "mailTheCustomer"));

        assertEquals("mailTheCustomer", toolDefinition.name());
        assertEquals("Emails the customer", toolDefinition.description());
    }

    private ToolDefinition getMultipleConnectionsToolDefinition(Map<String, ?> parameters) {
        ClusterElement clusterElement = createClusterElement(parameters);

        ToolCallback toolCallback = aiAgentToolFacade.getFunctionToolCallback(
            clusterElement, Map.of(), mock(ActionContext.class));

        return toolCallback.getToolDefinition();
    }

    private ClusterElement createClusterElement(Map<String, ?> parameters) {
        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("googleMail");
        when(clusterElementDefinition.getName()).thenReturn("sendEmail");
        when(clusterElementDefinition.getDescription()).thenReturn(CLUSTER_ELEMENT_DESCRIPTION);
        when(clusterElementDefinitionService.getClusterElementDefinition("googleMail", 1, "sendEmail"))
            .thenReturn(clusterElementDefinition);

        return new ClusterElement(
            mock(com.bytechef.platform.configuration.domain.ComponentConnection.class), "A tool", Map.of(),
            "Send Email", "googleMail/v1/sendEmail", parameters, "aiAgent_1");
    }

    private ToolDefinition getToolDefinition(Map<String, ?> parameters) {
        ClusterElement clusterElement = createClusterElement(parameters);

        ToolCallback toolCallback = aiAgentToolFacade.getFunctionToolCallback(
            clusterElement, mock(ComponentConnection.class), mock(ActionContext.class));

        return toolCallback.getToolDefinition();
    }
}
