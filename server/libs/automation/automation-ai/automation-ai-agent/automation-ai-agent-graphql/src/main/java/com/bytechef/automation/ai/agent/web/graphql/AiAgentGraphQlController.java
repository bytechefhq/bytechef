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

import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentVersionDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for CRUD management of {@code AiAgent} entities and their
 * {@link AiAgentChannel}/{@link AiAgentElement} building blocks. Every mutation delegates 1:1 to {@link AiAgentFacade},
 * which owns authorization and the draft-workflow-regeneration side effects — this controller only maps between
 * GraphQL's ID-as-string wire shape and the facade's {@code long}/{@code Long} parameters, and flattens
 * {@link AiAgentDTO} into the {@code AiAgent} GraphQL type via {@link AiAgentPayload}.
 *
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
    public List<AiAgentPayload> aiAgents(@Argument long workspaceId) {
        return agentFacade.getAgents(workspaceId)
            .stream()
            .map(AiAgentPayload::new)
            .toList();
    }

    /**
     * Backs the AiAgent Deployments page. Every projectId-scoped {@code ProjectDeploymentService} listing path
     * (including {@code getProjectDeployments(long)}) excludes {@code __AI_AGENT__} projects, so this query does not
     * reuse {@code workspaceProjectDeployments} — it delegates to {@link AiAgentFacade#getAgentDeployments(Long)},
     * which assembles the list from the unfiltered per-environment lookup instead. See that method's javadoc.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<AiAgentDeploymentDTO> aiAgentDeployments(@Argument long workspaceId) {
        return agentFacade.getAgentDeployments(workspaceId);
    }

    /**
     * Backs the client's "Agent Chats" picker. Sibling of {@code workspaceChatWorkflows} (in
     * {@code automation-configuration-graphql}), which structurally cannot return agent chat workflows — agents live in
     * hidden {@code __AI_AGENT__} projects that every projectId-scoped {@code ProjectDeploymentService} listing path
     * filters out. Rows carry a {@code workflowExecutionId} built exactly the way that query builds it, so the client
     * opens both kinds of chat through one code path.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<ChatAgentDTO> workspaceChatAgents(@Argument long workspaceId, @Argument long environmentId) {
        return agentFacade.getWorkspaceChatAgents(workspaceId, environmentId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentPayload createAiAgent(@Argument CreateAiAgentInput input) {
        return new AiAgentPayload(agentFacade.createAgent(input.title(), input.description(), input.workspaceId()));
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
    public List<Tag> aiAgentDeploymentTags(@Argument long workspaceId) {
        return agentFacade.getAgentDeploymentTags(workspaceId);
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

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<Tag> aiAgentTags(@Argument long workspaceId) {
        return agentFacade.getAgentTags(workspaceId);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateAiAgentDeploymentTags(@Argument UpdateAiAgentDeploymentTagsInput input) {
        agentFacade.updateAgentDeploymentTags(input.id(), toTags(input.tags()));

        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean updateAiAgentTags(@Argument UpdateAiAgentTagsInput input) {
        agentFacade.updateAgentTags(input.id(), toTags(input.tags()));

        return true;
    }

    /**
     * A tag the client typed but never saved arrives with a null id; it is left unset here so the facade's own save
     * mints the row.
     */
    private static List<Tag> toTags(@Nullable List<TagInput> tagInputs) {
        if (tagInputs == null) {
            return List.of();
        }

        return tagInputs.stream()
            .map(tagInput -> {
                Tag tag = new Tag();

                if (tagInput.id() != null) {
                    tag.setId(tagInput.id());
                }

                tag.setName(tagInput.name());

                return tag;
            })
            .toList();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public int publishAiAgent(@Argument long id, @Argument @Nullable String description) {
        return agentFacade.publishAgent(id, description);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiAgentPayload importAiAgent(@Argument long workspaceId, @Argument String json) {
        return new AiAgentPayload(agentFacade.importAgent(workspaceId, json));
    }

    /**
     * Resolves the {@code AiAgent} type's {@code draftWorkflowId} field. Not part of {@link AiAgentDTO} (which
     * describes publish state, not the draft's own identity), so it requires a dedicated facade call rather than a
     * plain property read off {@link AiAgentPayload}.
     */
    @SchemaMapping(typeName = "AiAgent", field = "draftWorkflowId")
    public String draftWorkflowId(AiAgentPayload agentPayload) {
        return agentFacade.getDraftWorkflowId(agentPayload.id());
    }

    /**
     * Flattens an {@link AiAgentDTO} into the shape the {@code AiAgent} GraphQL type's fields are read from via
     * reflection: the nested {@code AiAgent} row's own columns alongside the DTO's derived publish-state fields and its
     * position-ordered {@link AiAgentChannel}/{@link AiAgentElement} lists. {@code draftWorkflowId} is deliberately
     * absent — see {@link #draftWorkflowId(AiAgentPayload)}.
     */
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
            return agentDTO.agent()
                .getProjectId();
        }

        public UUID uuid() {
            return agentDTO.agent()
                .getUuid();
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

        public List<Tag> tags() {
            return agentDTO.tags();
        }

        public @Nullable Instant lastModifiedDate() {
            return agentDTO.agent()
                .getLastModifiedDate();
        }
    }

    public record CreateAiAgentInput(String title, @Nullable String description, long workspaceId) {
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

    @SuppressFBWarnings("EI_EXPOSE_REP")
    /**
     * @param id the {@code ProjectDeployment} id — an agent deployment IS one
     */
    public record UpdateAiAgentDeploymentTagsInput(long id, @Nullable List<TagInput> tags) {
    }

    public record UpdateAiAgentTagsInput(long id, @Nullable List<TagInput> tags) {
    }

    public record TagInput(@Nullable Long id, String name) {
    }
}
