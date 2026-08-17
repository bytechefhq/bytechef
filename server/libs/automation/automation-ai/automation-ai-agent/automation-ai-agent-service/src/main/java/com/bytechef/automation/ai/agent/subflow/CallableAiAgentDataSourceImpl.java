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

package com.bytechef.automation.ai.agent.subflow;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link CallableAiAgentDataSource} for the {@code workflow} component's {@code callAiAgent} cluster
 * element — bridges the SPI seam in {@code platform-workflow-task-dispatcher-api} to the real
 * {@code automation-ai-agent} domain, mirroring {@code SubflowDataSourceImpl}/{@code SubflowResolverImpl}'s split of
 * picker-listing vs. runtime resolution (including the same workspace-accessibility posture, see
 * {@link #fetchAccessibleWorkspaceIds()}), but keyed by {@code AiAgent.uuid} rather than a raw workflow uuid.
 *
 * @author Ivica Cardic
 */
@Component
class CallableAiAgentDataSourceImpl implements CallableAiAgentDataSource {

    private final AiAgentService agentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    CallableAiAgentDataSourceImpl(
        AiAgentService agentService, ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        UserService userService, WorkspaceFacade workspaceFacade) {

        this.agentService = agentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    public List<CallableAiAgentEntry> getCallableAgents(String search) {
        String lowerCaseSearch = (search == null) ? null : search.toLowerCase();

        // Scope the picker to the caller's accessible workspaces so it cannot enumerate other workspaces' agents
        // (cross-workspace IDOR) -- same posture as SubflowDataSourceImpl#getSubWorkflows, including the
        // no-principal fail-open fallback (see fetchAccessibleWorkspaceIds's doc).
        Set<Long> accessibleWorkspaceIds = fetchAccessibleWorkspaceIds();

        List<CallableAiAgentEntry> callableAgentEntries = new ArrayList<>();

        for (AiAgent agent : agentService.getAgents(null)) {
            Project project = projectService.getProject(agent.getProjectId());

            if (accessibleWorkspaceIds != null && !accessibleWorkspaceIds.contains(project.getWorkspaceId())) {
                continue;
            }

            if (!hasPublishedVersion(project)) {
                continue;
            }

            String title = agent.getTitle();

            if (lowerCaseSearch == null || lowerCaseSearch.isEmpty() ||
                title.toLowerCase()
                    .contains(lowerCaseSearch)) {

                callableAgentEntries.add(
                    new CallableAiAgentEntry(
                        agent.getUuid()
                            .toString(),
                        title, agent.getDescription()));
            }
        }

        return callableAgentEntries;
    }

    @Override
    public ResolvedAiAgent resolveAgent(String agentUuid, boolean editorEnvironment) {
        AiAgent agent = fetchAgentByUuid(agentUuid)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found for uuid " + agentUuid));

        Project project = projectService.getProject(agent.getProjectId());

        Set<Long> accessibleWorkspaceIds = fetchAccessibleWorkspaceIds();

        if (accessibleWorkspaceIds != null && !accessibleWorkspaceIds.contains(project.getWorkspaceId())) {
            throw new IllegalArgumentException(
                "Agent '" + agent.getTitle() + "' (" + agentUuid + ") is not accessible");
        }

        // In a non-editor (production dispatch) run there is no draft fallback -- the target agent must have been
        // published at least once, same invariant AiAgentFacadeImpl.validateForPublish enforces at SUB_AGENT-publish
        // time for the generator's own callAiAgent rows. The editor is more permissive (an agent can be test-called
        // from the canvas before it has ever been published), mirroring SubflowResolverImpl's own
        // editorEnvironment-gated draft-vs-published split.
        if (!editorEnvironment && !hasPublishedVersion(project)) {
            throw new IllegalArgumentException(
                "Agent '" + agent.getTitle() + "' (" + agentUuid + ") has no published version");
        }

        // ProjectWorkflow.uuid is lineage-stable across every version of a project (draft and every published
        // duplicate share it -- see AiAgentFacadeImpl#resolveSubAgentRef's identical citation), so any version's
        // ProjectWorkflow row yields the same value SubflowResolver#resolveSubflow later re-resolves to a concrete
        // workflow id, itself re-applying the draft-vs-published split via editorEnvironment.
        String workflowId = getVersionWorkflowId(project.getId(), project.getLastProjectVersion());
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return new ResolvedAiAgent(projectWorkflow.getUuidAsString(), agent.getName(), agent.getDescription());
    }

    /**
     * Resolves the current user's accessible workspace ids, or {@code null} when no user principal is available --
     * verbatim the same posture as {@code SubflowDataSourceImpl#fetchAccessibleWorkspaceIds}: the caller is then left
     * unfiltered rather than breaking (e.g. the EE remote option-load path before principal propagation lands, or
     * runtime dispatch with no HTTP-request-scoped principal at all); it tightens automatically once a principal is
     * present.
     */
    private @Nullable Set<Long> fetchAccessibleWorkspaceIds() {
        return userService.fetchCurrentUser()
            .map(user -> workspaceFacade.getUserWorkspaces(user.getId())
                .stream()
                .map(Workspace::getId)
                .collect(Collectors.toSet()))
            .orElse(null);
    }

    private Optional<AiAgent> fetchAgentByUuid(String agentUuid) {
        return agentService.getAgents(null)
            .stream()
            .filter(agent -> agentUuid.equals(agent.getUuid()
                .toString()))
            .findFirst();
    }

    private boolean hasPublishedVersion(Project project) {
        return project.getProjectVersions()
            .stream()
            .anyMatch(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED);
    }

    private String getVersionWorkflowId(long projectId, int projectVersion) {
        List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion);

        if (workflowIds.size() != 1) {
            throw new IllegalStateException(
                "Expected exactly one workflow for project " + projectId + " version " + projectVersion + ", found "
                    + workflowIds.size());
        }

        return workflowIds.getFirst();
    }
}
