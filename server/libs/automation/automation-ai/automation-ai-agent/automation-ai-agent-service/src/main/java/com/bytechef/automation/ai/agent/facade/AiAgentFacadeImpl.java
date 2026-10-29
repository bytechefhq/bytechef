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
import com.bytechef.automation.ai.agent.channel.AgentChannelResolver;
import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.domain.AiAgentVersion;
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
import com.bytechef.automation.ai.agent.service.AiAgentVersionService;
import com.bytechef.automation.ai.agent.util.AiAgentSettings;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator;
import com.bytechef.automation.ai.agent.util.AiAgentWorkflowGenerator.SubAgentRef;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.HostedChatTriggers;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AiAgentFacadeImpl implements AiAgentFacade {

    private static final Logger log = LoggerFactory.getLogger(AiAgentFacadeImpl.class);

    private static final int MAX_NAME_LENGTH = 64;

    private static final String MANUAL_TRIGGER_NAME = "manual";

    private static final String EXPORT_VERSION = "exportVersion";
    private static final int EXPORT_VERSION_VALUE = 1;
    private static final String EXPORT_NAME = "name";
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
    private static final String SNAPSHOT_CONNECTION_ID = "connectionId";
    private static final String SNAPSHOT_REFERENCE_ID = "referenceId";
    private static final String SNAPSHOT_REFERENCE_UUID = "referenceUuid";
    private static final String SNAPSHOT_UUID = "uuid";

    private static final Set<String> REFERENCE_ELEMENT_KINDS = Set.of(
        AiAgentElement.KIND_SKILL, AiAgentElement.KIND_SUB_AGENT, AiAgentElement.KIND_KNOWLEDGE_BASE);

    private static final Set<String> SKIPPED_IMPORT_ELEMENT_KINDS = Set.of(
        AiAgentElement.KIND_SKILL, AiAgentElement.KIND_SUB_AGENT, AiAgentElement.KIND_KNOWLEDGE_BASE,
        AiAgentElement.KIND_CHAT_MEMORY);

    private final AiAgentService agentService;
    private final AgentChannelResolver agentChannelResolver;
    private final AiAgentChannelService agentChannelService;
    private final AiAgentElementService agentElementService;
    private final AiAgentVersionService agentVersionService;
    private final EnvironmentService environmentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkspaceConnectionService workspaceConnectionService;
    private final WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService;
    private final PrincipalJobService principalJobService;
    private final JobService jobService;
    private final UserService userService;
    private final String webhookUrl;

    @SuppressFBWarnings("EI2")
    public AiAgentFacadeImpl(
        AiAgentService agentService, AgentChannelResolver agentChannelResolver,
        AiAgentChannelService agentChannelService,
        AiAgentElementService agentElementService, AiAgentVersionService agentVersionService,
        EnvironmentService environmentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        TriggerDefinitionService triggerDefinitionService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceConnectionService workspaceConnectionService,
        WorkspaceKnowledgeBaseService workspaceKnowledgeBaseService, PrincipalJobService principalJobService,
        JobService jobService, UserService userService,
        @Value("${bytechef.webhook-url}") String webhookUrl,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.agentService = agentService;
        this.agentChannelResolver = agentChannelResolver;
        this.agentChannelService = agentChannelService;
        this.agentElementService = agentElementService;
        this.agentVersionService = agentVersionService;
        this.environmentService = environmentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workspaceConnectionService = workspaceConnectionService;
        this.workspaceKnowledgeBaseService = workspaceKnowledgeBaseService;
        this.principalJobService = principalJobService;
        this.jobService = jobService;
        this.userService = userService;
        this.webhookUrl = webhookUrl;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AGENT_CREATE')")
    public AiAgentDTO createAgent(String title, String description, long workspaceId, @Nullable Long projectId) {
        Objects.requireNonNull(title, "title");

        return createAgent(title, description, workspaceId, projectId, title);
    }

    private AiAgentDTO createAgent(
        String title, @Nullable String description, long workspaceId, @Nullable Long projectId, String baseName) {

        String name = uniqueName(slugify(baseName), workspaceId);

        Project project = projectId == null
            ? createAgentProject(title, description, workspaceId)
            : getWorkspaceProject(projectId, workspaceId);

        AiAgent agent = new AiAgent();

        agent.setName(name);
        agent.setTitle(title);
        agent.setDescription(description);
        agent.setWorkspaceId(workspaceId);
        agent.setUuid(UUID.randomUUID());

        Workflow workflow = workflowService.create(
            JsonUtils.write(Map.of("label", name, "tasks", List.of())), Workflow.Format.JSON,
            Workflow.SourceType.JDBC);

        ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
            project.getId(), project.getLastProjectVersion(), workflow.getId(), ProjectWorkflowType.AI_AGENT);

        agent.setProjectWorkflowUuid(projectWorkflow.getUuid());

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

        regenerateAndSaveWorkflow(savedAgent, project.getId());

        return toAgentDTO(savedAgent, project.getId());
    }

    private Project createAgentProject(String title, @Nullable String description, long workspaceId) {
        Project project = new Project();

        project.setName(uniqueProjectName(title, workspaceId));
        project.setDescription(description);
        project.setWorkspaceId(workspaceId);

        return projectService.create(project);
    }

    private Project getWorkspaceProject(long projectId, long workspaceId) {
        Project project = projectService.getProject(projectId);

        if (!Objects.equals(project.getWorkspaceId(), workspaceId) || SystemProjects.isSystemProject(project)) {

            throw new IllegalArgumentException(
                "Project " + projectId + " is not available in workspace " + workspaceId);
        }

        return project;
    }

    private String uniqueProjectName(String title, long workspaceId) {
        Set<String> projectNames = projectService.getProjects(null, null, null, null, null, workspaceId)
            .stream()
            .map(Project::getName)
            .collect(Collectors.toSet());

        if (!projectNames.contains(title)) {
            return title;
        }

        int suffix = 2;

        while (projectNames.contains(title + " (" + suffix + ")")) {
            suffix++;
        }

        return title + " (" + suffix + ")";
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_DELETE')")
    public void deleteAgent(long id) {
        AiAgent agent = agentService.getAgent(id);
        long projectId = agentService.getProjectId(agent);

        if (isEnabledInAnyDeployment(agent, projectId)) {
            throw new ConfigurationException(
                "Agent " + id + " cannot be deleted while it is enabled in a deployment",
                AiAgentErrorType.AGENT_HAS_DEPLOYMENTS);
        }

        if (!agentService.getSubAgentReferencingAgents(id)
            .isEmpty()) {

            throw new ConfigurationException(
                "Agent " + id + " cannot be deleted while it is referenced as a sub-agent",
                AiAgentErrorType.AGENT_REFERENCED_AS_SUB_AGENT);
        }

        List<ProjectWorkflow> agentProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, String.valueOf(agent.getProjectWorkflowUuid()));

        agentService.delete(id);

        List<String> deletedWorkflowIds = new ArrayList<>();

        for (ProjectWorkflow agentProjectWorkflow : agentProjectWorkflows) {
            String workflowId = agentProjectWorkflow.getWorkflowId();

            projectWorkflowService.delete(
                agentProjectWorkflow.getProjectId(), agentProjectWorkflow.getProjectVersion(), workflowId);

            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(workflowId);
            }

            for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
                .getWorkflowProjectDeploymentWorkflows(workflowId)) {

                projectDeploymentWorkflowService.delete(projectDeploymentWorkflow.getId());
            }

            workflowService.delete(workflowId);

            deletedWorkflowIds.add(workflowId);
        }

        workflowTestConfigurationService.delete(deletedWorkflowIds);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")
    public void deleteProjectAgents(long projectId) {
        List<AiAgent> projectAgents = agentService.getProjectAgents(projectId);

        Set<Long> projectAgentIds = projectAgents.stream()
            .map(AiAgent::getId)
            .collect(Collectors.toSet());

        for (AiAgent projectAgent : projectAgents) {
            boolean referencedFromOutside = agentService.getSubAgentReferencingAgents(projectAgent.getId())
                .stream()
                .anyMatch(referencingAgent -> !projectAgentIds.contains(referencingAgent.getId()));

            if (referencedFromOutside) {
                throw new ConfigurationException(
                    "Project " + projectId + " cannot be deleted while its agent " + projectAgent.getId() +
                        " is referenced as a sub-agent from another project",
                    AiAgentErrorType.AGENT_REFERENCED_AS_SUB_AGENT);
            }
        }

        for (AiAgent projectAgent : projectAgents) {
            agentService.delete(projectAgent.getId());
        }
    }

    private boolean isEnabledInAnyDeployment(AiAgent agent, long projectId) {
        for (Environment environment : Environment.values()) {
            for (ProjectDeployment projectDeployment : projectDeployments(projectId, environment)) {
                boolean enabled = agentDeploymentWorkflows(agent, projectDeployment)
                    .stream()
                    .anyMatch(ProjectDeploymentWorkflow::isEnabled);

                if (enabled) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<ProjectDeployment> projectDeployments(long projectId, Environment environment) {
        return projectDeploymentService.getAllProjectDeployments(projectId)
            .stream()
            .filter(projectDeployment -> projectDeployment.getEnvironment() == environment)
            .toList();
    }

    private List<ProjectDeploymentWorkflow> agentDeploymentWorkflows(
        AiAgent agent, ProjectDeployment projectDeployment) {

        Optional<String> agentWorkflowIdOptional = projectWorkflowService.fetchProjectWorkflowWorkflowId(
            projectDeployment.getId(), String.valueOf(agent.getProjectWorkflowUuid()));

        if (agentWorkflowIdOptional.isEmpty()) {
            return List.of();
        }

        String agentWorkflowId = agentWorkflowIdOptional.get();

        return projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeployment.getId())
            .stream()
            .filter(projectDeploymentWorkflow -> agentWorkflowId.equals(projectDeploymentWorkflow.getWorkflowId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_VIEW')")
    @Transactional(readOnly = true)
    public AiAgentDTO getAgent(long id) {
        return toAgentDTO(agentService.getAgent(id));
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AGENT_VIEW')")
    @Transactional(readOnly = true)
    public List<AiAgentDTO> getAgents(long workspaceId) {
        return visibleAgents(workspaceId)
            .stream()
            .map(projectAgent -> toAgentDTO(projectAgent.agent(), projectAgent.projectId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_VIEW')")
    @Transactional(readOnly = true)
    public List<AiAgentVersionDTO> getAgentVersions(long id) {
        AiAgent agent = agentService.getAgent(id);
        Project project = projectService.getProject(agentService.getProjectId(agent));

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
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_VIEW')")
    @Transactional(readOnly = true)
    public String exportAgent(long id) {
        AiAgent agent = agentService.getAgent(id);

        return JsonUtils.writeWithDefaultPrettyPrinter(toExportedAgent(agent, false));
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public List<String> exportProjectAgentVersions(long projectId, int projectVersion) {
        List<String> exportedAgents = new ArrayList<>();

        for (AiAgentVersion agentVersion : agentVersionService.getProjectAgentVersions(projectId, projectVersion)) {
            Map<String, Object> exportedAgent = new LinkedHashMap<>(JsonUtils.readMap(agentVersion.getDefinition()));

            exportedAgent.remove(SNAPSHOT_UUID);
            exportedAgent.put(EXPORT_CHANNELS, withoutSnapshotIds(exportedAgent.get(EXPORT_CHANNELS)));
            exportedAgent.put(EXPORT_ELEMENTS, withoutSnapshotIds(exportedAgent.get(EXPORT_ELEMENTS)));

            exportedAgents.add(JsonUtils.writeWithDefaultPrettyPrinter(exportedAgent));
        }

        return exportedAgents;
    }

    private Map<String, Object> toExportedAgent(AiAgent agent, boolean snapshot) {
        List<Map<String, Object>> channels = agentChannelService.getByAgentId(agent.getId())
            .stream()
            .map(channel -> toExportedChannel(channel, snapshot))
            .toList();

        List<Map<String, Object>> elements = agentElementService.getByAgentId(agent.getId())
            .stream()
            .map(element -> toExportedElement(element, snapshot))
            .toList();

        Map<String, Object> exportedAgent = new LinkedHashMap<>();

        exportedAgent.put(EXPORT_VERSION, EXPORT_VERSION_VALUE);
        exportedAgent.put(EXPORT_NAME, agent.getName());
        exportedAgent.put(EXPORT_TITLE, agent.getTitle());
        exportedAgent.put(EXPORT_DESCRIPTION, agent.getDescription());
        exportedAgent.put(EXPORT_INSTRUCTIONS, agent.getInstructions());
        exportedAgent.put(EXPORT_SETTINGS, agent.getSettings());
        exportedAgent.put(EXPORT_CHANNELS, channels);
        exportedAgent.put(EXPORT_ELEMENTS, elements);

        if (snapshot) {
            exportedAgent.put(SNAPSHOT_UUID, String.valueOf(agent.getUuid()));
        }

        return exportedAgent;
    }

    private static List<Map<String, Object>> withoutSnapshotIds(@Nullable Object value) {
        List<Map<String, Object>> entries = new ArrayList<>();

        for (Map<String, ?> entry : exportedList(value)) {
            Map<String, Object> exportedEntry = new LinkedHashMap<>(entry);

            exportedEntry.remove(SNAPSHOT_CONNECTION_ID);
            exportedEntry.remove(SNAPSHOT_REFERENCE_ID);
            exportedEntry.remove(SNAPSHOT_REFERENCE_UUID);

            entries.add(exportedEntry);
        }

        return entries;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AGENT_CREATE')")
    public AiAgentDTO importAgent(long workspaceId, String json, @Nullable Long projectId) {
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

        String exportedName = exportedString(exportedAgent, EXPORT_NAME);

        AiAgentDTO agentDTO = createAgent(
            title, exportedString(exportedAgent, EXPORT_DESCRIPTION), workspaceId, projectId,
            exportedName == null || exportedName.isBlank() ? title : exportedName);

        AiAgent agent = agentDTO.agent();

        agent.setInstructions(exportedString(exportedAgent, EXPORT_INSTRUCTIONS));
        agent.setSettings(exportedMap(exportedAgent.get(EXPORT_SETTINGS)));

        agentService.update(agent);

        long agentId = agent.getId();

        for (Map<String, ?> channel : exportedList(exportedAgent.get(EXPORT_CHANNELS))) {
            String channelType = exportedString(channel, EXPORT_CHANNEL_TYPE);

            if (channelType == null || isPermanentChannelType(channelType) || !isKnownChannelType(channelType)) {
                continue;
            }

            addAgentChannel(agentId, channelType, exportedMap(channel.get(EXPORT_PARAMETERS)), null);
        }

        for (Map<String, ?> element : exportedList(exportedAgent.get(EXPORT_ELEMENTS))) {
            String kind = exportedString(element, EXPORT_KIND);

            if (kind == null || SKIPPED_IMPORT_ELEMENT_KINDS.contains(kind)) {
                continue;
            }

            addAgentElement(agentId, kind, null, exportedMap(element.get(EXPORT_PARAMETERS)), null);
        }

        return toAgentDTO(agentService.getAgent(agentId));
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AGENT_CREATE')")
    public List<AiAgentDTO> copyProjectAgents(long sourceProjectId, long targetProjectId, long workspaceId) {
        getWorkspaceProject(sourceProjectId, workspaceId);

        List<AiAgent> sourceAgents = agentService.getProjectAgents(sourceProjectId);

        Map<Long, AiAgent> copiedAgents = new LinkedHashMap<>();

        for (AiAgent sourceAgent : sourceAgents) {
            AiAgentDTO copiedAgentDTO = createAgent(
                sourceAgent.getTitle(), sourceAgent.getDescription(), workspaceId, targetProjectId,
                sourceAgent.getName());

            AiAgent copiedAgent = copiedAgentDTO.agent();

            copiedAgent.setInstructions(sourceAgent.getInstructions());
            copiedAgent.setSettings(new HashMap<>(sourceAgent.getSettings()));

            copiedAgents.put(sourceAgent.getId(), agentService.update(copiedAgent));
        }

        for (AiAgent sourceAgent : sourceAgents) {
            AiAgent copiedAgent = copiedAgents.get(sourceAgent.getId());

            copyAgentChannels(sourceAgent.getId(), copiedAgent.getId());
            copyAgentElements(sourceAgent.getId(), copiedAgent.getId(), copiedAgents);
        }

        List<AiAgentDTO> copiedAgentDTOs = new ArrayList<>();

        for (AiAgent copiedAgent : copiedAgents.values()) {
            regenerateAndSaveWorkflow(copiedAgent, targetProjectId);

            copiedAgentDTOs.add(toAgentDTO(agentService.getAgent(copiedAgent.getId()), targetProjectId));
        }

        return copiedAgentDTOs;
    }

    private void copyAgentChannels(long sourceAgentId, long copiedAgentId) {
        List<AiAgentChannel> permanentChannels = agentChannelService.getByAgentId(copiedAgentId);

        for (AiAgentChannel sourceChannel : agentChannelService.getByAgentId(sourceAgentId)) {
            AiAgentChannel channel = permanentChannels.stream()
                .filter(permanentChannel -> Objects.equals(
                    permanentChannel.getChannelType(), sourceChannel.getChannelType()))
                .findFirst()
                .orElseGet(() -> new AiAgentChannel(copiedAgentId, sourceChannel.getChannelType()));

            channel.setConnectionId(sourceChannel.getConnectionId());
            channel.setParameters(new HashMap<>(sourceChannel.getParameters()));
            channel.setPosition(sourceChannel.getPosition());

            if (channel.getId() == null) {
                agentChannelService.create(channel);
            } else {
                agentChannelService.update(channel);
            }
        }
    }

    private void copyAgentElements(long sourceAgentId, long copiedAgentId, Map<Long, AiAgent> copiedAgents) {
        for (AiAgentElement element : agentElementService.getByAgentId(copiedAgentId)) {
            agentElementService.delete(element.getId());
        }

        for (AiAgentElement sourceElement : agentElementService.getByAgentId(sourceAgentId)) {
            AiAgentElement element = new AiAgentElement(copiedAgentId, sourceElement.getKind());

            Long referenceId = sourceElement.getReferenceId();

            if (AiAgentElement.KIND_SUB_AGENT.equals(sourceElement.getKind()) && referenceId != null &&
                copiedAgents.containsKey(referenceId)) {

                referenceId = copiedAgents.get(referenceId)
                    .getId();
            }

            element.setConnectionId(sourceElement.getConnectionId());
            element.setParameters(new HashMap<>(sourceElement.getParameters()));
            element.setPosition(sourceElement.getPosition());
            element.setReferenceId(referenceId);

            agentElementService.create(element);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_EDIT')")
    public AiAgentDTO updateAgentFromExport(long id, String json) {
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

        AiAgent agent = agentService.getAgent(id);

        agent.setTitle(title);
        agent.setDescription(exportedString(exportedAgent, EXPORT_DESCRIPTION));
        agent.setInstructions(exportedString(exportedAgent, EXPORT_INSTRUCTIONS));
        agent.setSettings(exportedMap(exportedAgent.get(EXPORT_SETTINGS)));

        AiAgent updatedAgent = agentService.update(agent);

        applyExportedChannels(id, exportedList(exportedAgent.get(EXPORT_CHANNELS)));
        applyExportedElements(id, exportedList(exportedAgent.get(EXPORT_ELEMENTS)));

        regenerateAndSaveWorkflow(updatedAgent);

        return toAgentDTO(agentService.getAgent(id));
    }

    private void applyExportedChannels(long agentId, List<Map<String, ?>> exportedChannels) {
        List<AiAgentChannel> existingChannels = new ArrayList<>(agentChannelService.getByAgentId(agentId));

        Map<String, List<AiAgentChannel>> existingChannelsByType = existingChannels.stream()
            .collect(
                Collectors.groupingBy(
                    AiAgentChannel::getChannelType, LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));

        for (Map<String, ?> exportedChannel : exportedChannels) {
            String channelType = exportedString(exportedChannel, EXPORT_CHANNEL_TYPE);

            if (channelType == null || !isKnownChannelType(channelType)) {
                continue;
            }

            List<AiAgentChannel> sameTypeChannels = existingChannelsByType.getOrDefault(channelType, new ArrayList<>());
            Map<String, Object> parameters = exportedMap(exportedChannel.get(EXPORT_PARAMETERS));

            if (sameTypeChannels.isEmpty()) {
                if (isPermanentChannelType(channelType)) {
                    continue;
                }

                AiAgentChannel channel = new AiAgentChannel(agentId, channelType);

                channel.setParameters(parameters);
                channel.setPosition(nextPosition(existingChannels, AiAgentChannel::getPosition));

                existingChannels.add(agentChannelService.create(channel));
            } else {
                AiAgentChannel channel = sameTypeChannels.removeFirst();

                channel.setParameters(parameters);

                agentChannelService.update(channel);
            }
        }

        for (List<AiAgentChannel> unmatchedChannels : existingChannelsByType.values()) {
            for (AiAgentChannel unmatchedChannel : unmatchedChannels) {
                if (!isPermanentChannelType(unmatchedChannel.getChannelType())) {
                    agentChannelService.delete(unmatchedChannel.getId());
                }
            }
        }
    }

    private void applyExportedElements(long agentId, List<Map<String, ?>> exportedElements) {
        List<AiAgentElement> existingElements = agentElementService.getByAgentId(agentId);

        Map<String, List<AiAgentElement>> existingElementsByKind = existingElements.stream()
            .filter(element -> !REFERENCE_ELEMENT_KINDS.contains(element.getKind()))
            .collect(
                Collectors.groupingBy(
                    AiAgentElement::getKind, LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));

        List<AiAgentElement> positionedElements = new ArrayList<>(existingElements);

        for (Map<String, ?> exportedElement : exportedElements) {
            String kind = exportedString(exportedElement, EXPORT_KIND);

            if (kind == null || REFERENCE_ELEMENT_KINDS.contains(kind)) {
                continue;
            }

            List<AiAgentElement> sameKindElements = existingElementsByKind.getOrDefault(kind, new ArrayList<>());
            Map<String, Object> parameters = exportedMap(exportedElement.get(EXPORT_PARAMETERS));

            if (sameKindElements.isEmpty()) {
                AiAgentElement element = new AiAgentElement(agentId, kind);

                element.setParameters(parameters);
                element.setPosition(nextPosition(positionedElements, AiAgentElement::getPosition));

                positionedElements.add(agentElementService.create(element));
            } else {
                AiAgentElement element = sameKindElements.removeFirst();

                element.setParameters(parameters);

                agentElementService.update(element);
            }
        }

        for (List<AiAgentElement> unmatchedElements : existingElementsByKind.values()) {
            for (AiAgentElement unmatchedElement : unmatchedElements) {
                agentElementService.delete(unmatchedElement.getId());
            }
        }
    }

    private static Map<String, Object> toExportedChannel(AiAgentChannel channel, boolean snapshot) {
        Map<String, Object> exportedChannel = new LinkedHashMap<>();

        exportedChannel.put(EXPORT_CHANNEL_TYPE, channel.getChannelType());
        exportedChannel.put(EXPORT_POSITION, channel.getPosition());
        exportedChannel.put(EXPORT_PARAMETERS, channel.getParameters());

        if (snapshot && channel.getConnectionId() != null) {
            exportedChannel.put(SNAPSHOT_CONNECTION_ID, channel.getConnectionId());
        }

        return exportedChannel;
    }

    private Map<String, Object> toExportedElement(AiAgentElement element, boolean snapshot) {
        Map<String, Object> exportedElement = new LinkedHashMap<>();

        exportedElement.put(EXPORT_KIND, element.getKind());
        exportedElement.put(EXPORT_POSITION, element.getPosition());
        exportedElement.put(EXPORT_PARAMETERS, element.getParameters());

        if (snapshot && element.getConnectionId() != null) {
            exportedElement.put(SNAPSHOT_CONNECTION_ID, element.getConnectionId());
        }

        Long referenceId = element.getReferenceId();

        if (snapshot && referenceId != null) {
            exportedElement.put(SNAPSHOT_REFERENCE_ID, referenceId);

            if (AiAgentElement.KIND_SUB_AGENT.equals(element.getKind())) {
                Optional<AiAgent> subAgent = agentService.fetchAgent(referenceId);

                subAgent
                    .ifPresent(agent -> exportedElement.put(SNAPSHOT_REFERENCE_UUID, String.valueOf(agent.getUuid())));
            }
        }

        return exportedElement;
    }

    private static @Nullable Long exportedLong(Map<String, ?> exported, String key) {
        return exported.get(key) instanceof Number number ? number.longValue() : null;
    }

    private static int exportedPosition(Map<String, ?> exported) {
        return exported.get(EXPORT_POSITION) instanceof Number number ? number.intValue() : 0;
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

    private boolean isKnownChannelType(String channelType) {
        Optional<ResolvedAgentChannel> resolvedAgentChannel = agentChannelResolver.resolve(channelType);

        return resolvedAgentChannel.isPresent();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<ResolvedAgentChannel> getAgentChannelDefinitions() {
        return agentChannelResolver.resolveAll();
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AGENT_VIEW')")
    @Transactional(readOnly = true)
    public List<AiAgentDeploymentDTO> getAgentDeployments(long workspaceId) {
        List<ProjectAgent> projectAgents = visibleAgents(workspaceId);

        List<AiAgentDeploymentDTO> agentDeployments = new ArrayList<>();

        for (ProjectAgent projectAgent : projectAgents) {
            for (Environment environment : Environment.values()) {
                for (ProjectDeployment projectDeployment : projectDeployments(projectAgent.projectId(), environment)) {
                    List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = agentDeploymentWorkflows(
                        projectAgent.agent(), projectDeployment);

                    if (projectDeploymentWorkflows.isEmpty()) {
                        continue;
                    }

                    agentDeployments.add(
                        toAgentDeploymentDTO(
                            projectAgent.agent(), projectAgent.projectId(), projectDeployment,
                            projectDeploymentWorkflows));
                }
            }
        }

        return agentDeployments;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'AGENT_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<ChatAgentDTO> getWorkspaceChatAgents(long workspaceId, long environmentId) {
        Environment environment = environmentService.getEnvironment(environmentId);

        List<ChatAgentDTO> chatAgents = new ArrayList<>();

        for (ProjectAgent projectAgent : visibleAgents(workspaceId)) {
            AiAgent agent = projectAgent.agent();

            for (ProjectDeployment projectDeployment : projectDeployments(projectAgent.projectId(), environment)) {
                if (!projectDeployment.isEnabled()) {
                    continue;
                }

                chatAgents.addAll(toChatAgents(agent, projectDeployment));
            }
        }

        return chatAgents;
    }

    private List<ChatAgentDTO> toChatAgents(AiAgent agent, ProjectDeployment projectDeployment) {
        long projectDeploymentId = projectDeployment.getId();

        List<ChatAgentDTO> chatAgents = new ArrayList<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : agentDeploymentWorkflows(agent, projectDeployment)) {
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

        return chatAgents;
    }

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
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_EDIT')")
    public AiAgentDTO updateAgent(long id, String title, String description, String instructions) {
        AiAgent agent = agentService.getAgent(id);

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
    @PreAuthorize("hasPermission(#id, 'AiAgent', 'AGENT_EDIT')")
    public void updateAgentSettings(long id, Map<String, Object> settings) {
        AiAgent agent = agentService.getAgent(id);

        if (settings != null) {
            validateConnectionInAgentWorkspace(agent, AiAgentSettings.getWebSearchConnectionId(settings));
        }

        agent.setSettings(settings == null ? Map.of() : settings);

        AiAgent updatedAgent = agentService.update(agent);

        regenerateAndSaveWorkflow(updatedAgent);
    }

    @Override
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT')")
    public AiAgentChannel addAgentChannel(
        long agentId, String channelType, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {

        AiAgent agent = agentService.getAgent(agentId);

        validateChannelType(channelType);
        validateConnectionInAgentWorkspace(agent, connectionId);

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
    @PreAuthorize("hasPermission(#channelId, 'AiAgentChannel', 'AGENT_EDIT')")
    public void updateAgentChannel(
        long channelId, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
        AiAgentChannel channel = agentChannelService.getAgentChannel(channelId);
        AiAgent agent = agentService.getAgent(channel.getAgentId());

        validateConnectionInAgentWorkspace(agent, connectionId);

        if (parameters != null) {
            channel.setParameters(parameters);
        }

        if (connectionId != null) {
            channel.setConnectionId(connectionId);
        }

        agentChannelService.update(channel);

        regenerateAndSaveWorkflow(agent);
    }

    @Override
    @PreAuthorize("hasPermission(#channelId, 'AiAgentChannel', 'AGENT_EDIT')")
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
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT')")
    public AiAgentElement addAgentElement(
        long agentId, String kind, @Nullable Long referenceId, @Nullable Map<String, Object> parameters,
        @Nullable Long connectionId) {

        AiAgent agent = agentService.getAgent(agentId);

        validateConnectionInAgentWorkspace(agent, connectionId);

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
            validateSubAgentReference(agent, referenceId);
        }

        if (AiAgentElement.KIND_KNOWLEDGE_BASE.equals(kind)) {
            validateKnowledgeBaseInAgentWorkspace(agent, referenceId);
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
    @PreAuthorize("hasPermission(#elementId, 'AiAgentElement', 'AGENT_EDIT')")
    public void updateAgentElement(
        long elementId, @Nullable Map<String, Object> parameters, @Nullable Long connectionId) {
        AiAgentElement element = agentElementService.getAgentElement(elementId);
        AiAgent agent = agentService.getAgent(element.getAgentId());

        validateConnectionInAgentWorkspace(agent, connectionId);

        if (parameters != null) {
            element.setParameters(parameters);
        }

        if (connectionId != null) {
            element.setConnectionId(connectionId);
        }

        agentElementService.update(element);

        regenerateAndSaveWorkflow(agent);
    }

    @Override
    @PreAuthorize("hasPermission(#elementId, 'AiAgentElement', 'AGENT_EDIT')")
    public void deleteAgentElement(long elementId) {
        AiAgentElement element = agentElementService.getAgentElement(elementId);
        AiAgent agent = agentService.getAgent(element.getAgentId());

        agentElementService.delete(elementId);

        regenerateAndSaveWorkflow(agent);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public boolean isProjectPublishable(long projectId) {
        for (AiAgent agent : agentService.getProjectAgents(projectId)) {
            try {
                validateForPublish(agent, projectId);
            } catch (ConfigurationException configurationException) {
                return false;
            }
        }

        return true;
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')")
    public void prepareProjectPublish(long projectId) {
        Project project = projectService.getProject(projectId);

        for (AiAgent agent : agentService.getProjectAgents(projectId)) {
            validateForPublish(agent, projectId);

            regenerateAndSaveWorkflow(agent, projectId);

            agentVersionService.save(
                projectId, project.getLastProjectVersion(), agent.getProjectWorkflowUuid(),
                JsonUtils.write(toExportedAgent(agent, true)));
        }
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')")
    public void restoreProjectAgent(long projectId, int projectVersion, UUID projectWorkflowUuid) {
        Project project = projectService.getProject(projectId);

        Optional<AiAgentVersion> agentVersionOptional = agentVersionService.fetchAgentVersion(
            projectWorkflowUuid, projectVersion);

        AiAgentVersion agentVersion = agentVersionOptional
            .filter(curAgentVersion -> curAgentVersion.getProjectId() == projectId)
            .orElseThrow(() -> new ConfigurationException(
                "Agent workflow %s has no saved agent in project %s version %s".formatted(
                    projectWorkflowUuid, projectId, projectVersion),
                AiAgentErrorType.AGENT_VERSION_NOT_FOUND));

        Map<String, ?> snapshot = JsonUtils.readMap(agentVersion.getDefinition());

        Optional<AiAgent> projectAgent = agentService.getProjectAgents(projectId)
            .stream()
            .filter(agent -> projectWorkflowUuid.equals(agent.getProjectWorkflowUuid()))
            .findFirst();

        AiAgent restoredAgent = restoreAgentFields(
            projectAgent.orElseGet(() -> recreateAgent(project, projectWorkflowUuid, snapshot)), snapshot);

        restoreChannels(restoredAgent.getId(), exportedList(snapshot.get(EXPORT_CHANNELS)));
        restoreElements(restoredAgent, exportedList(snapshot.get(EXPORT_ELEMENTS)));

        regenerateAndSaveWorkflow(agentService.getAgent(restoredAgent.getId()), projectId);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')")
    public void restoreProjectAgents(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        Map<UUID, AiAgent> projectAgents = new LinkedHashMap<>();

        for (AiAgent projectAgent : agentService.getProjectAgents(projectId)) {
            projectAgents.put(projectAgent.getProjectWorkflowUuid(), projectAgent);
        }

        Map<AiAgent, Map<String, ?>> restoredAgents = new LinkedHashMap<>();

        for (AiAgentVersion agentVersion : agentVersionService.getProjectAgentVersions(projectId, projectVersion)) {
            Map<String, ?> snapshot = JsonUtils.readMap(agentVersion.getDefinition());

            AiAgent agent = projectAgents.remove(agentVersion.getProjectWorkflowUuid());

            if (agent == null) {
                agent = recreateAgent(project, agentVersion.getProjectWorkflowUuid(), snapshot);
            }

            restoredAgents.put(restoreAgentFields(agent, snapshot), snapshot);
        }

        for (Map.Entry<AiAgent, Map<String, ?>> restoredAgent : restoredAgents.entrySet()) {
            long agentId = restoredAgent.getKey()
                .getId();
            Map<String, ?> snapshot = restoredAgent.getValue();

            restoreChannels(agentId, exportedList(snapshot.get(EXPORT_CHANNELS)));
            restoreElements(restoredAgent.getKey(), exportedList(snapshot.get(EXPORT_ELEMENTS)));
        }

        for (AiAgent projectAgent : projectAgents.values()) {
            for (AiAgentElement element : agentElementService.getByAgentId(projectAgent.getId())) {
                if (AiAgentElement.KIND_SUB_AGENT.equals(element.getKind())) {
                    agentElementService.delete(element.getId());
                }
            }
        }

        for (AiAgent projectAgent : projectAgents.values()) {
            deleteAgent(projectAgent.getId());
        }

        for (AiAgent restoredAgent : restoredAgents.keySet()) {
            regenerateAndSaveWorkflow(agentService.getAgent(restoredAgent.getId()), projectId);
        }
    }

    private AiAgent restoreAgentFields(AiAgent agent, Map<String, ?> snapshot) {
        agent.setTitle(Objects.requireNonNull(exportedString(snapshot, EXPORT_TITLE), "title"));
        agent.setDescription(exportedString(snapshot, EXPORT_DESCRIPTION));
        agent.setInstructions(exportedString(snapshot, EXPORT_INSTRUCTIONS));
        agent.setSettings(exportedMap(snapshot.get(EXPORT_SETTINGS)));

        return agentService.update(agent);
    }

    private AiAgent recreateAgent(Project project, UUID projectWorkflowUuid, Map<String, ?> snapshot) {
        long workspaceId = Objects.requireNonNull(project.getWorkspaceId(), "workspaceId");
        String name = uniqueName(
            Objects.requireNonNull(exportedString(snapshot, EXPORT_NAME), "name"), workspaceId);
        String uuid = exportedString(snapshot, SNAPSHOT_UUID);

        AiAgent agent = new AiAgent();

        agent.setName(name);
        agent.setTitle(Objects.requireNonNull(exportedString(snapshot, EXPORT_TITLE), "title"));
        agent.setWorkspaceId(workspaceId);
        agent.setUuid(uuid == null ? UUID.randomUUID() : UUID.fromString(uuid));
        agent.setProjectWorkflowUuid(projectWorkflowUuid);

        Workflow workflow = workflowService.create(
            JsonUtils.write(Map.of("label", name, "tasks", List.of())), Workflow.Format.JSON,
            Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(
            project.getId(), project.getLastProjectVersion(), workflow.getId(), projectWorkflowUuid,
            ProjectWorkflowType.AI_AGENT);

        return agentService.create(agent);
    }

    private void restoreChannels(long agentId, List<Map<String, ?>> snapshotChannels) {
        Map<String, List<AiAgentChannel>> existingChannelsByType = agentChannelService.getByAgentId(agentId)
            .stream()
            .collect(
                Collectors.groupingBy(
                    AiAgentChannel::getChannelType, LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));

        for (Map<String, ?> snapshotChannel : snapshotChannels) {
            String channelType = exportedString(snapshotChannel, EXPORT_CHANNEL_TYPE);

            if (channelType == null || !isKnownChannelType(channelType)) {
                continue;
            }

            List<AiAgentChannel> sameTypeChannels = existingChannelsByType.getOrDefault(channelType, new ArrayList<>());

            AiAgentChannel channel = sameTypeChannels.isEmpty()
                ? new AiAgentChannel(agentId, channelType) : sameTypeChannels.removeFirst();

            channel.setConnectionId(exportedLong(snapshotChannel, SNAPSHOT_CONNECTION_ID));
            channel.setParameters(exportedMap(snapshotChannel.get(EXPORT_PARAMETERS)));
            channel.setPosition(exportedPosition(snapshotChannel));

            if (channel.getId() == null) {
                agentChannelService.create(channel);
            } else {
                agentChannelService.update(channel);
            }
        }

        for (List<AiAgentChannel> unmatchedChannels : existingChannelsByType.values()) {
            for (AiAgentChannel unmatchedChannel : unmatchedChannels) {
                if (!isPermanentChannelType(unmatchedChannel.getChannelType())) {
                    agentChannelService.delete(unmatchedChannel.getId());
                }
            }
        }
    }

    private Optional<AiAgent> fetchSnapshotSubAgent(Map<String, ?> snapshotElement, @Nullable Long referenceId) {
        String referenceUuid = exportedString(snapshotElement, SNAPSHOT_REFERENCE_UUID);

        if (referenceUuid != null) {
            return agentService.fetchAgentByUuid(UUID.fromString(referenceUuid));
        }

        return referenceId == null ? Optional.empty() : agentService.fetchAgent(referenceId);
    }

    private void restoreElements(
        AiAgent agent, List<Map<String, ?>> snapshotElements) {

        long agentId = agent.getId();

        Map<String, List<AiAgentElement>> existingElementsByKind = agentElementService.getByAgentId(agentId)
            .stream()
            .collect(
                Collectors.groupingBy(
                    AiAgentElement::getKind, LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));

        for (Map<String, ?> snapshotElement : snapshotElements) {
            String kind = exportedString(snapshotElement, EXPORT_KIND);

            if (kind == null) {
                continue;
            }

            Long referenceId = exportedLong(snapshotElement, SNAPSHOT_REFERENCE_ID);

            if (AiAgentElement.KIND_SUB_AGENT.equals(kind)) {
                Optional<AiAgent> snapshotSubAgent = fetchSnapshotSubAgent(snapshotElement, referenceId);

                Optional<AiAgent> subAgent = snapshotSubAgent
                    .filter(curSubAgent -> !Objects.equals(curSubAgent.getId(), agentId))
                    .filter(curSubAgent -> Objects.equals(curSubAgent.getWorkspaceId(), agent.getWorkspaceId()));

                if (subAgent.isEmpty()) {
                    continue;
                }

                referenceId = subAgent.get()
                    .getId();
            }

            List<AiAgentElement> sameKindElements = existingElementsByKind.getOrDefault(kind, new ArrayList<>());

            AiAgentElement element = sameKindElements.isEmpty()
                ? new AiAgentElement(agentId, kind) : sameKindElements.removeFirst();

            element.setConnectionId(exportedLong(snapshotElement, SNAPSHOT_CONNECTION_ID));
            element.setParameters(exportedMap(snapshotElement.get(EXPORT_PARAMETERS)));
            element.setPosition(exportedPosition(snapshotElement));
            element.setReferenceId(referenceId);

            if (element.getId() == null) {
                agentElementService.create(element);
            } else {
                agentElementService.update(element);
            }
        }

        for (List<AiAgentElement> unmatchedElements : existingElementsByKind.values()) {
            for (AiAgentElement unmatchedElement : unmatchedElements) {
                agentElementService.delete(unmatchedElement.getId());
            }
        }
    }

    @Override
    @PreAuthorize("hasPermission(#agentId, 'AiAgent', 'AGENT_VIEW')")
    @Transactional(readOnly = true)
    public String getDraftWorkflowId(long agentId) {
        AiAgent agent = agentService.getAgent(agentId);
        Project project = projectService.getProject(agentService.getProjectId(agent));

        return getVersionWorkflowId(agent, project.getId(), project.getLastProjectVersion());
    }

    private void validateForPublish(AiAgent agent, long projectId) {
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        long modelCount = elements.stream()
            .filter(element -> AiAgentElement.KIND_MODEL.equals(element.getKind()))
            .count();

        if (modelCount != 1) {
            throw new ConfigurationException(
                modelCount == 0
                    ? "Agent '%s' has no model selected.".formatted(getAgentLabel(agent))
                    : "Agent '%s' has %d models; select exactly one.".formatted(getAgentLabel(agent), modelCount),
                AiAgentErrorType.MODEL_MISSING);
        }

        for (AiAgentChannel channel : agentChannelService.getByAgentId(agent.getId())) {
            ResolvedAgentChannel resolvedAgentChannel = agentChannelResolver.resolve(channel.getChannelType())
                .orElseThrow(() -> new ConfigurationException(
                    "Agent '%s' has a channel of unknown type '%s'.".formatted(
                        getAgentLabel(agent), channel.getChannelType()),
                    AiAgentErrorType.UNKNOWN_CHANNEL_TYPE));

            if (resolvedAgentChannel.connectionRequired() && channel.getConnectionId() == null) {
                throw new ConfigurationException(
                    "Agent '%s' has a '%s' channel with no connection selected.".formatted(
                        getAgentLabel(agent), channel.getChannelType()),
                    AiAgentErrorType.CHANNEL_CONNECTION_MISSING);
            }

            validateRequiredChannelParameters(agent, channel, resolvedAgentChannel);
        }

        for (AiAgentElement element : elements) {
            if (!AiAgentElement.KIND_SUB_AGENT.equals(element.getKind())) {
                continue;
            }

            Long referenceId = Objects.requireNonNull(
                element.getReferenceId(), "SUB_AGENT element referenceId");

            AiAgent targetAgent = agentService.getAgent(referenceId);
            long targetProjectId = agentService.getProjectId(targetAgent);

            if (targetProjectId == projectId) {
                continue;
            }

            Project targetProject = projectService.getProject(targetProjectId);

            boolean targetHasPublishedVersion = targetProject.getProjectVersions()
                .stream()
                .anyMatch(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED);

            if (!targetHasPublishedVersion) {
                throw new ConfigurationException(
                    "Agent '%s' uses sub-agent '%s', whose project has no published version.".formatted(
                        getAgentLabel(agent), getAgentLabel(targetAgent)),
                    AiAgentErrorType.SUB_AGENT_NOT_PUBLISHED);
            }
        }

        Map<String, ?> settings = agent.getSettings();

        if (AiAgentSettings.isConnectionBackedWebSearchEnabled(settings)
            && AiAgentSettings.getWebSearchConnectionId(settings) == null) {
            throw new ConfigurationException(
                "Agent '%s' has web search enabled but no web search connection selected.".formatted(
                    getAgentLabel(agent)),
                AiAgentErrorType.BUILT_IN_TOOL_CONNECTION_MISSING);
        }

        if (AiAgentSettings.isNativeWebSearchEnabled(settings)) {
            validateNativeWebSearchSupported(agent, elements);
        }

        if (AiAgentSettings.isThinkingEnabled(settings)) {
            validateThinkingSupported(agent, elements);
        }
    }

    private void validateNativeWebSearchSupported(AiAgent agent, List<AiAgentElement> elements) {
        String provider = getModelProvider(elements);

        if (!AiAgentSettings.NATIVE_WEB_SEARCH_MODEL_PROVIDERS.contains(provider)) {
            throw new ConfigurationException(
                "Agent '" + getAgentLabel(agent) + "' uses native web search, which model provider '" + provider
                    + "' does not support — supported providers: "
                    + AiAgentSettings.NATIVE_WEB_SEARCH_MODEL_PROVIDERS,
                AiAgentErrorType.NATIVE_WEB_SEARCH_UNSUPPORTED);
        }
    }

    private void validateThinkingSupported(AiAgent agent, List<AiAgentElement> elements) {
        String provider = getModelProvider(elements);

        if (!AiAgentSettings.THINKING_MODEL_PROVIDERS.contains(provider)) {
            throw new ConfigurationException(
                "Agent '" + getAgentLabel(agent) + "' uses thinking, which model provider '" + provider
                    + "' does not support — supported providers: " + AiAgentSettings.THINKING_MODEL_PROVIDERS,
                AiAgentErrorType.THINKING_UNSUPPORTED);
        }
    }

    private static String getAgentLabel(AiAgent agent) {
        String title = agent.getTitle();

        return StringUtils.isBlank(title) ? agent.getName() : title;
    }

    private static String getModelProvider(List<AiAgentElement> elements) {
        AiAgentElement modelElement = elements.stream()
            .filter(element -> AiAgentElement.KIND_MODEL.equals(element.getKind()))
            .findFirst()
            .orElseThrow();

        return MapUtils.getString(modelElement.getParameters(), "provider", "");
    }

    private static void validateRequiredChannelParameters(
        AiAgent agent, AiAgentChannel channel, ResolvedAgentChannel resolvedAgentChannel) {

        Set<String> requiredReplyPropertyNames = resolvedAgentChannel.requiredReplyPropertyNames();

        if (requiredReplyPropertyNames.isEmpty()) {
            return;
        }

        Map<String, ?> parameters = channel.getParameters();
        ResolvedAgentChannel.Binding binding = resolvedAgentChannel.binding();

        for (Map.Entry<String, String> entry : binding.replyChannelParameters()
            .entrySet()) {

            if (!requiredReplyPropertyNames.contains(entry.getValue())) {
                continue;
            }

            if (parameters.get(entry.getKey()) == null) {
                throw new ConfigurationException(
                    "Agent '" + getAgentLabel(agent) + "' has a '" + channel.getChannelType()
                        + "' channel without parameter '" + entry.getKey()
                        + "' set, which supplies the reply action's required property '" + entry.getValue() + "'.",
                    AiAgentErrorType.CHANNEL_PARAMETER_MISSING);
            }
        }
    }

    private void regenerateAndSaveWorkflow(AiAgent agent) {
        regenerateAndSaveWorkflow(agent, agentService.getProjectId(agent));
    }

    private void regenerateAndSaveWorkflow(AiAgent agent, long projectId) {
        Project project = projectService.getProject(projectId);
        String workflowId = getVersionWorkflowId(agent, projectId, project.getLastProjectVersion());
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<AiAgentChannel> channels = agentChannelService.getByAgentId(agent.getId());
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        String definition = AiAgentWorkflowGenerator.generate(
            agent, channels, elements, this::resolveSubAgentRef, resolveCreatorUserId(agent),
            resolveChannels(channels));

        workflowService.update(workflowId, definition, workflow.getVersion());

        syncTestConnections(agent, workflowId, channels, elements);
    }

    private void syncTestConnections(
        AiAgent agent, String workflowId, List<AiAgentChannel> channels, List<AiAgentElement> elements) {

        Map<Long, Long> channelConnectionIds = channels.stream()
            .filter(channel -> channel.getConnectionId() != null)
            .collect(Collectors.toMap(AiAgentChannel::getId, AiAgentChannel::getConnectionId));

        Map<Long, Long> elementConnectionIds = elements.stream()
            .filter(element -> element.getConnectionId() != null)
            .collect(Collectors.toMap(AiAgentElement::getId, AiAgentElement::getConnectionId));

        Long webSearchConnectionId = AiAgentSettings.getWebSearchConnectionId(agent.getSettings());

        Map<AiAgentWorkflowGenerator.ConnectionRef, Long> connectionIds = new LinkedHashMap<>();

        for (AiAgentWorkflowGenerator.ConnectionRef connectionRef : AiAgentWorkflowGenerator.buildConnectionRefs(
            agent, channels, elements, resolveChannels(channels))) {

            Long connectionId = switch (connectionRef.ownerKind()) {
                case CHANNEL -> channelConnectionIds.get(connectionRef.ownerId());
                case ELEMENT -> elementConnectionIds.get(connectionRef.ownerId());
                case AGENT_SETTINGS -> webSearchConnectionId;
            };

            if (connectionId != null) {
                connectionIds.put(connectionRef, connectionId);
            }
        }

        Set<String> connectedNodeKeys = connectionIds.keySet()
            .stream()
            .map(connectionRef -> connectionRef.workflowNodeName() + "/" + connectionRef.workflowConnectionKey())
            .collect(Collectors.toSet());

        for (WorkflowTestConfiguration workflowTestConfiguration : workflowTestConfigurationService
            .getWorkflowTestConfigurations(workflowId)) {

            for (WorkflowTestConfigurationConnection workflowTestConfigurationConnection : List.copyOf(
                workflowTestConfiguration.getConnections())) {

                String workflowNodeName = workflowTestConfigurationConnection.getWorkflowNodeName();
                String workflowConnectionKey = workflowTestConfigurationConnection.getWorkflowConnectionKey();

                if (!connectedNodeKeys.contains(workflowNodeName + "/" + workflowConnectionKey)) {
                    workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(
                        workflowId, workflowNodeName, workflowConnectionKey,
                        workflowTestConfiguration.getEnvironmentId());
                }
            }
        }

        for (Map.Entry<AiAgentWorkflowGenerator.ConnectionRef, Long> entry : connectionIds.entrySet()) {
            AiAgentWorkflowGenerator.ConnectionRef connectionRef = entry.getKey();

            for (Environment environment : Environment.values()) {
                workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
                    workflowId, connectionRef.workflowNodeName(), connectionRef.workflowConnectionKey(),
                    entry.getValue(), connectionRef.trigger(), environment.ordinal());
            }
        }
    }

    private void validateKnowledgeBaseInAgentWorkspace(AiAgent agent, @Nullable Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("KNOWLEDGE_BASE element requires a referenceId");
        }

        Project project = projectService.getProject(agentService.getProjectId(agent));

        boolean inWorkspace = workspaceKnowledgeBaseService.getWorkspaceKnowledgeBases(project.getWorkspaceId())
            .stream()
            .anyMatch(
                workspaceKnowledgeBase -> Objects.equals(workspaceKnowledgeBase.getKnowledgeBaseId(), knowledgeBaseId));

        if (!inWorkspace) {
            throw new ConfigurationException(
                "Knowledge base " + knowledgeBaseId + " does not belong to the workspace of agent " + agent.getId(),
                AiAgentErrorType.REFERENCE_NOT_IN_WORKSPACE);
        }
    }

    private void validateConnectionInAgentWorkspace(AiAgent agent, @Nullable Long connectionId) {
        if (connectionId == null) {
            return;
        }

        Project project = projectService.getProject(agentService.getProjectId(agent));

        boolean inWorkspace = workspaceConnectionService.getWorkspaceConnections(project.getWorkspaceId())
            .stream()
            .anyMatch(workspaceConnection -> Objects.equals(workspaceConnection.getConnectionId(), connectionId));

        if (!inWorkspace) {
            throw new ConfigurationException(
                "Connection " + connectionId + " does not belong to the workspace of agent " + agent.getId(),
                AiAgentErrorType.CONNECTION_NOT_IN_WORKSPACE);
        }
    }

    private Function<String, ResolvedAgentChannel> resolveChannels(List<AiAgentChannel> channels) {
        Map<String, ResolvedAgentChannel> resolvedChannels = new LinkedHashMap<>();

        for (AiAgentChannel channel : channels) {
            String channelType = channel.getChannelType();

            if (!resolvedChannels.containsKey(channelType)) {
                agentChannelResolver.resolve(channelType)
                    .ifPresent(resolvedAgentChannel -> resolvedChannels.put(channelType, resolvedAgentChannel));
            }
        }

        return resolvedChannels::get;
    }

    private SubAgentRef resolveSubAgentRef(Long targetAgentId) {
        AiAgent targetAgent = agentService.getAgent(targetAgentId);

        return new SubAgentRef(targetAgent.getName(), targetAgent.getDescription(), targetAgent.getUuid());
    }

    private @Nullable Long resolveCreatorUserId(AiAgent agent) {
        String createdBy = agent.getCreatedBy();

        if (createdBy == null) {
            return null;
        }

        return userService.fetchUserByLogin(createdBy)
            .map(User::getId)
            .orElse(null);
    }

    private String getVersionWorkflowId(AiAgent agent, long projectId, int projectVersion) {
        return projectWorkflowService
            .fetchProjectWorkflow(projectId, projectVersion, String.valueOf(agent.getProjectWorkflowUuid()))
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow(() -> new IllegalStateException(
                "Agent " + agent.getId() + " has no workflow in project " + projectId + " version "
                    + projectVersion));
    }

    private AiAgentDTO toAgentDTO(AiAgent agent) {
        return toAgentDTO(agent, agentService.getProjectId(agent));
    }

    private AiAgentDTO toAgentDTO(AiAgent agent, long projectId) {
        List<AiAgentChannel> channels = agentChannelService.getByAgentId(agent.getId());
        List<AiAgentElement> elements = agentElementService.getByAgentId(agent.getId());

        Project project = projectService.getProject(projectId);
        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        boolean unpublishedChanges;
        int lastPublishedVersion;
        Instant publishedDate;

        if (lastPublishedProjectVersion == null) {
            unpublishedChanges = true;
            lastPublishedVersion = 0;
            publishedDate = null;
        } else {
            Optional<ProjectWorkflow> publishedProjectWorkflow = projectWorkflowService.fetchProjectWorkflow(
                project.getId(), lastPublishedProjectVersion.getVersion(),
                String.valueOf(agent.getProjectWorkflowUuid()));

            if (publishedProjectWorkflow.isEmpty()) {
                unpublishedChanges = true;
                lastPublishedVersion = 0;
                publishedDate = null;
            } else {
                lastPublishedVersion = lastPublishedProjectVersion.getVersion();
                publishedDate = lastPublishedProjectVersion.getPublishedDate();

                ProjectWorkflow projectWorkflow = publishedProjectWorkflow.get();

                String draftWorkflowId = getVersionWorkflowId(agent, projectId, project.getLastProjectVersion());

                unpublishedChanges = !Objects.equals(
                    workflowService.getWorkflow(draftWorkflowId)
                        .getDefinition(),
                    workflowService.getWorkflow(projectWorkflow.getWorkflowId())
                        .getDefinition());
            }
        }

        return new AiAgentDTO(
            agent, projectId, channels, elements, unpublishedChanges, lastPublishedVersion, publishedDate);
    }

    private List<ProjectAgent> visibleAgents(long workspaceId) {
        List<AiAgent> agents = agentService.getAgents(workspaceId);

        if (agents.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> projectIds = agentService.getProjectIds(agents);

        return agents.stream()
            .filter(agent -> projectIds.containsKey(agent.getId()))
            .map(agent -> new ProjectAgent(agent, projectIds.get(agent.getId())))
            .toList();
    }

    private AiAgentDeploymentDTO toAgentDeploymentDTO(
        AiAgent agent, long projectId, ProjectDeployment projectDeployment,
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {

        List<AiAgentDeploymentWorkflowDTO> agentDeploymentWorkflowDTOs = projectDeploymentWorkflows.stream()
            .map(projectDeploymentWorkflow -> toAgentDeploymentWorkflowDTO(
                projectDeployment.getId(), projectDeploymentWorkflow))
            .toList();

        return new AiAgentDeploymentDTO(
            projectDeployment.getId(), projectDeployment.getName(), agent.getId(), agent.getTitle(),
            projectId, (int) projectDeployment.getEnvironmentId(), projectDeployment.isEnabled(),
            projectDeployment.getProjectVersion(), agentDeploymentWorkflowDTOs,
            getLastExecutionDate(projectDeployment.getId(), projectDeploymentWorkflows));
    }

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

    private static boolean isPermanentChannelType(String channelType) {
        return AiAgentChannelType.CHAT.equals(channelType) || AiAgentChannelType.WORKFLOW_CALL.equals(channelType);
    }

    private void validateChannelType(String channelType) {
        resolveChannelType(channelType);
    }

    private ResolvedAgentChannel resolveChannelType(String channelType) {
        return agentChannelResolver.resolve(channelType)
            .orElseThrow(
                () -> new ConfigurationException(
                    "Unknown agent channel type: " + channelType, AiAgentErrorType.UNKNOWN_CHANNEL_TYPE));
    }

    private static boolean isSingletonKind(String kind) {
        return AiAgentElement.KIND_MODEL.equals(kind) || AiAgentElement.KIND_CHAT_MEMORY.equals(kind)
            || AiAgentElement.KIND_APPROVAL_GATE.equals(kind) || AiAgentElement.KIND_APPROVAL_TOOL.equals(kind);
    }

    private void validateSubAgentReference(AiAgent agent, @Nullable Long referenceId) {
        long agentId = agent.getId();

        if (referenceId == null) {
            throw new IllegalArgumentException("SUB_AGENT element requires a referenceId");
        }

        if (referenceId == agentId) {
            throw new ConfigurationException("Agent " + agentId + " cannot reference itself as a sub-agent",
                AiAgentErrorType.SUB_AGENT_CYCLE);
        }

        AiAgent targetAgent = agentService.getAgent(referenceId);

        if (!Objects.equals(targetAgent.getWorkspaceId(), agent.getWorkspaceId())) {
            throw new ConfigurationException(
                "Agent " + referenceId + " does not belong to the workspace of agent " + agentId,
                AiAgentErrorType.REFERENCE_NOT_IN_WORKSPACE);
        }

        if (isReachable(referenceId, agentId, new HashSet<>())) {
            throw new ConfigurationException(
                "Agent " + referenceId + " already (transitively) references agent " + agentId
                    + " as a sub-agent; adding this reference would create a cycle",
                AiAgentErrorType.SUB_AGENT_CYCLE);
        }
    }

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

    private static <T> int nextPosition(List<T> existing, ToIntFunction<T> positionExtractor) {
        return existing.stream()
            .mapToInt(positionExtractor)
            .max()
            .orElse(-1) + 1;
    }

    private record ProjectAgent(AiAgent agent, long projectId) {
    }
}
