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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ChannelDefinition;
import com.bytechef.automation.ai.agent.channel.ChannelDefinitions;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO.AiAgentDeploymentTriggerDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO.AiAgentDeploymentWorkflowDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentVersionDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import com.bytechef.automation.ai.agent.exception.AiAgentErrorType;
import com.bytechef.automation.ai.agent.service.AiAgentChannelService;
import com.bytechef.automation.ai.agent.service.AiAgentElementService;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.ai.agent.util.AiAgentSettings;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator.ConnectionRefOwnerKind;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator.SubAgentRef;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.HostedChatTriggers;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AiAgentFacade}.
 *
 * <p>
 * Provisioning (create) and every draft-affecting mutation (channel/element add-update-delete, instructions/title
 * edits) go through the raw {@link ProjectService}/{@link ProjectWorkflowService}/{@link WorkflowService} rather than
 * {@code ProjectWorkflowFacade}, mirroring {@code WorkspaceKnowledgeBaseSourceFacadeImpl}'s call sequence — the raw
 * services operate on the agent's hidden {@code __AI_AGENT__} project directly, while {@code ProjectWorkflowFacade}'s
 * own permission gates are shaped around user-visible projects and would reject a hidden one.
 * </p>
 *
 * <p>
 * <b>Authorization.</b> Every method carries {@code @PreAuthorize("isAuthenticated()")} — the minimum the Task 10 brief
 * requires. A tighter workspace-membership check (the pattern recent AI-gateway workspace facades use,
 * {@code hasPermission(#workspaceId, 'Workspace', '...')}) was deliberately not added: that machinery is wired through
 * {@code AutomationPermissionEvaluator}/{@code PermissionService} and a matching set of registered permission names,
 * neither of which exists for the {@code AiAgent} entity yet. Adding a half-wired permission name here would be worse
 * than an honest {@code isAuthenticated()} — a future task should introduce real {@code AGENT_*} permissions end-to-end
 * (service + evaluator + UI) rather than this facade guessing at names nothing else recognizes.
 * </p>
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAgentFacadeImpl implements AiAgentFacade {

    private static final Logger log = LoggerFactory.getLogger(AiAgentFacadeImpl.class);

    private static final int MAX_NAME_LENGTH = 64;

    private static final String MANUAL_TRIGGER_NAME = "manual";

    // Export document keys. exportVersion is written but never read: a reader that has to guess a document's shape
    // has already lost, and the first breaking change to this format needs somewhere to branch on.
    private static final String EXPORT_VERSION = "exportVersion";
    private static final int EXPORT_VERSION_VALUE = 1;
    private static final String EXPORT_TITLE = "title";
    private static final String EXPORT_DESCRIPTION = "description";
    private static final String EXPORT_INSTRUCTIONS = "instructions";
    private static final String EXPORT_SETTINGS = "settings";
    private static final String EXPORT_CHANNELS = "channels";
    private static final String EXPORT_ELEMENTS = "elements";
    private static final String EXPORT_CHANNEL_TYPE = "channelType";
    private static final String EXPORT_KIND = "kind";
    private static final String EXPORT_POSITION = "position";
    private static final String EXPORT_PARAMETERS = "parameters";

    private static final Set<String> SKIPPED_IMPORT_ELEMENT_KINDS = Set.of(
        AiAgentElement.KIND_SKILL, AiAgentElement.KIND_SUB_AGENT, AiAgentElement.KIND_KNOWLEDGE_BASE,
        AiAgentElement.KIND_CHAT_MEMORY);

    private final AiAgentService agentService;
    private final AiAgentChannelService agentChannelService;
    private final AiAgentElementService agentElementService;
    private final EnvironmentService environmentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final TagService tagService;
    private final PrincipalJobService principalJobService;
    private final JobService jobService;
    private final String webhookUrl;

    @SuppressFBWarnings("EI2")
    public AiAgentFacadeImpl(
        AiAgentService agentService, AiAgentChannelService agentChannelService,
        AiAgentElementService agentElementService, EnvironmentService environmentService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, TagService tagService,
        PrincipalJobService principalJobService, JobService jobService,
        @Value("${bytechef.webhook-url}") String webhookUrl) {

        this.agentService = agentService;
        this.agentChannelService = agentChannelService;
        this.agentElementService = agentElementService;
        this.environmentService = environmentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.tagService = tagService;
        this.principalJobService = principalJobService;
        this.jobService = jobService;
        this.webhookUrl = webhookUrl;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public AiAgentDTO createAgent(String title, String description, Long workspaceId) {
        Objects.requireNonNull(title, "title");

        String name = uniqueName(slugify(title), workspaceId);
        UUID uuid = UUID.randomUUID();

        Project project = new Project();

        project.setName(SystemProjects.AI_AGENT_NAME_PREFIX + uuid);
        project.setDescription("System project backing the '" + title + "' Agent. Do not edit.");
        project.setWorkspaceId(workspaceId);

        Project savedProject = projectService.create(project);

        AiAgent agent = new AiAgent();

        agent.setName(name);
        agent.setTitle(title);
        agent.setDescription(description);
        agent.setWorkspaceId(workspaceId);
        agent.setProjectId(savedProject.getId());
        agent.setUuid(uuid);

        AiAgent savedAgent = agentService.create(agent);

        AiAgentChannel chatChannel = new AiAgentChannel(savedAgent.getId(), AiAgentChannelType.CHAT);

        chatChannel.setPosition(0);
        agentChannelService.create(chatChannel);

        AiAgentChannel workflowCallChannel = new AiAgentChannel(savedAgent.getId(), AiAgentChannelType.WORKFLOW_CALL);

        workflowCallChannel.setPosition(1);
        agentChannelService.create(workflowCallChannel);

        AiAgentElement chatMemoryElement = new AiAgentElement(savedAgent.getId(), AiAgentElement.KIND_CHAT_MEMORY);

        chatMemoryElement.setPosition(0);
        agentElementService.create(chatMemoryElement);

        String definition = generateDefinition(savedAgent);

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(
            savedProject.getId(), savedProject.getLastProjectVersion(), workflow.getId());

        return toAgentDTO(savedAgent);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void deleteAgent(long id) {
        AiAgent agent = agentService.getAgent(id);

        if (hasAnyDeployment(agent.getProjectId())) {
            throw new ConfigurationException(
                "Agent " + id + " cannot be deleted while its project has deployments",
                AiAgentErrorType.AGENT_HAS_DEPLOYMENTS);
        }

        if (!agentService.getSubAgentReferencingAgents(id)
            .isEmpty()) {

            throw new ConfigurationException(
                "Agent " + id + " cannot be deleted while it is referenced as a sub-agent",
                AiAgentErrorType.AGENT_REFERENCED_AS_SUB_AGENT);
        }

        long projectId = agent.getProjectId();

        agentService.delete(id);

        Project project = projectService.getProject(projectId);

        // Every project version's workflow rows, not just the last one: publishAgent (Task 11) duplicates the
        // workflow into a fresh row on each published version, so a published agent has one workflow row per
        // version — deleting only getLastProjectVersion()'s would leak the older versions' rows.
        for (ProjectVersion projectVersion : project.getProjectVersions()) {
            int version = projectVersion.getVersion();

            for (String workflowId : projectWorkflowService.getProjectWorkflowIds(projectId, version)) {
                projectWorkflowService.delete(projectId, version, workflowId);
                workflowService.delete(workflowId);
            }
        }

        // ProjectServiceImpl.delete carries @PreAuthorize("hasPermission(#id, 'Project', 'PROJECT_DELETE')").
        // Verified this does not block the common case: ProjectOwnershipResolver ("Project" token) resolves via
        // project.workspace_id and returns no ownerUserId, so PermissionServiceImpl (CE) treats any workspace-owned
        // project as shared — any authenticated caller passes. This only fails closed when the project has no
        // workspace_id at all, i.e. an agent created with workspaceId = null (AiAgent.workspaceId is nullable);
        // deleteAgent would then throw AccessDeniedException here. Left as a known gap rather than dropping to an
        // ungated repository-level delete, since the documented/expected usage is workspace-scoped agents and this
        // call already carries the same audit-publishing ProjectFacadeImpl's own delete path relies on.
        projectService.delete(projectId);
    }

    /**
     * {@code ProjectDeploymentService.getProjectDeployments(long)} (the listing-surface read) deliberately excludes
     * every {@code SystemProjects} prefix — including {@code __AI_AGENT__} — so it always reports zero deployments for
     * an agent's hidden project, which would make this guard a no-op. {@code fetchProjectDeployment(projectId,
     * environment)} runs a plain, unfiltered lookup instead, so it is checked once per {@link Environment}.
     */
    private boolean hasAnyDeployment(long projectId) {
        for (Environment environment : Environment.values()) {
            if (projectDeploymentService.fetchProjectDeployment(projectId, environment)
                .isPresent()) {

                return true;
            }
        }

        return false;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public AiAgentDTO getAgent(long id) {
        return toAgentDTO(agentService.getAgent(id));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<AiAgentDTO> getAgents(Long workspaceId) {
        return agentService.getAgents(workspaceId)
            .stream()
            .map(this::toAgentDTO)
            .toList();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Tag> getAgentTags(Long workspaceId) {
        List<AiAgent> agents = agentService.getAgents(workspaceId);

        List<Long> tagIds = agents.stream()
            .map(AiAgent::getTagIds)
            .flatMap(List::stream)
            .distinct()
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Tag> getAgentDeploymentTags(Long workspaceId) {
        List<Long> tagIds = getAgentDeployments(workspaceId)
            .stream()
            .map(AiAgentDeploymentDTO::tags)
            .flatMap(List::stream)
            .map(Tag::getId)
            .distinct()
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<AiAgentVersionDTO> getAgentVersions(long id) {
        AiAgent agent = agentService.getAgent(id);
        Project project = projectService.getProject(agent.getProjectId());

        // Newest first, so the sheet opens on the most recent versions — the draft leads, exactly as the project
        // version history sheet expects its own list to arrive.
        return project.getProjectVersions()
            .stream()
            .sorted(Comparator.comparingInt(ProjectVersion::getVersion)
                .reversed())
            .map(
                projectVersion -> new AiAgentVersionDTO(
                    projectVersion.getVersion(), projectVersion.getDescription(), projectVersion.getPublishedDate(),
                    String.valueOf(projectVersion.getStatus())))
            .toList();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public String exportAgent(long id) {
        AiAgent agent = agentService.getAgent(id);

        List<Map<String, Object>> channels = agentChannelService.getByAgentId(id)
            .stream()
            .map(AiAgentFacadeImpl::toExportedChannel)
            .toList();

        List<Map<String, Object>> elements = agentElementService.getByAgentId(id)
            .stream()
            .map(AiAgentFacadeImpl::toExportedElement)
            .toList();

        Map<String, Object> exportedAgent = new LinkedHashMap<>();

        exportedAgent.put(EXPORT_VERSION, EXPORT_VERSION_VALUE);
        exportedAgent.put(EXPORT_TITLE, agent.getTitle());
        exportedAgent.put(EXPORT_DESCRIPTION, agent.getDescription());
        exportedAgent.put(EXPORT_INSTRUCTIONS, agent.getInstructions());
        exportedAgent.put(EXPORT_SETTINGS, agent.getSettings());
        exportedAgent.put(EXPORT_CHANNELS, channels);
        exportedAgent.put(EXPORT_ELEMENTS, elements);

        return JsonUtils.writeWithDefaultPrettyPrinter(exportedAgent);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public AiAgentDTO importAgent(long workspaceId, String json) {
        Map<String, ?> exportedAgent;

        try {
            exportedAgent = JsonUtils.readMap(json);
        } catch (RuntimeException runtimeException) {
            throw new ConfigurationException(
                "Agent import is not valid JSON", AiAgentErrorType.INVALID_AGENT_IMPORT);
        }

        String title = exportedString(exportedAgent, EXPORT_TITLE);

        if (title == null || title.isBlank()) {
            throw new ConfigurationException("Agent import carries no title", AiAgentErrorType.INVALID_AGENT_IMPORT);
        }

        // Through createAgent rather than assembling rows here, so an imported agent gets the same backing project,
        // permanent channels and draft workflow every other agent has.
        AiAgentDTO agentDTO = createAgent(title, exportedString(exportedAgent, EXPORT_DESCRIPTION), workspaceId);

        AiAgent agent = agentDTO.agent();

        agent.setInstructions(exportedString(exportedAgent, EXPORT_INSTRUCTIONS));
        agent.setSettings(exportedMap(exportedAgent.get(EXPORT_SETTINGS)));

        agentService.update(agent);

        long agentId = agent.getId();

        for (Map<String, ?> channel : exportedList(exportedAgent.get(EXPORT_CHANNELS))) {
            String channelType = exportedString(channel, EXPORT_CHANNEL_TYPE);

            // The permanent chat and workflowCall channels already exist on the fresh agent, and a type the registry
            // does not know comes from a newer ByteChef than this one — skipped rather than failing the whole import.
            if (channelType == null || isPermanentChannelType(channelType) || !isKnownChannelType(channelType)) {
                continue;
            }

            addAgentChannel(agentId, channelType, exportedMap(channel.get(EXPORT_PARAMETERS)), null);
        }

        for (Map<String, ?> element : exportedList(exportedAgent.get(EXPORT_ELEMENTS))) {
            String kind = exportedString(element, EXPORT_KIND);

            // Reference-carrying kinds mean nothing in the target workspace, and CHAT_MEMORY is a singleton
            // createAgent already added — see importAgent's contract on AiAgentFacade.
            if (kind == null || SKIPPED_IMPORT_ELEMENT_KINDS.contains(kind)) {
                continue;
            }

            addAgentElement(agentId, kind, null, exportedMap(element.get(EXPORT_PARAMETERS)), null);
        }

        return toAgentDTO(agentService.getAgent(agentId));
    }

    private static Map<String, Object> toExportedChannel(AiAgentChannel channel) {
        Map<String, Object> exportedChannel = new LinkedHashMap<>();

        exportedChannel.put(EXPORT_CHANNEL_TYPE, channel.getChannelType());
        exportedChannel.put(EXPORT_POSITION, channel.getPosition());
        exportedChannel.put(EXPORT_PARAMETERS, channel.getParameters());

        return exportedChannel;
    }

    private static Map<String, Object> toExportedElement(AiAgentElement element) {
        Map<String, Object> exportedElement = new LinkedHashMap<>();

        exportedElement.put(EXPORT_KIND, element.getKind());
        exportedElement.put(EXPORT_POSITION, element.getPosition());
        exportedElement.put(EXPORT_PARAMETERS, element.getParameters());

        return exportedElement;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, ?>> exportedList(@Nullable Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, ?>> entries = new ArrayList<>();

        for (Object entry : list) {
            if (entry instanceof Map<?, ?> map) {
                entries.add((Map<String, ?>) map);
            }
        }

        return entries;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> exportedMap(@Nullable Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        return (Map<String, Object>) map;
    }

    private static @Nullable String exportedString(Map<String, ?> exported, String key) {
        Object value = exported.get(key);

        return value instanceof String string ? string : null;
    }

    private static boolean isKnownChannelType(String channelType) {
        try {
            ChannelDefinitions.getChannelDefinition(channelType);

            return true;
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void updateAgentDeploymentTags(long id, List<Tag> tags) {
        // Same shape as updateAgentTags: persist any tag the client typed but never saved, so every id below is a
        // real row before a project_deployment_tag FK references it.
        List<Tag> savedTags = tags == null || tags.isEmpty() ? List.of() : tagService.save(tags);

        projectDeploymentService.update(id, savedTags.stream()
            .map(Tag::getId)
            .toList());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void updateAgentTags(long id, List<Tag> tags) {
        // Persist any tag the client typed but never saved, so every id below is a real row before it is referenced
        // by an ai_agent_tag FK.
        List<Tag> savedTags = tags == null || tags.isEmpty() ? List.of() : tagService.save(tags);

        agentService.update(id, savedTags.stream()
            .map(Tag::getId)
            .toList());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<AiAgentDeploymentDTO> getAgentDeployments(Long workspaceId) {
        List<AiAgent> agents = agentService.getAgents(workspaceId);

        List<AiAgentDeploymentDTO> agentDeployments = new ArrayList<>();

        for (AiAgent agent : agents) {
            for (Environment environment : Environment.values()) {
                projectDeploymentService.fetchProjectDeployment(agent.getProjectId(), environment)
                    .ifPresent(
                        projectDeployment -> agentDeployments.add(toAgentDeploymentDTO(agent, projectDeployment)));
            }
        }

        return agentDeployments;
    }

    /**
     * Same per-environment {@code fetchProjectDeployment} mechanism {@link #getAgentDeployments} and
     * {@link #hasAnyDeployment} rely on, narrowed to a single {@link Environment} and to the workflows the client can
     * actually open a chat against.
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<ChatAgentDTO> getWorkspaceChatAgents(Long workspaceId, long environmentId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        List<ChatAgentDTO> chatAgents = new ArrayList<>();

        for (AiAgent agent : agentService.getAgents(workspaceId)) {
            ProjectDeployment projectDeployment = projectDeploymentService
                .fetchProjectDeployment(agent.getProjectId(), environment)
                .orElse(null);

            if (projectDeployment == null || !projectDeployment.isEnabled()) {
                continue;
            }

            long projectDeploymentId = projectDeployment.getId();

            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
                projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId);

            for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
                if (!projectDeploymentWorkflow.isEnabled()) {
                    continue;
                }

                Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());

                List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

                if (!HostedChatTriggers.hasHostedChatTrigger(workflowTriggers)) {
                    continue;
                }

                String workflowExecutionId = resolveStaticWebhookExecutionId(
                    projectDeploymentId, workflow, workflowTriggers);

                if (workflowExecutionId == null) {
                    continue;
                }

                String workflowLabel = workflow.getLabel();

                chatAgents.add(
                    new ChatAgentDTO(
                        agent.getId(), agent.getName(), agent.getTitle(), projectDeploymentId, workflowExecutionId,
                        workflowLabel == null ? "Untitled Workflow" : workflowLabel));
            }
        }

        return chatAgents;
    }

    /**
     * Resolves the workflow's first non-{@code manual} {@code STATIC_WEBHOOK} trigger to its
     * {@link WorkflowExecutionId} string, or {@code null} when it has none.
     *
     * <p>
     * Replication of {@code ProjectDeploymentWorkflowGraphQlController.resolveStaticWebhookExecutionId} (minus its
     * per-request lookup maps, which exist only to batch that query's cross-project loads): that controller is in
     * {@code automation-configuration-graphql}, which {@code automation-ai-agent-service} cannot depend on without an
     * inverted dependency. The produced string is what the client hands straight back to the chat webhook endpoint, so
     * its construction — {@code (AUTOMATION, projectDeploymentId, projectWorkflow uuid, trigger name)} — must not drift
     * from the sibling query's.
     * </p>
     */
    private @Nullable String resolveStaticWebhookExecutionId(
        long projectDeploymentId, Workflow workflow, List<WorkflowTrigger> workflowTriggers) {

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.name(), workflowNodeType.version(),
                Objects.requireNonNull(workflowNodeType.operation()));

            if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
                !Objects.equals(triggerDefinition.getName(), MANUAL_TRIGGER_NAME)) {

                ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                    PlatformType.AUTOMATION, projectDeploymentId, projectWorkflow.getUuidAsString(),
                    workflowTrigger.getName());

                return workflowExecutionId.toString();
            }
        }

        return null;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public AiAgentDTO updateAgent(long id, String title, String description, String instructions) {
        AiAgent agent = agentService.getAgent(id);

        // Partial update, symmetric with updateAgentChannel/updateAgentElement: null on any of the three fields
        // leaves the existing value unchanged — there is currently no way to explicitly CLEAR description or
        // instructions through this method (do a client-side no-op or a future dedicated clear if that's ever
        // needed). This matters beyond title: description also feeds a SUB_AGENT element's generated toolDescription
        // (see AiAgentWorkflowGenerator.buildSubAgentElement / AiAgentFacadeImpl.resolveSubAgentRef), and
        // AgentInstructionsCard.tsx — the only client caller of this method — sends only instructions on every edit;
        // treating a null description as "clear" would silently wipe it on each instructions-only save.
        if (title != null) {
            agent.setTitle(title);
        }

        if (description != null) {
            agent.setDescription(description);
        }

        if (instructions != null) {
            agent.setInstructions(instructions);
        }

        AiAgent updatedAgent = agentService.update(agent);

        regenerateAndSaveWorkflow(updatedAgent);

        return toAgentDTO(updatedAgent);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void updateAgentSettings(long id, Map<String, Object> settings) {
        AiAgent agent = agentService.getAgent(id);

        // Whole-map replace, not a per-key merge — see the interface javadoc for why.
        agent.setSettings(settings == null ? Map.of() : settings);

        AiAgent updatedAgent = agentService.update(agent);

        regenerateAndSaveWorkflow(updatedAgent);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public AiAgentChannel addAgentChannel(
        long agentId, String channelType, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {

        AiAgent agent = agentService.getAgent(agentId);

        validateChannelType(channelType);

        List<AiAgentChannel> existingChannels = agentChannelService.getByAgentId(agentId);

        if (isPermanentChannelType(channelType)) {
            boolean alreadyPresent = existingChannels.stream()
                .anyMatch(channel -> channelType.equals(channel.getChannelType()));

            if (alreadyPresent) {
                throw new ConfigurationException(
                    "Agent " + agentId + " already has a '" + channelType + "' channel",
                    AiAgentErrorType.CHANNEL_ALREADY_PRESENT);
            }
        }

        AiAgentChannel channel = new AiAgentChannel(agentId, channelType);

        channel.setPosition(nextPosition(existingChannels, AiAgentChannel::getPosition));

        if (parameters != null) {
            channel.setParameters(parameters);
        }

        channel.setConnectionId(connectionId);

        AiAgentChannel savedChannel = agentChannelService.create(channel);

        regenerateAndSaveWorkflow(agent);

        return savedChannel;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void updateAgentChannel(
        long channelId, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
        AiAgentChannel channel = agentChannelService.getAgentChannel(channelId);

        // Both parameters are partial-update fields: null means "leave unchanged", symmetric with addAgentChannel's
        // null-parameters handling. There is currently no way to explicitly CLEAR a wired connectionId through this
        // method — do a deleteAgentChannel + addAgentChannel(..., connectionId = null) instead. See AiAgentFacade's
        // interface javadoc.
        if (parameters != null) {
            channel.setParameters(parameters);
        }

        if (connectionId != null) {
            channel.setConnectionId(connectionId);
        }

        agentChannelService.update(channel);

        regenerateAndSaveWorkflow(agentService.getAgent(channel.getAgentId()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void deleteAgentChannel(long channelId) {
        AiAgentChannel channel = agentChannelService.getAgentChannel(channelId);

        if (isPermanentChannelType(channel.getChannelType())) {
            throw new ConfigurationException(
                "Channel " + channelId + " of type '" + channel.getChannelType() + "' cannot be deleted",
                AiAgentErrorType.CHANNEL_NOT_DELETABLE);
        }

        AiAgent agent = agentService.getAgent(channel.getAgentId());

        agentChannelService.delete(channelId);

        regenerateAndSaveWorkflow(agent);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public AiAgentElement addAgentElement(
        long agentId, String kind, @Nullable Long referenceId, @Nullable Map<String, Object> parameters,
        @Nullable Long connectionId) {

        AiAgent agent = agentService.getAgent(agentId);

        List<AiAgentElement> existingElements = agentElementService.getByAgentId(agentId);

        if (isSingletonKind(kind)) {
            boolean alreadyPresent = existingElements.stream()
                .anyMatch(element -> kind.equals(element.getKind()));

            if (alreadyPresent) {
                throw new ConfigurationException(
                    "Agent " + agentId + " already has a '" + kind + "' element",
                    AiAgentErrorType.ELEMENT_KIND_ALREADY_PRESENT);
            }
        }

        if (AiAgentElement.KIND_SUB_AGENT.equals(kind)) {
            validateSubAgentReference(agentId, referenceId);
        }

        AiAgentElement element = new AiAgentElement(agentId, kind);

        element.setReferenceId(referenceId);

        if (parameters != null) {
            element.setParameters(parameters);
        }

        element.setConnectionId(connectionId);
        element.setPosition(nextPosition(existingElements, AiAgentElement::getPosition));

        AiAgentElement savedElement = agentElementService.create(element);

        regenerateAndSaveWorkflow(agent);

        return savedElement;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void updateAgentElement(
        long elementId, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
        AiAgentElement element = agentElementService.getAgentElement(elementId);

        // Both parameters are partial-update fields: null means "leave unchanged", symmetric with
        // updateAgentChannel's handling — see that method's comment for why, and how to clear a connectionId today.
        if (parameters != null) {
            element.setParameters(parameters);
        }

        if (connectionId != null) {
            element.setConnectionId(connectionId);
        }

        agentElementService.update(element);

        regenerateAndSaveWorkflow(agentService.getAgent(element.getAgentId()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public void deleteAgentElement(long elementId) {
        AiAgentElement element = agentElementService.getAgentElement(elementId);
        AiAgent agent = agentService.getAgent(element.getAgentId());

        agentElementService.delete(elementId);

        regenerateAndSaveWorkflow(agent);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public int publishAgent(long id, String description) {
        AiAgent agent = agentService.getAgent(id);

        validateForPublish(agent);

        // Same as every other mutation in this class — the draft workflow must reflect the agent's current
        // channels/elements/instructions before it is duplicated into the new published version.
        regenerateAndSaveWorkflow(agent);

        return publishProjectVersion(agent.getProjectId(), description);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public String getDraftWorkflowId(long agentId) {
        AiAgent agent = agentService.getAgent(agentId);
        Project project = projectService.getProject(agent.getProjectId());

        return getVersionWorkflowId(project.getId(), project.getLastProjectVersion());
    }

    // --- publish validation -------------------------------------------------------------------------------------

    private void validateForPublish(AiAgent agent) {
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        long modelCount = elements.stream()
            .filter(element -> AiAgentElement.KIND_MODEL.equals(element.getKind()))
            .count();

        if (modelCount != 1) {
            throw new ConfigurationException(
                "Agent " + agent.getId() + " must have exactly one MODEL element to publish, found " + modelCount,
                AiAgentErrorType.MODEL_MISSING);
        }

        for (AiAgentChannel channel : agentChannelService.getByAgentId(agent.getId())) {
            ChannelDefinition channelDefinition = ChannelDefinitions.getChannelDefinition(channel.getChannelType());

            if (channelDefinition.connectionRequired() && channel.getConnectionId() == null) {
                throw new ConfigurationException(
                    "Channel " + channel.getId() + " of type '" + channel.getChannelType()
                        + "' requires a connection to publish",
                    AiAgentErrorType.CHANNEL_CONNECTION_MISSING);
            }
        }

        for (AiAgentElement element : elements) {
            if (!AiAgentElement.KIND_SUB_AGENT.equals(element.getKind())) {
                continue;
            }

            Long referenceId = Objects.requireNonNull(
                element.getReferenceId(), "SUB_AGENT element referenceId");

            AiAgent targetAgent = agentService.getAgent(referenceId);
            Project targetProject = projectService.getProject(targetAgent.getProjectId());

            boolean targetHasPublishedVersion = targetProject.getProjectVersions()
                .stream()
                .anyMatch(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED);

            if (!targetHasPublishedVersion) {
                throw new ConfigurationException(
                    "Sub-agent " + targetAgent.getId() + " referenced by agent " + agent.getId()
                        + " has no published project version",
                    AiAgentErrorType.SUB_AGENT_NOT_PUBLISHED);
            }
        }

        // Approvals no longer need a precondition of their own: they are delivered over the agent's own channels
        // (AiAgentWorkflowGenerator.buildApprovalDeliveryChannels), and the chat channel is created with every agent
        // and cannot be deleted — so a gated TOOL or an APPROVAL_TOOL always has somewhere to ask. This used to
        // demand an APPROVAL_CHANNEL element, a row nothing creates any more.

        Map<String, ?> settings = agent.getSettings();

        if (AiAgentSettings.isWebSearchEnabled(settings)
            && AiAgentSettings.getWebSearchConnectionId(settings) == null) {
            throw new ConfigurationException(
                "Agent " + agent.getId()
                    + " has settings.builtInTools.webSearch enabled but no webSearchConnectionId to publish",
                AiAgentErrorType.BUILT_IN_TOOL_CONNECTION_MISSING);
        }
    }

    // --- publish ------------------------------------------------------------------------------------------------

    /**
     * Publishes {@code projectId}'s current draft version and duplicates every one of its workflows into the new
     * version, verbatim replicating {@code ProjectFacadeImpl.publishProject}'s body (as of this writing,
     * {@code server/libs/automation/automation-configuration/automation-configuration-service/.../
     * ProjectFacadeImpl.java:543-570}) with the raw services directly, rather than delegating to
     * {@code ProjectFacade.publishProject}. That facade method carries
     * {@code @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_EDIT')")} — shaped around user-visible projects —
     * and would reject the caller on an agent's hidden {@code __AI_AGENT__} project, the same reason the rest of this
     * class bypasses {@code ProjectWorkflowFacade} (see this class's javadoc).
     */
    int publishProjectVersion(long projectId, @Nullable String description) {
        int oldProjectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, oldProjectVersion);

        int newProjectVersion = projectService.publishProject(projectId, description, false);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.publishWorkflow(
                projectId, oldProjectVersion, oldWorkflowId, oldProjectWorkflow);

            workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
        }

        return newProjectVersion;
    }

    // --- draft regeneration -----------------------------------------------------------------------------------

    private void regenerateAndSaveWorkflow(AiAgent agent) {
        Project project = projectService.getProject(agent.getProjectId());
        String workflowId = getVersionWorkflowId(project.getId(), project.getLastProjectVersion());
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<AiAgentChannel> channels = agentChannelService.getByAgentId(agent.getId());
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        String definition = AiAgentWorkflowGenerator.generate(agent, channels, elements, this::resolveSubAgentRef);

        workflowService.update(workflowId, definition, workflow.getVersion());

        syncTestConnections(agent, workflowId, channels, elements);
    }

    /**
     * Test runs (see {@code TestWorkflowExecutorImpl.getWorkflowTestParameters}) source connections ONLY from a
     * workflow's {@code WorkflowTestConfiguration}, never from {@code AiAgentChannel}/{@code AiAgentElement} directly —
     * so every draft regeneration re-syncs each row's {@code connectionId} into the draft workflow's test
     * configuration, keyed exactly the way {@link AiAgentWorkflowGenerator#generate} emits its {@code connections}
     * blocks (see {@link AiAgentWorkflowGenerator#buildConnectionRefs}). Synced into every {@link Environment}
     * (mirroring {@link #hasAnyDeployment}'s per-environment loop) since a channel/element's {@code connectionId} is
     * not itself environment-scoped, but {@code WorkflowTestConfiguration} is.
     */
    private void syncTestConnections(
        AiAgent agent, String workflowId, List<AiAgentChannel> channels, List<AiAgentElement> elements) {

        Map<Long, Long> channelConnectionIds = channels.stream()
            .filter(channel -> channel.getConnectionId() != null)
            .collect(Collectors.toMap(AiAgentChannel::getId, AiAgentChannel::getConnectionId));

        Map<Long, Long> elementConnectionIds = elements.stream()
            .filter(element -> element.getConnectionId() != null)
            .collect(Collectors.toMap(AiAgentElement::getId, AiAgentElement::getConnectionId));

        Long webSearchConnectionId = AiAgentSettings.getWebSearchConnectionId(agent.getSettings());

        if (channelConnectionIds.isEmpty() && elementConnectionIds.isEmpty() && webSearchConnectionId == null) {
            return;
        }

        for (AiAgentWorkflowGenerator.ConnectionRef connectionRef : AiAgentWorkflowGenerator.buildConnectionRefs(
            agent, channels, elements)) {

            // An approval delivery node is owned by the agent_channel row it was derived from, so it resolves
            // through the CHANNEL branch and reuses that channel's own connection — no element-side fallback.
            Long connectionId = switch (connectionRef.ownerKind()) {
                case CHANNEL -> channelConnectionIds.get(connectionRef.ownerId());
                case ELEMENT -> elementConnectionIds.get(connectionRef.ownerId());
                case AGENT_SETTINGS -> webSearchConnectionId;
            };

            if (connectionId == null) {
                continue;
            }

            boolean isChannel = connectionRef.ownerKind() == ConnectionRefOwnerKind.CHANNEL;

            for (Environment environment : Environment.values()) {
                workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
                    workflowId, connectionRef.workflowNodeName(), connectionRef.workflowConnectionKey(), connectionId,
                    isChannel, environment.ordinal());
            }
        }
    }

    private String generateDefinition(AiAgent agent) {
        List<AiAgentChannel> channels = agentChannelService.getByAgentId(agent.getId());
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        return AiAgentWorkflowGenerator.generate(agent, channels, elements, this::resolveSubAgentRef);
    }

    private SubAgentRef resolveSubAgentRef(Long targetAgentId) {
        AiAgent targetAgent = agentService.getAgent(targetAgentId);

        return new SubAgentRef(targetAgent.getName(), targetAgent.getDescription(), targetAgent.getUuid());
    }

    /**
     * Resolves {@code (projectId, projectVersion)} to its single workflow id. Used for the current draft version
     * ({@code project.getLastProjectVersion()}) everywhere except {@link #toAgentDTO}, which also uses it for the last
     * *published* version — the name says "version", not "draft", because it is not draft-specific.
     */
    private String getVersionWorkflowId(long projectId, int projectVersion) {
        List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion);

        if (workflowIds.size() != 1) {
            throw new IllegalStateException(
                "Expected exactly one workflow for project " + projectId + " version " + projectVersion
                    + ", found " + workflowIds.size());
        }

        return workflowIds.get(0);
    }

    // --- read model ---------------------------------------------------------------------------------------------

    private AiAgentDTO toAgentDTO(AiAgent agent) {
        List<AiAgentChannel> channels = agentChannelService.getByAgentId(agent.getId());
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        Project project = projectService.getProject(agent.getProjectId());
        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        boolean unpublishedChanges;
        int lastPublishedVersion;
        Instant publishedDate;

        if (lastPublishedProjectVersion == null) {
            unpublishedChanges = true;
            lastPublishedVersion = 0;
            publishedDate = null;
        } else {
            lastPublishedVersion = lastPublishedProjectVersion.getVersion();
            publishedDate = lastPublishedProjectVersion.getPublishedDate();

            String draftWorkflowId = getVersionWorkflowId(project.getId(), project.getLastProjectVersion());
            String publishedWorkflowId = getVersionWorkflowId(project.getId(), lastPublishedVersion);

            // Content comparison, not a lastModifiedDate/publishedDate timestamp comparison (the original Task 10
            // heuristic): publishAgent's duplicate-then-relink step (see publishProjectVersion) always stamps the new
            // draft workflow's lastModifiedDate strictly *after* the just-published version's publishedDate — they're
            // set moments apart in the same publishAgent call — so a timestamp comparison would misreport
            // "unpublished changes" immediately after every publish, before any actual edit happened. The draft is
            // genuinely a byte-identical copy of what was just published (duplicateWorkflow copies the definition
            // string verbatim) until AiAgentWorkflowGenerator's deterministic regeneration actually produces different
            // JSON on a later mutation, so comparing definitions directly is both correct and precise.
            unpublishedChanges = !Objects.equals(
                workflowService.getWorkflow(draftWorkflowId)
                    .getDefinition(),
                workflowService.getWorkflow(publishedWorkflowId)
                    .getDefinition());
        }

        // One tag lookup per agent, so the list path is N+1 in tags. Deliberate: toAgentDTO already issues a Project
        // read plus two workflow reads per agent, so batching only this one query would not change the shape of the
        // list path — and agent counts per workspace are small.
        List<Tag> tags = tagService.getTags(agent.getTagIds());

        return new AiAgentDTO(agent, channels, elements, unpublishedChanges, lastPublishedVersion, publishedDate, tags);
    }

    /**
     * Builds one {@link AiAgentDeploymentDTO} row per {@link ProjectDeployment}, flattening in the owning agent's title
     * and every one of its workflows' per-trigger info.
     */
    private AiAgentDeploymentDTO toAgentDeploymentDTO(AiAgent agent, ProjectDeployment projectDeployment) {
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeployment.getId());

        List<AiAgentDeploymentWorkflowDTO> agentDeploymentWorkflowDTOs = projectDeploymentWorkflows.stream()
            .map(projectDeploymentWorkflow -> toAgentDeploymentWorkflowDTO(
                projectDeployment.getId(), projectDeploymentWorkflow))
            .toList();

        return new AiAgentDeploymentDTO(
            projectDeployment.getId(), projectDeployment.getName(), agent.getId(), agent.getTitle(),
            agent.getProjectId(), (int) projectDeployment.getEnvironmentId(), projectDeployment.isEnabled(),
            projectDeployment.getProjectVersion(), agentDeploymentWorkflowDTOs,
            tagService.getTags(projectDeployment.getTagIds()),
            getLastExecutionDate(projectDeployment.getId(), projectDeploymentWorkflows));
    }

    /**
     * The deployment's most recent finished run, derived exactly as {@code ProjectDeploymentFacadeImpl} derives its
     * own: the last job recorded against the deployment for its workflows. An agent deploys a single generated
     * workflow, so this is that workflow's last run.
     */
    private @Nullable Instant getLastExecutionDate(
        long projectDeploymentId, List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {

        List<String> workflowIds = projectDeploymentWorkflows.stream()
            .map(ProjectDeploymentWorkflow::getWorkflowId)
            .toList();

        if (workflowIds.isEmpty()) {
            return null;
        }

        return principalJobService.fetchLastWorkflowJobId(projectDeploymentId, workflowIds, PlatformType.AUTOMATION)
            .map(jobId -> {
                Job job = jobService.getJob(jobId);

                return job.getEndDate();
            })
            .orElse(null);
    }

    private AiAgentDeploymentWorkflowDTO toAgentDeploymentWorkflowDTO(
        long projectDeploymentId, ProjectDeploymentWorkflow projectDeploymentWorkflow) {

        Workflow workflow = workflowService.getWorkflow(projectDeploymentWorkflow.getWorkflowId());
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        List<AiAgentDeploymentTriggerDTO> agentDeploymentTriggerDTOs = workflowTriggers.stream()
            .map(workflowTrigger -> new AiAgentDeploymentTriggerDTO(
                workflowTrigger.getName(), workflowTrigger.getType(), workflowTrigger.getParameters(),
                resolveStaticWebhookUrl(projectDeploymentId, workflow, workflowTrigger)))
            .toList();

        return new AiAgentDeploymentWorkflowDTO(
            projectDeploymentWorkflow.getWorkflowId(), projectDeploymentWorkflow.isEnabled(),
            agentDeploymentTriggerDTOs);
    }

    /**
     * Resolves a SINGLE trigger's own static webhook URL. Every trigger on a generated agent workflow is a different
     * channel (see {@code AiAgentWorkflowGenerator}'s {@code branch_in}), and different channels can resolve to
     * different URLs (or none) — e.g. Slack/Rocket.Chat/WhatsApp are {@code STATIC_WEBHOOK} and each gets its own URL,
     * chat is {@code STATIC_WEBHOOK} too (the hosted-chat link), Telegram is {@code DYNAMIC_WEBHOOK} (it registers its
     * own webhook with the provider, so there is no URL for a human to copy — this returns {@code null} for it), and
     * schedule/workflowCall aren't webhook-triggered at all. Called once per trigger, rather than returning the first
     * hit for the whole workflow (the pre-fix behavior), so callers never mix up one channel's URL with another's.
     *
     * <p>
     * Same {@code STATIC_WEBHOOK}-trigger-detection mechanism {@code ProjectDeploymentFacadeImpl.getStaticWebhookUrl}
     * and {@code ProjectDeploymentWorkflowGraphQlController.staticWebhookUrl} use, replicated here rather than shared
     * because neither of those live in a module {@code automation-ai-agent-service} can depend on without an inverted
     * dependency (both are in {@code automation-configuration}, which does not know about agents).
     */
    private @Nullable String resolveStaticWebhookUrl(
        long projectDeploymentId, Workflow workflow, WorkflowTrigger workflowTrigger) {

        WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        TriggerDefinition triggerDefinition;

        try {
            triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
                Objects.requireNonNull(triggerWorkflowNodeType.operation()));
        } catch (Exception exception) {
            log.warn(
                "Unable to resolve trigger definition for node type '{}' (workflow {}, trigger {}) while computing "
                    + "an agent deployment's static webhook URL; treating it as having no webhook URL",
                workflowTrigger.getType(), workflow.getId(), workflowTrigger.getName(), exception);

            return null;
        }

        if (triggerDefinition.getType() == TriggerType.STATIC_WEBHOOK &&
            !Objects.equals(triggerDefinition.getName(), MANUAL_TRIGGER_NAME)) {

            ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflow.getId());

            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, projectDeploymentId, projectWorkflow.getUuidAsString(),
                workflowTrigger.getName());

            return webhookUrl.replace("{id}", workflowExecutionId.toString());
        }

        return null;
    }

    // --- channel/element validation -----------------------------------------------------------------------------

    private static boolean isPermanentChannelType(String channelType) {
        return AiAgentChannelType.CHAT.equals(channelType) || AiAgentChannelType.WORKFLOW_CALL.equals(channelType);
    }

    private static void validateChannelType(String channelType) {
        try {
            ChannelDefinitions.getChannelDefinition(channelType);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                illegalArgumentException.getMessage(), AiAgentErrorType.UNKNOWN_CHANNEL_TYPE);
        }
    }

    /**
     * KNOWLEDGE_BASE is deliberately absent: RAG is a multiple cluster element, so an agent carries one rag entry per
     * knowledge base, each with its own retrieval parameters. The rest stay singletons — an agent has one model, one
     * chat memory, and the two approval rows are switches rather than collections.
     */
    private static boolean isSingletonKind(String kind) {
        return AiAgentElement.KIND_MODEL.equals(kind) || AiAgentElement.KIND_CHAT_MEMORY.equals(kind)
            || AiAgentElement.KIND_APPROVAL_GATE.equals(kind) || AiAgentElement.KIND_APPROVAL_TOOL.equals(kind);
    }

    private void validateSubAgentReference(long agentId, @Nullable Long referenceId) {
        if (referenceId == null) {
            throw new IllegalArgumentException("SUB_AGENT element requires a referenceId");
        }

        if (referenceId == agentId) {
            throw new ConfigurationException("Agent " + agentId + " cannot reference itself as a sub-agent",
                AiAgentErrorType.SUB_AGENT_CYCLE);
        }

        // Confirms the target exists (throws IllegalArgumentException otherwise, same as every other by-id lookup
        // in this facade).
        agentService.getAgent(referenceId);

        if (isReachable(referenceId, agentId, new HashSet<>())) {
            throw new ConfigurationException(
                "Agent " + referenceId + " already (transitively) references agent " + agentId
                    + " as a sub-agent; adding this reference would create a cycle",
                AiAgentErrorType.SUB_AGENT_CYCLE);
        }
    }

    /**
     * Whether {@code targetAgentId} is reachable from {@code startAgentId} by following {@code SUB_AGENT} element
     * references. Used to detect that adding a {@code startAgentId -> ... -> targetAgentId} edge, on top of the
     * proposed new {@code targetAgentId -> startAgentId} edge, would close a cycle.
     */
    private boolean isReachable(long startAgentId, long targetAgentId, Set<Long> visited) {
        if (!visited.add(startAgentId)) {
            return false;
        }

        for (AiAgentElement element : agentElementService.getByAgentId(startAgentId)) {
            if (!AiAgentElement.KIND_SUB_AGENT.equals(element.getKind())) {
                continue;
            }

            Long subAgentId = element.getReferenceId();

            if (subAgentId == null) {
                continue;
            }

            if (subAgentId == targetAgentId || isReachable(subAgentId, targetAgentId, visited)) {
                return true;
            }
        }

        return false;
    }

    // --- naming --------------------------------------------------------------------------------------------------

    /**
     * Lowercases, collapses every run of characters outside {@code AiAgent.NAME_PATTERN}'s alphabet into a single
     * hyphen, trims leading/trailing hyphens, and clamps to {@value #MAX_NAME_LENGTH} chars. Same idiom as
     * {@code AiHubTaskServiceImpl.slugify} (EE), duplicated here because automation-ai-agent (CE) cannot depend on the
     * EE ai-hub module — see {@code SystemProjects}'s comment on {@code MCP_SERVER_DEPLOYMENT_NAME_PREFIX} for the same
     * precedent.
     */
    private static String slugify(String title) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        String lower = title.toLowerCase(Locale.ROOT);
        String collapsed = lower.replaceAll("[^a-z0-9_-]+", "-");
        String trimmed = collapsed.replaceAll("^-+|-+$", "");

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                "title '" + title + "' produces an empty slug — use letters, digits, hyphens, or underscores");
        }

        return truncate(trimmed);
    }

    private static String truncate(String name) {
        if (name.length() <= MAX_NAME_LENGTH) {
            return name;
        }

        return name.substring(0, MAX_NAME_LENGTH)
            .replaceAll("-+$", "");
    }

    /**
     * Suffixes {@code baseName} with {@code -2}, {@code -3}, ... until it does not collide with any existing agent name
     * in the caller's workspace (per the Task 10 brief: uniqueness is scoped to
     * {@code agentService.getAgents(workspaceId)}, not global).
     */
    private String uniqueName(String baseName, Long workspaceId) {
        Set<String> existingNames = agentService.getAgents(workspaceId)
            .stream()
            .map(AiAgent::getName)
            .collect(Collectors.toSet());

        if (!existingNames.contains(baseName)) {
            return baseName;
        }

        for (int suffix = 2;; suffix++) {
            String suffixText = "-" + suffix;
            String truncatedBase = baseName.length() > MAX_NAME_LENGTH - suffixText.length()
                ? baseName.substring(0, MAX_NAME_LENGTH - suffixText.length())
                : baseName;
            String candidate = truncatedBase + suffixText;

            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }
    }

    // --- positioning ---------------------------------------------------------------------------------------------

    private static <T> int nextPosition(List<T> existing, ToIntFunction<T> positionExtractor) {
        return existing.stream()
            .mapToInt(positionExtractor)
            .max()
            .orElse(-1) + 1;
    }
}
