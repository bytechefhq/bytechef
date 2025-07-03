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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.repository.McpProjectWorkflowRepository;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.configuration.domain.McpProjectWorkflow;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpProjectWorkflowService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpProjectWorkflowServiceImpl implements McpProjectWorkflowService {

    private final McpProjectWorkflowRepository mcpProjectWorkflowRepository;

    public McpProjectWorkflowServiceImpl(McpProjectWorkflowRepository mcpProjectWorkflowRepository) {
        this.mcpProjectWorkflowRepository = mcpProjectWorkflowRepository;
    }

    @Override
    public McpProjectWorkflow create(McpProjectWorkflow mcpProjectWorkflow) {
        return mcpProjectWorkflowRepository.save(mcpProjectWorkflow);
    }

    @Override
    public McpProjectWorkflow create(Long mcpProjectId, Long projectDeploymentWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(mcpProjectId, projectDeploymentWorkflowId);

        return create(mcpProjectWorkflow);
    }

    @Override
    public void delete(long mcpProjectWorkflowId) {
        mcpProjectWorkflowRepository.deleteById(mcpProjectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpProjectWorkflow> fetchMcpProjectWorkflow(long mcpProjectWorkflowId) {
        return mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpProjectWorkflow> getMcpProjectWorkflows() {
        return mcpProjectWorkflowRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpProjectWorkflow> getMcpProjectMcpProjectWorkflows(Long mcpProjectId) {
        return mcpProjectWorkflowRepository.findAllByMcpProjectId(mcpProjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpProjectWorkflow> getProjectDeploymentWorkflowMcpProjectWorkflows(Long projectDeploymentWorkflowId) {
        return mcpProjectWorkflowRepository.findAllByProjectDeploymentWorkflowId(projectDeploymentWorkflowId);
    }

    @Override
    public McpProjectWorkflow update(McpProjectWorkflow mcpProjectWorkflow) {
        McpProjectWorkflow currentMcpProjectWorkflow =
            OptionalUtils.get(mcpProjectWorkflowRepository.findById(mcpProjectWorkflow.getId()));

        currentMcpProjectWorkflow.setMcpProjectId(mcpProjectWorkflow.getMcpProjectId());
        currentMcpProjectWorkflow.setProjectDeploymentWorkflowId(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
        currentMcpProjectWorkflow.setVersion(mcpProjectWorkflow.getVersion());

        return mcpProjectWorkflowRepository.save(currentMcpProjectWorkflow);
    }

    @Override
    public McpProjectWorkflow update(long id, Long mcpProjectId, Long projectDeploymentWorkflowId) {
        McpProjectWorkflow existingMcpProjectWorkflow = fetchMcpProjectWorkflow(id)
            .orElseThrow(() -> new IllegalArgumentException("McpProjectWorkflow not found with id: " + id));

        if (mcpProjectId != null) {
            existingMcpProjectWorkflow.setMcpProjectId(mcpProjectId);
        }

        if (projectDeploymentWorkflowId != null) {
            existingMcpProjectWorkflow.setProjectDeploymentWorkflowId(projectDeploymentWorkflowId);
        }

        return update(existingMcpProjectWorkflow);
    }
}
