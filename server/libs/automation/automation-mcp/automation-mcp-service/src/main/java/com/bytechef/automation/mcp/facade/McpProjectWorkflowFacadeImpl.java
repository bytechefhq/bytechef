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

package com.bytechef.automation.mcp.facade;

import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link McpProjectWorkflowFacade}.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpProjectWorkflowFacadeImpl implements McpProjectWorkflowFacade {

    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowFacadeImpl(
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public void deleteMcpProjectWorkflow(long mcpProjectWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.fetchMcpProjectWorkflow(mcpProjectWorkflowId)
            .orElseThrow(
                () -> new IllegalArgumentException("McpProjectWorkflow not found: " + mcpProjectWorkflowId));

        mcpProjectWorkflowService.delete(mcpProjectWorkflowId);

        projectDeploymentWorkflowService.delete(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
    }
}
