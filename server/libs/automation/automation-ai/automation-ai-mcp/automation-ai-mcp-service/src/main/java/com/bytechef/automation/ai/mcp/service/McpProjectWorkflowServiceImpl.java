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
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpProjectWorkflowService} interface.
 * <p>
 * Every write overload carries its own {@code @PreAuthorize}, including the entity-taking ones that only the id-taking
 * overloads call today. Delegation between them is a {@code this.} call, which the Spring proxy does not intercept, so
 * an entity-taking overload left unguarded would be authorized by nothing at all the moment anything injected this
 * service and called it directly.
 * <p>
 * The two list reads - {@code getMcpProjectMcpProjectWorkflows} and
 * {@code getProjectDeploymentWorkflowMcpProjectWorkflows} - are deliberately NOT annotated. Their callers are trusted
 * internal paths that run without a usable {@code SecurityContext}: the MCP serve path
 * ({@code AutomationMcpToolFacade}, authenticated by a server secret key rather than a user), the delete-cascade
 * listeners, and an agent tool callback on a worker thread. A guard here would reject all three. Authorization for the
 * externally reachable paths sits where the request actually enters: {@code McpProjectWorkflowGraphQlController} guards
 * its by-project query, and the nested {@code McpProject.mcpProjectWorkflows} field resolver is reachable only through
 * {@code McpProjectService}'s already-guarded {@code fetchMcpProject} / {@code getMcpServerMcpProjects}.
 * <p>
 * There is deliberately no unparameterised {@code getMcpProjectWorkflows()}: it returned every row in the table, could
 * not be guarded by id, and its only caller was a GraphQL root query nothing consumed.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpProjectWorkflowServiceImpl implements McpProjectWorkflowService {

    /**
     * Deliberately uniform: a project deployment workflow that does not exist and one owned by somebody else's
     * deployment are indistinguishable to the caller, so a rejection cannot be used to probe for rows.
     */
    private static final String INVALID_PROJECT_DEPLOYMENT_WORKFLOW =
        "Invalid projectDeploymentWorkflowId for the given MCP project";

    // The MCP project is read through its repository rather than McpProjectService, whose read is itself guarded by
    // the McpProject ownership resolver - going through the service would re-enter authorization from inside it.
    private final McpProjectRepository mcpProjectRepository;
    private final McpProjectWorkflowRepository mcpProjectWorkflowRepository;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public McpProjectWorkflowServiceImpl(
        McpProjectRepository mcpProjectRepository, McpProjectWorkflowRepository mcpProjectWorkflowRepository,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.mcpProjectRepository = mcpProjectRepository;
        this.mcpProjectWorkflowRepository = mcpProjectWorkflowRepository;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectWorkflow.mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProjectWorkflow create(McpProjectWorkflow mcpProjectWorkflow) {
        validateProjectDeploymentWorkflow(
            mcpProjectWorkflow.getMcpProjectId(), mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        return mcpProjectWorkflowRepository.save(mcpProjectWorkflow);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProjectWorkflow create(Long mcpProjectId, Long projectDeploymentWorkflowId) {
        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(mcpProjectId, projectDeploymentWorkflowId);

        return create(mcpProjectWorkflow);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')")
    public void delete(long mcpProjectWorkflowId) {
        mcpProjectWorkflowRepository.deleteById(mcpProjectWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_VIEW')")
    public Optional<McpProjectWorkflow> fetchMcpProjectWorkflow(long mcpProjectWorkflowId) {
        return mcpProjectWorkflowRepository.findById(mcpProjectWorkflowId);
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
    @PreAuthorize("hasPermission(#mcpProjectWorkflow.id, 'McpProjectWorkflow', 'MCP_EDIT') and " +
        "hasPermission(#mcpProjectWorkflow.mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProjectWorkflow update(McpProjectWorkflow mcpProjectWorkflow) {
        validateProjectDeploymentWorkflow(
            mcpProjectWorkflow.getMcpProjectId(), mcpProjectWorkflow.getProjectDeploymentWorkflowId());

        McpProjectWorkflow currentMcpProjectWorkflow =
            OptionalUtils.get(mcpProjectWorkflowRepository.findById(mcpProjectWorkflow.getId()));

        currentMcpProjectWorkflow.setMcpProjectId(mcpProjectWorkflow.getMcpProjectId());
        currentMcpProjectWorkflow.setProjectDeploymentWorkflowId(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
        currentMcpProjectWorkflow.setVersion(mcpProjectWorkflow.getVersion());

        return mcpProjectWorkflowRepository.save(currentMcpProjectWorkflow);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpProjectWorkflow', 'MCP_EDIT') and " +
        "(#mcpProjectId == null or hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT'))")
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

    @Override
    @PreAuthorize("hasPermission(#id, 'McpProjectWorkflow', 'MCP_EDIT')")
    public McpProjectWorkflow updateParameters(long id, Map<String, ?> parameters) {
        McpProjectWorkflow existingMcpProjectWorkflow = fetchMcpProjectWorkflow(id)
            .orElseThrow(() -> new IllegalArgumentException("McpProjectWorkflow not found with id: " + id));

        existingMcpProjectWorkflow.setParameters(parameters);

        return mcpProjectWorkflowRepository.save(existingMcpProjectWorkflow);
    }

    private void validateProjectDeploymentWorkflow(Long mcpProjectId, Long projectDeploymentWorkflowId) {
        if (mcpProjectId == null || projectDeploymentWorkflowId == null) {
            throw new IllegalArgumentException(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);
        }

        McpProject mcpProject = mcpProjectRepository.findById(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException(INVALID_PROJECT_DEPLOYMENT_WORKFLOW));

        Long projectDeploymentId = mcpProject.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            throw new IllegalArgumentException(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);
        }

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId);

        boolean owned = projectDeploymentWorkflows.stream()
            .anyMatch(
                projectDeploymentWorkflow -> Objects.equals(
                    projectDeploymentWorkflow.getId(), projectDeploymentWorkflowId));

        if (!owned) {
            throw new IllegalArgumentException(INVALID_PROJECT_DEPLOYMENT_WORKFLOW);
        }
    }
}
