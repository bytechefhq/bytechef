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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AiAgentService {

    AiAgent create(AiAgent agent);

    void delete(long id);

    Optional<AiAgent> fetchAgent(long id);

    Optional<AiAgent> fetchAgentByUuid(UUID uuid);

    AiAgent getAgent(long id);

    List<AiAgent> getAgents(@Nullable Long workspaceId);

    long getProjectId(AiAgent agent);

    Map<Long, Long> getProjectIds(Collection<AiAgent> agents);

    List<AiAgent> getProjectAgents(long projectId);

    List<AiAgent> getSubAgentReferencingAgents(long referencedAgentId);

    AiAgent update(AiAgent agent);
}
