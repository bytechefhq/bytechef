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

package com.bytechef.automation.ai.agent.web.graphql;

import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentVersionDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
public class AiAgentGraphQlController {

    private final AiAgentFacade agentFacade;

    @SuppressFBWarnings("EI")
    public AiAgentGraphQlController(AiAgentFacade agentFacade) {
        this.agentFacade = agentFacade;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentPayload aiAgent(@Argument long id) {
        return new AiAgentPayload(agentFacade.getAgent(id));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiAgentChannelDefinitionPayload> aiAgentChannelDefinitions() {
        return agentFacade.getAgentChannelDefinitions()
            .stream()
            .map(AiAgentChannelDefinitionPayload::new)
            .toList();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiAgentPayload> aiAgents(@Argument long workspaceId) {
        return agentFacade.getAgents(workspaceId)
            .stream()
            .map(AiAgentPayload::new)
            .toList();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiAgentDeploymentDTO> aiAgentDeployments(@Argument long workspaceId) {
        return agentFacade.getAgentDeployments(workspaceId);
    }

    @QueryMapping
    public List<ChatAgentDTO> workspaceChatAgents(@Argument long workspaceId, @Argument long environmentId) {
        return agentFacade.getWorkspaceChatAgents(workspaceId, environmentId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentPayload createAiAgent(@Argument CreateAiAgentInput input) {
        return new AiAgentPayload(agentFacade.createAgent(
            input.title(), input.description(), input.workspaceId(), input.projectId()));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentPayload updateAiAgent(@Argument UpdateAiAgentInput input) {
        return new AiAgentPayload(
            agentFacade.updateAgent(input.id(), input.title(), input.description(), input.instructions()));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateAiAgentSettings(@Argument long id, @Argument Map<String, Object> settings) {
        agentFacade.updateAgentSettings(id, settings);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiAgent(@Argument long id) {
        agentFacade.deleteAgent(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentChannel addAiAgentChannel(@Argument AddAiAgentChannelInput input) {
        return agentFacade.addAgentChannel(
            input.agentId(), input.channelType(), input.parameters(), input.connectionId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateAiAgentChannel(@Argument UpdateAiAgentChannelInput input) {
        agentFacade.updateAgentChannel(input.id(), input.parameters(), input.connectionId());

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiAgentChannel(@Argument long id) {
        agentFacade.deleteAgentChannel(id);

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentElement addAiAgentElement(@Argument AddAiAgentElementInput input) {
        return agentFacade.addAgentElement(
            input.agentId(), input.kind(), input.referenceId(), input.parameters(), input.connectionId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateAiAgentElement(@Argument UpdateAiAgentElementInput input) {
        agentFacade.updateAgentElement(input.id(), input.parameters(), input.connectionId());

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteAiAgentElement(@Argument long id) {
        agentFacade.deleteAgentElement(id);

        return true;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiAgentVersionDTO> aiAgentVersions(@Argument long id) {
        return agentFacade.getAgentVersions(id);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public String exportAiAgent(@Argument long id) {
        return agentFacade.exportAgent(id);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentPayload importAiAgent(
        @Argument long workspaceId, @Argument String json, @Argument @Nullable Long projectId) {

        return new AiAgentPayload(agentFacade.importAgent(workspaceId, json, projectId));
    }

    @SchemaMapping(typeName = "AiAgent", field = "draftWorkflowId")
    public String draftWorkflowId(AiAgentPayload agentPayload) {
        return agentFacade.getDraftWorkflowId(agentPayload.id());
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public record AiAgentPayload(AiAgentDTO agentDTO) {

        public Long id() {
            return agentDTO.agent()
                .getId();
        }

        public String name() {
            return agentDTO.agent()
                .getName();
        }

        public String title() {
            return agentDTO.agent()
                .getTitle();
        }

        public @Nullable String description() {
            return agentDTO.agent()
                .getDescription();
        }

        public @Nullable String instructions() {
            return agentDTO.agent()
                .getInstructions();
        }

        public @Nullable Long workspaceId() {
            return agentDTO.agent()
                .getWorkspaceId();
        }

        public long projectId() {
            return agentDTO.projectId();
        }

        public UUID uuid() {
            return agentDTO.agent()
                .getUuid();
        }

        public UUID projectWorkflowUuid() {
            return agentDTO.agent()
                .getProjectWorkflowUuid();
        }

        public boolean unpublishedChanges() {
            return agentDTO.unpublishedChanges();
        }

        public int lastPublishedVersion() {
            return agentDTO.lastPublishedVersion();
        }

        public @Nullable Instant publishedDate() {
            return agentDTO.publishedDate();
        }

        public List<AiAgentChannel> channels() {
            return agentDTO.channels();
        }

        public List<AiAgentElement> elements() {
            return agentDTO.elements();
        }

        public @Nullable Map<String, ?> settings() {
            return agentDTO.settings();
        }

        public @Nullable Instant lastModifiedDate() {
            return agentDTO.agent()
                .getLastModifiedDate();
        }
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public record AiAgentChannelDefinitionPayload(ResolvedAgentChannel resolvedAgentChannel) {

        public String channelType() {
            return resolvedAgentChannel.name();
        }

        public String componentName() {
            return resolvedAgentChannel.componentName();
        }

        public int componentVersion() {
            WorkflowNodeType triggerNodeType = WorkflowNodeType.ofType(resolvedAgentChannel.triggerType());

            return triggerNodeType.version();
        }

        public String triggerName() {
            WorkflowNodeType triggerNodeType = WorkflowNodeType.ofType(resolvedAgentChannel.triggerType());

            return Objects.requireNonNull(triggerNodeType.operation());
        }

        public @Nullable String replyActionName() {
            String replyActionType = resolvedAgentChannel.replyActionType();

            if (replyActionType == null) {
                return null;
            }

            WorkflowNodeType replyActionNodeType = WorkflowNodeType.ofType(replyActionType);

            return replyActionNodeType.operation();
        }

        public String title() {
            return resolvedAgentChannel.title();
        }

        public @Nullable String description() {
            return resolvedAgentChannel.description();
        }

        public @Nullable String icon() {
            return resolvedAgentChannel.icon();
        }

        public boolean connectionRequired() {
            return resolvedAgentChannel.connectionRequired();
        }

        public boolean propertiesConfigurable() {
            return resolvedAgentChannel.propertiesConfigurable();
        }

        public boolean approvalCapable() {
            return resolvedAgentChannel.approvalDelivery() != null;
        }

        public boolean pinned() {
            String channelType = channelType();

            return AiAgentChannelType.CHAT.equals(channelType)
                || AiAgentChannelType.WORKFLOW_CALL.equals(channelType);
        }

        public boolean schedule() {
            return AiAgentChannelType.SCHEDULE.equals(channelType());
        }
    }

    public record CreateAiAgentInput(
        String title, @Nullable String description, long workspaceId, @Nullable Long projectId) {

    }

    public record UpdateAiAgentInput(
        long id, @Nullable String title, @Nullable String description, @Nullable String instructions) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record AddAiAgentChannelInput(
        long agentId, String channelType, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record UpdateAiAgentChannelInput(
        long id, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record AddAiAgentElementInput(
        long agentId, String kind, @Nullable Long referenceId, @Nullable Map<String, Object> parameters,
        @Nullable Long connectionId) {
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public record UpdateAiAgentElementInput(
        long id, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
    }
}
