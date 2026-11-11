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

package com.bytechef.automation.ai.agent.event;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.listener.ProjectContentContributor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.commons.util.JsonUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiAgentProjectContentContributor implements ProjectContentContributor {

    static final String CONTENT_DIRECTORY = "agents/";

    private static final String FILE_EXTENSION = ".json";

    private final AiAgentFacade aiAgentFacade;
    private final AiAgentService aiAgentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAgentProjectContentContributor(
        @Lazy AiAgentFacade aiAgentFacade, @Lazy AiAgentService aiAgentService, @Lazy ProjectService projectService) {

        this.aiAgentFacade = aiAgentFacade;
        this.aiAgentService = aiAgentService;
        this.projectService = projectService;
    }

    @Override
    public String getContentDirectory() {
        return CONTENT_DIRECTORY;
    }

    @Override
    public void onProjectDuplicated(long sourceProjectId, long duplicateProjectId) {
        if (aiAgentService.getProjectAgents(sourceProjectId)
            .isEmpty()) {

            return;
        }

        aiAgentFacade.copyProjectAgents(sourceProjectId, duplicateProjectId, getWorkspaceId(duplicateProjectId));
    }

    @Override
    public Map<String, byte[]> exportProjectContent(long projectId, int projectVersion) {
        Map<String, byte[]> files = new LinkedHashMap<>();

        Project project = projectService.getProject(projectId);

        if (projectVersion != project.getLastProjectVersion()) {
            for (String json : aiAgentFacade.exportProjectAgentVersions(projectId, projectVersion)) {
                Object name = JsonUtils.readMap(json)
                    .get("name");

                files.put(CONTENT_DIRECTORY + name + FILE_EXTENSION, json.getBytes(StandardCharsets.UTF_8));
            }

            return files;
        }

        for (AiAgent agent : aiAgentService.getProjectAgents(projectId)) {
            String json = aiAgentFacade.exportAgent(agent.getId());

            files.put(CONTENT_DIRECTORY + agent.getName() + FILE_EXTENSION, json.getBytes(StandardCharsets.UTF_8));
        }

        return files;
    }

    @Override
    public void importProjectContent(long projectId, long workspaceId, Map<String, byte[]> files) {
        for (String json : getAgentFiles(files).values()) {
            aiAgentFacade.importAgent(workspaceId, json, projectId);
        }
    }

    @Override
    public boolean isProjectContentPublishable(long projectId) {
        return aiAgentFacade.isProjectPublishable(projectId);
    }

    @Override
    public void pullProjectContent(long projectId, Map<String, byte[]> files) {
        Map<String, String> agentFiles = getAgentFiles(files);

        if (agentFiles.isEmpty()) {
            return;
        }

        List<AiAgent> unmatchedAgents = new ArrayList<>(aiAgentService.getProjectAgents(projectId));

        for (Map.Entry<String, String> entry : agentFiles.entrySet()) {
            String json = entry.getValue();

            Optional<AiAgent> matchedAgent = findAgent(unmatchedAgents, entry.getKey(), json);

            if (matchedAgent.isPresent()) {
                AiAgent agent = matchedAgent.get();

                unmatchedAgents.remove(agent);

                aiAgentFacade.updateAgentFromExport(agent.getId(), json);
            } else {
                aiAgentFacade.importAgent(getWorkspaceId(projectId), json, projectId);
            }
        }
    }

    @Override
    public void restoreProjectContent(long projectId, int projectVersion) {
        aiAgentFacade.restoreProjectAgents(projectId, projectVersion);
    }

    @Override
    public void restoreProjectWorkflowContent(long projectId, int projectVersion, String workflowUuid) {
        aiAgentFacade.restoreProjectAgent(projectId, projectVersion, UUID.fromString(workflowUuid));
    }

    private static Optional<AiAgent> findAgent(List<AiAgent> agents, String agentName, String json) {
        Optional<AiAgent> namedAgent = agents.stream()
            .filter(agent -> Objects.equals(agent.getName(), agentName))
            .findFirst();

        if (namedAgent.isPresent()) {
            return namedAgent;
        }

        Object title = JsonUtils.readMap(json)
            .get("title");

        List<AiAgent> titledAgents = agents.stream()
            .filter(agent -> Objects.equals(agent.getTitle(), title))
            .toList();

        return titledAgents.size() == 1 ? Optional.of(titledAgents.getFirst()) : Optional.empty();
    }

    private static Map<String, String> getAgentFiles(Map<String, byte[]> files) {
        Map<String, String> agentFiles = new TreeMap<>();

        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            String path = entry.getKey();

            if (!path.startsWith(CONTENT_DIRECTORY) || !path.endsWith(FILE_EXTENSION)) {
                continue;
            }

            String agentName = path.substring(CONTENT_DIRECTORY.length(), path.length() - FILE_EXTENSION.length());

            if (agentName.isEmpty() || agentName.contains("/")) {
                continue;
            }

            agentFiles.put(agentName, new String(entry.getValue(), StandardCharsets.UTF_8));
        }

        return agentFiles;
    }

    private long getWorkspaceId(long projectId) {
        Project project = projectService.getProject(projectId);

        return Objects.requireNonNull(project.getWorkspaceId(), "workspaceId");
    }
}
