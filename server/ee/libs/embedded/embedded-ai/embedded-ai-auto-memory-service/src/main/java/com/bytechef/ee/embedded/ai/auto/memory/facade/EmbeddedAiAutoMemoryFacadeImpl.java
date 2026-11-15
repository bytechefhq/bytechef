/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemory;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemoryConnectedUser;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryAuditEvents;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryNotFoundException;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Embedded memories all live in the default workspace: integration instances are stored there, and so is every Embedded
 * automation project and connected user project. Workspace membership therefore cannot isolate them, and every method
 * is limited to tenant admins.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class EmbeddedAiAutoMemoryFacadeImpl implements EmbeddedAiAutoMemoryFacade {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final ProjectOwnershipService projectOwnershipService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public EmbeddedAiAutoMemoryFacadeImpl(
        AiAutoMemoryService aiAutoMemoryService, ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService,
        ConnectedUserService connectedUserService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        ProjectOwnershipService projectOwnershipService, ProjectService projectService) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserReferenceDeploymentService = connectedUserReferenceDeploymentService;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.projectOwnershipService = projectOwnershipService;
        this.projectService = projectService;
    }

    @Override
    @Audited(event = AiAutoMemoryAuditEvents.AI_AUTO_MEMORY_DELETED, data = {
        @AuditData(key = "aiAutoMemoryId", value = "#id"),
        @AuditData(key = "environment", value = "#environment"),
        @AuditData(key = "principalType", value = "#principalType"),
        @AuditData(key = "principalId", value = "#principalId")
    })
    @PreAuthorize("hasEmbeddedScopeInEnvironment('CONNECTED_USER_MANAGE', #environment)")
    public void deleteEmbeddedAiAutoMemory(
        Environment environment, AiAutoMemoryPrincipalType principalType, long principalId, long id) {

        aiAutoMemoryService.deleteById(toEmbeddedOwner(environment, principalType, principalId), id);
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironment('CONNECTED_USER_VIEW', #environment)")
    public List<EmbeddedAiAutoMemory> getEmbeddedAiAutoMemories(
        Environment environment, @Nullable Long connectedUserId, @Nullable AiAutoMemoryType memoryType) {

        List<AiAutoMemory> aiAutoMemories = listEmbeddedMemories(environment, memoryType);

        return toEmbeddedAiAutoMemories(aiAutoMemories)
            .stream()
            .filter(embeddedAiAutoMemory -> connectedUserId == null ||
                connectedUserId.equals(embeddedAiAutoMemory.connectedUserId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironment('CONNECTED_USER_VIEW', #environment)")
    public List<EmbeddedAiAutoMemoryConnectedUser> getEmbeddedAiAutoMemoryConnectedUsers(Environment environment) {
        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = toEmbeddedAiAutoMemories(
            listEmbeddedMemories(environment, null));

        Map<Long, EmbeddedAiAutoMemoryConnectedUser> connectedUsers = new LinkedHashMap<>();

        for (EmbeddedAiAutoMemory embeddedAiAutoMemory : embeddedAiAutoMemories) {
            Long connectedUserId = embeddedAiAutoMemory.connectedUserId();

            if (connectedUserId == null) {
                continue;
            }

            EmbeddedAiAutoMemoryConnectedUser connectedUser = connectedUsers.get(connectedUserId);

            int memoryCount = connectedUser == null ? 1 : connectedUser.memoryCount() + 1;

            connectedUsers.put(
                connectedUserId,
                new EmbeddedAiAutoMemoryConnectedUser(
                    connectedUserId, embeddedAiAutoMemory.connectedUserName(),
                    embeddedAiAutoMemory.connectedUserEmail(),
                    Objects.requireNonNull(embeddedAiAutoMemory.connectedUserExternalId()), memoryCount));
        }

        return new ArrayList<>(connectedUsers.values());
    }

    @Override
    @Audited(event = AiAutoMemoryAuditEvents.AI_AUTO_MEMORY_UPDATED, data = {
        @AuditData(key = "aiAutoMemoryId", value = "#id"),
        @AuditData(key = "environment", value = "#environment"),
        @AuditData(key = "principalType", value = "#principalType"),
        @AuditData(key = "principalId", value = "#principalId")
    })
    @PreAuthorize("hasEmbeddedScopeInEnvironment('CONNECTED_USER_MANAGE', #environment)")
    public EmbeddedAiAutoMemory updateEmbeddedAiAutoMemory(
        Environment environment, AiAutoMemoryPrincipalType principalType, long principalId, long id,
        long expectedVersion, AiAutoMemoryPatch patch) {

        AiAutoMemory updatedAiAutoMemory = aiAutoMemoryService.updateById(
            toEmbeddedOwner(environment, principalType, principalId), id, expectedVersion, patch);

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = toEmbeddedAiAutoMemories(List.of(updatedAiAutoMemory));

        return embeddedAiAutoMemories.getFirst();
    }

    private Set<Long> getEmbeddedProjectDeploymentIds(List<Long> projectDeploymentIds) {
        return projectOwnershipService.getOwnedProjectDeploymentIds(
            projectDeploymentIds, ProjectDeploymentOwner.EMBEDDED);
    }

    private List<AiAutoMemory> listEmbeddedMemories(Environment environment, @Nullable AiAutoMemoryType memoryType) {
        List<AiAutoMemory> aiAutoMemories = aiAutoMemoryService.listAllOwners(
            Workspace.DEFAULT_WORKSPACE_ID, environment, memoryType);

        Set<Long> embeddedProjectDeploymentIds = getEmbeddedProjectDeploymentIds(
            aiAutoMemories.stream()
                .filter(aiAutoMemory -> aiAutoMemory.getPrincipalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT)
                .map(AiAutoMemory::getPrincipalId)
                .distinct()
                .toList());

        return aiAutoMemories.stream()
            .filter(aiAutoMemory -> switch (aiAutoMemory.getPrincipalType()) {
                case INTEGRATION_INSTANCE -> true;
                case PROJECT_DEPLOYMENT -> embeddedProjectDeploymentIds.contains(aiAutoMemory.getPrincipalId());
                case USER -> false;
            })
            .toList();
    }

    /**
     * The service resolves a memory only within this exact owner, so a row of another principal type, another owner or
     * another environment is not found. A deployment Embedded does not own, such as a workspace's own automation, and a
     * user owner are reported as not found as well, so this API cannot reach Automation memories.
     */
    private AiAutoMemoryOwner toEmbeddedOwner(
        Environment environment, AiAutoMemoryPrincipalType principalType, long principalId) {

        boolean embeddedOwner = switch (principalType) {
            case INTEGRATION_INSTANCE -> true;
            case PROJECT_DEPLOYMENT -> {
                Set<Long> embeddedProjectDeploymentIds = getEmbeddedProjectDeploymentIds(List.of(principalId));

                yield embeddedProjectDeploymentIds.contains(principalId);
            }
            case USER -> false;
        };

        if (!embeddedOwner) {
            throw new AiAutoMemoryNotFoundException(
                "No Embedded memory owner " + principalType + " " + principalId + " exists");
        }

        return new AiAutoMemoryOwner(Workspace.DEFAULT_WORKSPACE_ID, principalType, principalId, environment);
    }

    private List<EmbeddedAiAutoMemory> toEmbeddedAiAutoMemories(List<AiAutoMemory> aiAutoMemories) {
        if (aiAutoMemories.isEmpty()) {
            return List.of();
        }

        Map<Long, IntegrationInstance> integrationInstances = getIntegrationInstances(aiAutoMemories);
        Map<Long, DeploymentSource> deploymentSources = getDeploymentSources(aiAutoMemories);

        List<Long> connectedUserIds = new ArrayList<>();

        integrationInstances.values()
            .forEach(integrationInstance -> connectedUserIds.add(integrationInstance.getConnectedUserId()));
        deploymentSources.values()
            .forEach(deploymentSource -> connectedUserIds.add(deploymentSource.connectedUserId()));

        Map<Long, ConnectedUser> connectedUsers = connectedUserService
            .getConnectedUsers(
                connectedUserIds.stream()
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));

        Map<Long, Integration> integrationsByInstanceId = getIntegrationsByInstanceId(integrationInstances);
        Map<Long, Project> automationProjects = getAutomationProjects(deploymentSources);

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = new ArrayList<>();

        for (AiAutoMemory aiAutoMemory : aiAutoMemories) {
            Long connectedUserId = null;
            Integration integration = null;
            Project automationProject = null;

            if (aiAutoMemory.getPrincipalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT) {
                DeploymentSource deploymentSource = deploymentSources.get(aiAutoMemory.getPrincipalId());

                if (deploymentSource != null) {
                    connectedUserId = deploymentSource.connectedUserId();

                    if (deploymentSource.automationProjectId() != null) {
                        automationProject = automationProjects.get(deploymentSource.automationProjectId());
                    }
                }
            } else {
                IntegrationInstance integrationInstance = integrationInstances.get(aiAutoMemory.getPrincipalId());

                if (integrationInstance != null) {
                    connectedUserId = integrationInstance.getConnectedUserId();
                    integration = integrationsByInstanceId.get(integrationInstance.getId());
                }
            }

            ConnectedUser connectedUser = connectedUserId == null ? null : connectedUsers.get(connectedUserId);

            embeddedAiAutoMemories.add(
                new EmbeddedAiAutoMemory(
                    aiAutoMemory, connectedUser == null ? null : connectedUser.getId(),
                    connectedUser == null ? null : connectedUser.getName(),
                    connectedUser == null ? null : connectedUser.getEmail(),
                    connectedUser == null ? null : connectedUser.getExternalId(),
                    integration == null ? null : integration.getId(),
                    integration == null ? null : integration.getName(),
                    integration == null ? null : integration.getComponentName(),
                    automationProject == null ? null : automationProject.getId(),
                    automationProject == null ? null : automationProject.getName()));
        }

        return embeddedAiAutoMemories;
    }

    private Map<Long, Project> getAutomationProjects(Map<Long, DeploymentSource> deploymentSources) {
        List<Long> automationProjectIds = deploymentSources.values()
            .stream()
            .map(DeploymentSource::automationProjectId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (automationProjectIds.isEmpty()) {
            return Map.of();
        }

        return projectService.getProjects(automationProjectIds)
            .stream()
            .collect(Collectors.toMap(Project::getId, Function.identity()));
    }

    /**
     * A deployment belongs to a connected user either as the reference deployment of an automation the user enabled, or
     * as the deployment of the user's own project, which holds the workflows they built themselves.
     */
    private Map<Long, DeploymentSource> getDeploymentSources(List<AiAutoMemory> aiAutoMemories) {
        Map<Long, DeploymentSource> deploymentSources = new HashMap<>();

        List<Long> projectDeploymentIds = aiAutoMemories.stream()
            .filter(aiAutoMemory -> aiAutoMemory.getPrincipalType() == AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT)
            .map(AiAutoMemory::getPrincipalId)
            .distinct()
            .toList();

        for (Long projectDeploymentId : projectDeploymentIds) {
            Optional<ConnectedUserReferenceDeployment> referenceDeployment =
                connectedUserReferenceDeploymentService.fetchByProjectDeploymentId(projectDeploymentId);

            if (referenceDeployment.isPresent()) {
                ConnectedUserReferenceDeployment connectedUserReferenceDeployment = referenceDeployment.get();

                deploymentSources.put(
                    projectDeploymentId,
                    new DeploymentSource(
                        connectedUserReferenceDeployment.getConnectedUserId(),
                        connectedUserReferenceDeployment.getProjectId()));

                continue;
            }

            Optional<Long> connectedUserId = connectedUserProjectService.fetchConnectedUserId(projectDeploymentId);

            connectedUserId.ifPresent(
                id -> deploymentSources.put(projectDeploymentId, new DeploymentSource(id, null)));
        }

        return deploymentSources;
    }

    private Map<Long, IntegrationInstance> getIntegrationInstances(List<AiAutoMemory> aiAutoMemories) {
        List<Long> integrationInstanceIds = aiAutoMemories.stream()
            .filter(aiAutoMemory -> aiAutoMemory.getPrincipalType() == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE)
            .map(AiAutoMemory::getPrincipalId)
            .distinct()
            .toList();

        if (integrationInstanceIds.isEmpty()) {
            return Map.of();
        }

        return integrationInstanceService.getIntegrationInstances(integrationInstanceIds)
            .stream()
            .collect(Collectors.toMap(IntegrationInstance::getId, Function.identity()));
    }

    private Map<Long, Integration> getIntegrationsByInstanceId(Map<Long, IntegrationInstance> integrationInstances) {
        if (integrationInstances.isEmpty()) {
            return Map.of();
        }

        Map<Long, IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService
                .getIntegrationInstanceConfigurations(
                    integrationInstances.values()
                        .stream()
                        .map(IntegrationInstance::getIntegrationInstanceConfigurationId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(IntegrationInstanceConfiguration::getId, Function.identity()));

        Map<Long, Integration> integrations = integrationService
            .getIntegrations(
                integrationInstanceConfigurations.values()
                    .stream()
                    .map(IntegrationInstanceConfiguration::getIntegrationId)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(Integration::getId, Function.identity()));

        Map<Long, Integration> integrationsByInstanceId = new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstances.values()) {
            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurations.get(integrationInstance.getIntegrationInstanceConfigurationId());

            if (integrationInstanceConfiguration == null) {
                continue;
            }

            Integration integration = integrations.get(integrationInstanceConfiguration.getIntegrationId());

            if (integration != null) {
                integrationsByInstanceId.put(integrationInstance.getId(), integration);
            }
        }

        return integrationsByInstanceId;
    }

    /**
     * @param automationProjectId the enabled automation's project, or null for the connected user's own workflows
     */
    private record DeploymentSource(long connectedUserId, @Nullable Long automationProjectId) {
    }
}
