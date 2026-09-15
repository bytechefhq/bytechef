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

package com.bytechef.automation.ai.mcp.service;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.security.McpProjectWorkspaceGuard;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpProjectService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpProjectServiceImpl implements McpProjectService {

    private final McpProjectRepository mcpProjectRepository;
    private final McpProjectWorkspaceGuard mcpProjectWorkspaceGuard;

    @SuppressFBWarnings("EI")
    public McpProjectServiceImpl(
        McpProjectRepository mcpProjectRepository, McpProjectWorkspaceGuard mcpProjectWorkspaceGuard) {

        this.mcpProjectRepository = mcpProjectRepository;
        this.mcpProjectWorkspaceGuard = mcpProjectWorkspaceGuard;
    }

    @Override
    public McpProject create(McpProject mcpProject) {
        return mcpProjectRepository.save(mcpProject);
    }

    // Can move a link to another server or deployment, so it needs MCP_EDIT on the project as it stands and on the
    // server it is moving to, and the result must still keep server and project in one workspace. Nothing calls it
    // today; the guard keeps a future caller from creating the cross-workspace link createMcpProject refuses.
    @Override
    @PreAuthorize("hasPermission(#mcpProject.id, 'McpProject', 'MCP_EDIT') and " +
        "hasPermission(#mcpProject.mcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpProject update(McpProject mcpProject) {
        mcpProjectWorkspaceGuard.requireDeploymentInServerWorkspace(
            mcpProject.getMcpServerId(), mcpProject.getProjectDeploymentId());

        McpProject currentMcpProject = OptionalUtils.get(mcpProjectRepository.findById(mcpProject.getId()));

        currentMcpProject.setProjectDeploymentId(mcpProject.getProjectDeploymentId());
        currentMcpProject.setMcpServerId(mcpProject.getMcpServerId());
        currentMcpProject.setVersion(mcpProject.getVersion());

        return mcpProjectRepository.save(currentMcpProject);
    }

    @Override
    public void delete(long mcpProjectId) {
        mcpProjectRepository.deleteById(mcpProjectId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_VIEW')")
    public Optional<McpProject> fetchMcpProject(long mcpProjectId) {
        return mcpProjectRepository.findById(mcpProjectId);
    }

    @Override
    public List<McpProject> getMcpProjects() {
        return mcpProjectRepository.findAll();
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
    public List<McpProject> getMcpServerMcpProjects(long mcpServerId) {
        return mcpProjectRepository.findAllByMcpServerId(mcpServerId);
    }
}
