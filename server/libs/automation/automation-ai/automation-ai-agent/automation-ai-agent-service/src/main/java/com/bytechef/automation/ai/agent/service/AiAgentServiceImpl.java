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

package com.bytechef.automation.ai.agent.service;

import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository.ProjectWorkflowProject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAgentServiceImpl implements AiAgentService {

    private final AiAgentRepository agentRepository;

    public AiAgentServiceImpl(AiAgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public AiAgent create(AiAgent agent) {
        return agentRepository.save(agent);
    }

    @Override
    public void delete(long id) {
        agentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAgent> fetchAgent(long id) {
        return agentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAgent> fetchAgentByUuid(UUID uuid) {
        return agentRepository.findByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public AiAgent getAgent(long id) {
        return agentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiAgent with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgent> getAgents(@Nullable Long workspaceId) {
        if (workspaceId == null) {
            return agentRepository.findAll();
        }

        return agentRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getProjectId(AiAgent agent) {
        return agentRepository.findProjectIdByProjectWorkflowUuid(agent.getProjectWorkflowUuid())
            .orElseThrow(() -> new IllegalStateException(
                "AiAgent with id " + agent.getId() + " has no project workflow " + agent.getProjectWorkflowUuid()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getProjectIds(Collection<AiAgent> agents) {
        if (agents.isEmpty()) {
            return Map.of();
        }

        Set<UUID> projectWorkflowUuids = agents.stream()
            .map(AiAgent::getProjectWorkflowUuid)
            .collect(Collectors.toSet());

        List<ProjectWorkflowProject> projectWorkflowProjects = agentRepository.findProjectIdsByProjectWorkflowUuids(
            projectWorkflowUuids);

        Map<UUID, Long> projectIdsByUuid = new HashMap<>();

        for (ProjectWorkflowProject projectWorkflowProject : projectWorkflowProjects) {
            projectIdsByUuid.put(projectWorkflowProject.projectWorkflowUuid(), projectWorkflowProject.projectId());
        }

        Map<Long, Long> projectIds = new HashMap<>();

        for (AiAgent agent : agents) {
            Long projectId = projectIdsByUuid.get(agent.getProjectWorkflowUuid());

            if (projectId != null) {
                projectIds.put(agent.getId(), projectId);
            }
        }

        return projectIds;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgent> getProjectAgents(long projectId) {
        return agentRepository.findAllByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgent> getSubAgentReferencingAgents(long referencedAgentId) {
        return agentRepository.findSubAgentReferencingAgents(referencedAgentId);
    }

    @Override
    public AiAgent update(AiAgent agent) {
        AiAgent currentAgent = agentRepository.findById(agent.getId())
            .orElseThrow(() -> new IllegalArgumentException("AiAgent with id " + agent.getId() + " not found"));

        currentAgent.setName(agent.getName());
        currentAgent.setTitle(agent.getTitle());
        currentAgent.setDescription(agent.getDescription());
        currentAgent.setInstructions(agent.getInstructions());
        currentAgent.setWorkspaceId(agent.getWorkspaceId());
        currentAgent.setProjectWorkflowUuid(agent.getProjectWorkflowUuid());
        currentAgent.setSettings(agent.getSettings());
        currentAgent.setVersion(agent.getVersion());

        return agentRepository.save(currentAgent);
    }
}
