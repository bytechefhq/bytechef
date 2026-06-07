/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 */
class ToolFacadeImplTest {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ConnectionIdHelper connectionIdHelper = mock(ConnectionIdHelper.class);

    private final ToolFacadeImpl toolFacade = new ToolFacadeImpl(
        clusterElementDefinitionFacade, mock(ClusterElementDefinitionService.class),
        mock(ComponentDefinitionService.class), mock(ConnectedUserService.class), connectionIdHelper,
        mock(IntegrationInstanceConfigurationService.class), mock(IntegrationInstanceService.class),
        mock(IntegrationService.class));

    @Test
    @SuppressWarnings("unchecked")
    void testExecuteToolInjectsReservedContextParameters() {
        when(connectionIdHelper.getConnectionId("user-1", "slack", null, Environment.PRODUCTION)).thenReturn(7L);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);

        when(clusterElementDefinitionFacade.executeTool(eq("slack"), eq("send"), captor.capture(), eq(7L)))
            .thenReturn("ok");

        Map<String, Object> input = new HashMap<>();

        input.put("text", "hi");

        Object result = toolFacade.executeTool("user-1", "slack_send", input, null, Environment.PRODUCTION);

        assertEquals("ok", result);

        Map<String, ?> passed = captor.getValue();

        assertEquals("user-1", passed.get(EmbeddedToolConstants.EXTERNAL_USER_ID));
        assertEquals("PRODUCTION", passed.get(EmbeddedToolConstants.ENVIRONMENT));
        assertEquals("hi", passed.get("text"));
    }
}
