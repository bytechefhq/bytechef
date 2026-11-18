/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.auto.memory.facade;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemory;
import com.bytechef.ee.embedded.ai.auto.memory.domain.EmbeddedAiAutoMemoryConnectedUser;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Integration-instance memories all live in the default workspace, so workspace membership cannot isolate them; every
 * method is therefore limited to tenant admins.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class EmbeddedAiAutoMemoryFacadeImpl implements EmbeddedAiAutoMemoryFacade {

    private static final String AI_AUTO_MEMORY_DELETED = "AI_AUTO_MEMORY_DELETED";
    private static final String AI_AUTO_MEMORY_UPDATED = "AI_AUTO_MEMORY_UPDATED";

    private final AiAutoMemoryService aiAutoMemoryService;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public EmbeddedAiAutoMemoryFacadeImpl(
        AiAutoMemoryService aiAutoMemoryService, ConnectedUserService connectedUserService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
    }

    @Override
    @Audited(event = AI_AUTO_MEMORY_DELETED, data = {
        @AuditData(key = "aiAutoMemoryId", value = "#id"),
        @AuditData(key = "environment", value = "#environment"),
        @AuditData(key = "principalType", value = "'INTEGRATION_INSTANCE'"),
        @AuditData(key = "principalId", value = "#integrationInstanceId")
    })
    @PreAuthorize("isTenantAdmin()")
    public void deleteEmbeddedAiAutoMemory(Environment environment, long integrationInstanceId, long id) {
        aiAutoMemoryService.deleteById(toIntegrationInstanceOwner(environment, integrationInstanceId), id);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<EmbeddedAiAutoMemory> getEmbeddedAiAutoMemories(
        Environment environment, @Nullable Long connectedUserId, @Nullable AiAutoMemoryType memoryType) {

        List<AiAutoMemory> aiAutoMemories = listIntegrationInstanceMemories(environment, memoryType);

        return toEmbeddedAiAutoMemories(aiAutoMemories)
            .stream()
            .filter(embeddedAiAutoMemory -> connectedUserId == null ||
                connectedUserId.equals(embeddedAiAutoMemory.connectedUserId()))
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<EmbeddedAiAutoMemoryConnectedUser> getEmbeddedAiAutoMemoryConnectedUsers(Environment environment) {
        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = toEmbeddedAiAutoMemories(
            listIntegrationInstanceMemories(environment, null));

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
    @Audited(event = AI_AUTO_MEMORY_UPDATED, data = {
        @AuditData(key = "aiAutoMemoryId", value = "#id"),
        @AuditData(key = "environment", value = "#environment"),
        @AuditData(key = "principalType", value = "'INTEGRATION_INSTANCE'"),
        @AuditData(key = "principalId", value = "#integrationInstanceId")
    })
    @PreAuthorize("isTenantAdmin()")
    public EmbeddedAiAutoMemory updateEmbeddedAiAutoMemory(
        Environment environment, long integrationInstanceId, long id, long expectedVersion,
        AiAutoMemoryPatch patch) {

        AiAutoMemory updatedAiAutoMemory = aiAutoMemoryService.updateById(
            toIntegrationInstanceOwner(environment, integrationInstanceId), id, expectedVersion, patch);

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = toEmbeddedAiAutoMemories(List.of(updatedAiAutoMemory));

        return embeddedAiAutoMemories.getFirst();
    }

    /**
     * The service resolves a memory only within this exact owner, so a row of another principal type, another instance
     * or another environment is not found.
     */
    private static AiAutoMemoryOwner toIntegrationInstanceOwner(Environment environment, long integrationInstanceId) {
        return new AiAutoMemoryOwner(
            Workspace.DEFAULT_WORKSPACE_ID, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, integrationInstanceId,
            environment);
    }

    private List<AiAutoMemory> listIntegrationInstanceMemories(
        Environment environment, @Nullable AiAutoMemoryType memoryType) {

        return aiAutoMemoryService.listAllOwners(Workspace.DEFAULT_WORKSPACE_ID, environment, memoryType)
            .stream()
            .filter(aiAutoMemory -> aiAutoMemory.getPrincipalType() == AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE)
            .toList();
    }

    private List<EmbeddedAiAutoMemory> toEmbeddedAiAutoMemories(List<AiAutoMemory> aiAutoMemories) {
        if (aiAutoMemories.isEmpty()) {
            return List.of();
        }

        List<Long> integrationInstanceIds = aiAutoMemories.stream()
            .map(AiAutoMemory::getPrincipalId)
            .distinct()
            .toList();

        Map<Long, IntegrationInstance> integrationInstances = integrationInstanceService
            .getIntegrationInstances(integrationInstanceIds)
            .stream()
            .collect(Collectors.toMap(IntegrationInstance::getId, Function.identity()));

        Map<Long, ConnectedUser> connectedUsers = connectedUserService
            .getConnectedUsers(
                integrationInstances.values()
                    .stream()
                    .map(IntegrationInstance::getConnectedUserId)
                    .distinct()
                    .toList())
            .stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));

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

        List<EmbeddedAiAutoMemory> embeddedAiAutoMemories = new ArrayList<>();

        for (AiAutoMemory aiAutoMemory : aiAutoMemories) {
            IntegrationInstance integrationInstance = integrationInstances.get(aiAutoMemory.getPrincipalId());

            ConnectedUser connectedUser = null;
            Integration integration = null;

            if (integrationInstance != null) {
                connectedUser = connectedUsers.get(integrationInstance.getConnectedUserId());

                IntegrationInstanceConfiguration integrationInstanceConfiguration =
                    integrationInstanceConfigurations.get(integrationInstance.getIntegrationInstanceConfigurationId());

                if (integrationInstanceConfiguration != null) {
                    integration = integrations.get(integrationInstanceConfiguration.getIntegrationId());
                }
            }

            embeddedAiAutoMemories.add(
                new EmbeddedAiAutoMemory(
                    aiAutoMemory, connectedUser == null ? null : connectedUser.getId(),
                    connectedUser == null ? null : connectedUser.getName(),
                    connectedUser == null ? null : connectedUser.getEmail(),
                    connectedUser == null ? null : connectedUser.getExternalId(),
                    integration == null ? null : integration.getId(),
                    integration == null ? null : integration.getName(),
                    integration == null ? null : integration.getComponentName()));
        }

        return embeddedAiAutoMemories;
    }
}
