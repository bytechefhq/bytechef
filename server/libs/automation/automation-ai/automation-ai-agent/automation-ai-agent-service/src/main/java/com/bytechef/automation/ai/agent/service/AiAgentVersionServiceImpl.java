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

import com.bytechef.automation.ai.agent.domain.AiAgentVersion;
import com.bytechef.automation.ai.agent.repository.AiAgentVersionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAgentVersionServiceImpl implements AiAgentVersionService {

    private final AiAgentVersionRepository agentVersionRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiAgentVersionServiceImpl(AiAgentVersionRepository agentVersionRepository) {
        this.agentVersionRepository = agentVersionRepository;
    }

    @Override
    public void deleteProjectAgentVersions(long projectId) {
        agentVersionRepository.deleteByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiAgentVersion> fetchAgentVersion(UUID projectWorkflowUuid, int projectVersion) {
        return agentVersionRepository.findByProjectWorkflowUuidAndProjectVersion(projectWorkflowUuid, projectVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiAgentVersion> getProjectAgentVersions(long projectId, int projectVersion) {
        return agentVersionRepository.findAllByProjectIdAndProjectVersion(projectId, projectVersion);
    }

    @Override
    public AiAgentVersion save(long projectId, int projectVersion, UUID projectWorkflowUuid, String definition) {
        Optional<AiAgentVersion> existingAgentVersion =
            agentVersionRepository.findByProjectWorkflowUuidAndProjectVersion(projectWorkflowUuid, projectVersion);

        AiAgentVersion agentVersion = existingAgentVersion.orElseGet(
            () -> new AiAgentVersion(projectId, projectVersion, projectWorkflowUuid, definition));

        agentVersion.setDefinition(definition);

        return agentVersionRepository.save(agentVersion);
    }
}
