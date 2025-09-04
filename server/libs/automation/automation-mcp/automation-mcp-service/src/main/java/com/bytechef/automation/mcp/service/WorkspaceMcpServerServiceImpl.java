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

package com.bytechef.automation.mcp.service;

import com.bytechef.automation.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.mcp.repository.WorkspaceMcpServerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link WorkspaceMcpServerService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceMcpServerServiceImpl implements WorkspaceMcpServerService {

    private final WorkspaceMcpServerRepository workspaceMcpServerRepository;

    public WorkspaceMcpServerServiceImpl(
        WorkspaceMcpServerRepository workspaceMcpServerRepository) {

        this.workspaceMcpServerRepository = workspaceMcpServerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMcpServer> getWorkspaceMcpServers(Long workspaceId) {
        return workspaceMcpServerRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public void assignMcpServerToWorkspace(Long mcpServerId, Long workspaceId) {
        WorkspaceMcpServer existing = workspaceMcpServerRepository.findByWorkspaceIdAndMcpServerId(
            workspaceId, mcpServerId);

        if (existing == null) {
            WorkspaceMcpServer workspaceMcpServer = new WorkspaceMcpServer(mcpServerId, workspaceId);

            workspaceMcpServerRepository.save(workspaceMcpServer);
        }
    }

    @Override
    public void removeMcpServerFromWorkspace(Long mcpServerId) {
        List<WorkspaceMcpServer> existingRelationships = workspaceMcpServerRepository.findByMcpServerId(mcpServerId);

        if (!existingRelationships.isEmpty()) {
            workspaceMcpServerRepository.deleteAll(existingRelationships);
        }
    }
}
