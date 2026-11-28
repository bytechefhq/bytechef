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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
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

        Set<Long> accessibleWorkspaceIds = fetchAccessibleWorkspaceIds();

        List<CallableAiAgentEntry> callableAgentEntries = new ArrayList<>();

        List<AiAgent> agents = agentService.getAgents(null);

        Map<Long, Long> projectIds = agentService.getProjectIds(agents);

        for (AiAgent agent : agents) {
            Long projectId = projectIds.get(agent.getId());

            if (projectId == null) {
                continue;
            }

            Project project = projectService.getProject(projectId);

            if (accessibleWorkspaceIds != null && !accessibleWorkspaceIds.contains(project.getWorkspaceId())) {
                continue;
            }

            if (!hasPublishedVersion(agent, project)) {
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

        Project project = projectService.getProject(agentService.getProjectId(agent));

        Set<Long> accessibleWorkspaceIds = fetchAccessibleWorkspaceIds();

        if (accessibleWorkspaceIds != null && !accessibleWorkspaceIds.contains(project.getWorkspaceId())) {
            throw new IllegalArgumentException(
                "Agent '" + agent.getTitle() + "' (" + agentUuid + ") is not accessible");
        }

        if (!editorEnvironment && !hasPublishedVersion(agent, project)) {
            throw new IllegalArgumentException(
                "Agent '" + agent.getTitle() + "' (" + agentUuid + ") has no published version");
        }

        ProjectWorkflow projectWorkflow =
            getVersionProjectWorkflow(agent, project.getId(), project.getLastProjectVersion());

        return new ResolvedAiAgent(projectWorkflow.getUuidAsString(), agent.getName(), agent.getDescription());
    }

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

    private boolean hasPublishedVersion(AiAgent agent, Project project) {
        String projectWorkflowUuid = String.valueOf(agent.getProjectWorkflowUuid());

        return project.getProjectVersions()
            .stream()
            .filter(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED)
            .anyMatch(projectVersion -> projectWorkflowService
                .fetchProjectWorkflow(project.getId(), projectVersion.getVersion(), projectWorkflowUuid)
                .isPresent());
    }

    private ProjectWorkflow getVersionProjectWorkflow(AiAgent agent, long projectId, int projectVersion) {
        return projectWorkflowService
            .fetchProjectWorkflow(projectId, projectVersion, String.valueOf(agent.getProjectWorkflowUuid()))
            .orElseThrow(() -> new IllegalStateException(
                "Agent " + agent.getId() + " has no workflow in project " + projectId + " version "
                    + projectVersion));
    }
}
