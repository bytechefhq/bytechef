/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 */
class EmbeddedMcpToolFacadeEnabledTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService =
        mock(McpIntegrationInstanceToolService.class);

    @Test
    void testGetFunctionToolCallbackReturnsNullWhenToolDisabled() {
        EmbeddedMcpToolFacade facade = new EmbeddedMcpToolFacade(
            mock(ObjectProvider.class), mock(ClusterElementDefinitionFacade.class), clusterElementDefinitionService,
            mock(ComponentDefinitionService.class), mock(ConnectedUserService.class), mock(Evaluator.class),
            mock(IntegrationInstanceConfigurationService.class),
            mock(IntegrationInstanceConfigurationWorkflowService.class), mock(IntegrationInstanceService.class),
            mock(IntegrationInstanceWorkflowService.class), mock(IntegrationService.class),
            mock(JobCompletionAwaiter.class), mock(JobResumeFacade.class), mock(JobService.class),
            mock(JwtTokenService.class), mcpComponentService,
            mock(McpIntegrationInstanceConfigurationWorkflowService.class), mcpIntegrationInstanceToolService,
            mock(McpServerService.class), mock(ObjectProvider.class), mock(PrincipalJobFacade.class),
            "http://localhost:9555", mock(TaskExecutionService.class), mock(TaskFileStorage.class),
            mock(ToolExecutionRecorder.class), mock(WorkflowService.class));

        McpTool disabledTool = new McpTool("disabled-tool", Map.of(), 10L);

        disabledTool.setId(1L);
        disabledTool.setEnabled(false);

        assertNull(facade.getFunctionToolCallback(disabledTool, "external-user", Environment.PRODUCTION, "tenant"));

        verifyNoInteractions(mcpComponentService, clusterElementDefinitionService, mcpIntegrationInstanceToolService);
    }
}
