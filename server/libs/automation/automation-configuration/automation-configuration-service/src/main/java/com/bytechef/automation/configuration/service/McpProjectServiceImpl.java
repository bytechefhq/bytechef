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

import com.bytechef.automation.configuration.domain.McpProject;
import com.bytechef.automation.configuration.repository.McpProjectRepository;
import com.bytechef.commons.util.OptionalUtils;
import java.util.List;
import java.util.Optional;
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

    public McpProjectServiceImpl(McpProjectRepository mcpProjectRepository) {
        this.mcpProjectRepository = mcpProjectRepository;
    }

    @Override
    public McpProject create(McpProject mcpProject) {
        return mcpProjectRepository.save(mcpProject);
    }

    @Override
    public McpProject update(McpProject mcpProject) {
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
    public Optional<McpProject> fetchMcpProject(long mcpProjectId) {
        return mcpProjectRepository.findById(mcpProjectId);
    }

    @Override
    public List<McpProject> getMcpProjects() {
        return mcpProjectRepository.findAll();
    }

    @Override
    public List<McpProject> getMcpServerMcpProjects(long mcpServerId) {
        return mcpProjectRepository.findAllByMcpServerId(mcpServerId);
    }
}
