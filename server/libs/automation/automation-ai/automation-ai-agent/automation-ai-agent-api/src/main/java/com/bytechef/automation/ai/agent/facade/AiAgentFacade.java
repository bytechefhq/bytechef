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

package com.bytechef.automation.ai.agent.facade;

import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentVersionDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AiAgentFacade {

    AiAgentDTO createAgent(String title, String description, long workspaceId, @Nullable Long projectId);

    void deleteAgent(long id);

    void deleteProjectAgents(long projectId);

    AiAgentDTO getAgent(long id);

    List<AiAgentDTO> getAgents(long workspaceId);

    List<AiAgentDeploymentDTO> getAgentDeployments(long workspaceId);

    List<ResolvedAgentChannel> getAgentChannelDefinitions();

    List<ChatAgentDTO> getWorkspaceChatAgents(long workspaceId, long environmentId);

    AiAgentDTO updateAgent(long id, String title, String description, String instructions);

    void updateAgentSettings(long id, Map<String, Object> settings);

    AiAgentChannel addAgentChannel(long agentId, String channelType, Map<String, Object> parameters, Long connectionId);

    void updateAgentChannel(long channelId, Map<String, Object> parameters, Long connectionId);

    void deleteAgentChannel(long channelId);

    AiAgentElement addAgentElement(
        long agentId, String kind, Long referenceId, Map<String, Object> parameters, Long connectionId);

    void updateAgentElement(long elementId, Map<String, Object> parameters, Long connectionId);

    void deleteAgentElement(long elementId);

    boolean isProjectPublishable(long projectId);

    void prepareProjectPublish(long projectId);

    List<AiAgentVersionDTO> getAgentVersions(long id);

    String exportAgent(long id);

    List<String> exportProjectAgentVersions(long projectId, int projectVersion);

    AiAgentDTO importAgent(long workspaceId, String json, @Nullable Long projectId);

    List<AiAgentDTO> copyProjectAgents(long sourceProjectId, long targetProjectId, long workspaceId);

    AiAgentDTO updateAgentFromExport(long id, String json);

    void restoreProjectAgent(long projectId, int projectVersion, UUID projectWorkflowUuid);

    void restoreProjectAgents(long projectId, int projectVersion);

    String getDraftWorkflowId(long agentId);
}
