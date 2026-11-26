/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.contributor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.automation.configuration.promotion.connection.PromotionConnectionScope;
import com.bytechef.ee.automation.configuration.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.configuration.promotion.deployment.PromotionPreviews;
import com.bytechef.ee.automation.configuration.promotion.deployment.WorkflowDefinitions;
import com.bytechef.ee.automation.promotion.contributor.EnvironmentPromotionContributor;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.ee.automation.promotion.util.PromotionChangeHashes;
import com.bytechef.ee.automation.promotion.util.PromotionEnvironmentUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.promotion.EmbeddedConnectionCandidates;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.configuration.util.InternalOnlyInputUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationPromotionContributor implements EnvironmentPromotionContributor {

    public static final String RESOURCE_TYPE = "INTEGRATION_INSTANCE_CONFIGURATION";

    private static final String ASSET_TYPE = "CONFIGURATION_WORKFLOW";

    private final ConnectionService connectionService;
    private final EmbeddedConnectionCandidates embeddedConnectionCandidates;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final TagService tagService;
    private final WorkflowService workflowService;

    private record NodeBindingKey(String workflowUuid, String nodeName, String key) {
    }

    @SuppressFBWarnings("EI")
    public IntegrationInstanceConfigurationPromotionContributor(
        ConnectionService connectionService, EmbeddedConnectionCandidates embeddedConnectionCandidates,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService, TagService tagService,
        WorkflowService workflowService) {

        this.connectionService = connectionService;
        this.embeddedConnectionCandidates = embeddedConnectionCandidates;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.EMBEDDED;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public EnvironmentPromotionPreview preview(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        IntegrationInstanceConfiguration source = loadSource(sourceId, sourceEnvironment, targetEnvironment);

        getIntegrationVersion(source);

        Map<String, String> workflowUuidsByWorkflowId = workflowUuidsByWorkflowId(source);
        List<IntegrationInstanceConfigurationWorkflow> sourceWorkflows =
            configurationWorkflows(source, workflowUuidsByWorkflowId);

        Optional<IntegrationInstanceConfiguration> existingTarget = fetchTarget(source, targetEnvironment);

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid = existingTarget
            .map(target -> configurationWorkflowsByUuid(target, workflowUuidsByWorkflowId))
            .orElse(Map.of());

        List<SourceBinding> sourceBindings = sourceBindings(sourceWorkflows, workflowUuidsByWorkflowId);

        List<PromotionChange> changes = predictChanges(
            source, sourceWorkflows, workflowUuidsByWorkflowId, existingTarget.orElse(null));

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            sourceBindings, existingTargetBindings(sourceBindings, targetWorkflowsByUuid), connectionService,
            connection -> embeddedConnectionCandidates.candidates(connection.getComponentName(), targetEnvironment));

        String environmentName = PromotionEnvironmentUtils.displayName(targetEnvironment);
        List<String> warnings = new ArrayList<>();

        if (existingTarget.isEmpty()) {
            warnings.add(
                "The promoted integration instance configuration is created disabled with empty connection " +
                    "settings; enter them in %s before enabling it.".formatted(environmentName));
        }

        warnings.addAll(
            inputWarnings(
                sourceWorkflows, workflowUuidsByWorkflowId, targetWorkflowsByUuid, environmentName));

        warnings.addAll(EmbeddedPromotionSupport.unresolvedConnectionWarnings(connectionMappings, environmentName));

        return new EnvironmentPromotionPreview(
            RESOURCE_TYPE, sourceId, sourceEnvironment, targetEnvironment,
            existingTarget.map(IntegrationInstanceConfiguration::getId)
                .orElse(null),
            existingTarget.map(IntegrationInstanceConfiguration::getName)
                .orElse(null),
            List.of(), connectionMappings, warnings, List.of(), changes,
            PromotionChangeHashes.destructiveChangesHash(changes));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional
    public EnvironmentPromotionResult promote(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment,
        Map<Long, Long> connectionMappings, @Nullable String expectedDestructiveChangesHash) {

        IntegrationInstanceConfiguration source = loadSource(sourceId, sourceEnvironment, targetEnvironment);

        int integrationVersion = getIntegrationVersion(source);

        Map<String, String> workflowUuidsByWorkflowId = workflowUuidsByWorkflowId(source);
        List<IntegrationInstanceConfigurationWorkflow> sourceWorkflows =
            configurationWorkflows(source, workflowUuidsByWorkflowId);
        List<SourceBinding> sourceBindings = sourceBindings(sourceWorkflows, workflowUuidsByWorkflowId);

        PromotionConnectionScope.checkMappedConnectionsBelongToSource(
            PromotionConnectionScope.sourceConnectionIds(sourceBindings), connectionMappings);

        EmbeddedPromotionSupport.checkMappedConnectionsAreCandidates(
            connectionMappings, targetEnvironment, connectionService, embeddedConnectionCandidates);

        Map<Long, Long> suggestedMappings = EmbeddedPromotionSupport.suggestedConnectionMappings(
            PromotionConnectionScope.sourceConnectionIds(sourceBindings), targetEnvironment, connectionService,
            embeddedConnectionCandidates);

        Optional<IntegrationInstanceConfiguration> existingTarget = fetchTarget(source, targetEnvironment);

        List<PromotionChange> changes = predictChanges(
            source, sourceWorkflows, workflowUuidsByWorkflowId, existingTarget.orElse(null));

        PromotionChangeHashes.checkNotStale(
            expectedDestructiveChangesHash, PromotionChangeHashes.destructiveChangesHash(changes));

        boolean created = existingTarget.isEmpty();

        IntegrationInstanceConfiguration target = existingTarget.orElseGet(
            () -> createTarget(source, targetEnvironment));

        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid =
            created ? Map.of() : configurationWorkflowsByUuid(target, workflowUuidsByWorkflowId);

        long targetId = Objects.requireNonNull(target.getId(), "id");
        Set<Long> unresolvedConnectionIds = new LinkedHashSet<>();
        List<IntegrationInstanceConfigurationWorkflowDTO> workflowDTOs = new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflow sourceWorkflow : sourceWorkflows) {
            String workflowUuid = workflowUuidsByWorkflowId.get(sourceWorkflow.getWorkflowId());

            IntegrationInstanceConfigurationWorkflow targetWorkflow = targetWorkflowsByUuid.get(workflowUuid);

            IntegrationInstanceConfigurationWorkflow promotedWorkflow = promotedWorkflow(
                targetId, sourceWorkflow, workflowUuid, targetWorkflow, connectionMappings, suggestedMappings,
                unresolvedConnectionIds);

            if (EmbeddedPromotionSupport.isCreatedDisabled(
                targetWorkflow == null, promotedWorkflow.isEnabled(), missingInternalOnlyInputName(promotedWorkflow))) {

                promotedWorkflow.setEnabled(false);
            }

            workflowDTOs
                .add(new IntegrationInstanceConfigurationWorkflowDTO(promotedWorkflow, null, null, workflowUuid));
        }

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfiguration(
            updatedConfiguration(source, integrationVersion, target, workflowDTOs));

        return new EnvironmentPromotionResult(targetId, created, null, new ArrayList<>(unresolvedConnectionIds));
    }

    private static void checkSourceEnvironment(
        long sourceId, Environment rowEnvironment, Environment sourceEnvironment) {

        if (rowEnvironment != sourceEnvironment) {
            throw new ConfigurationException(
                "Integration instance configuration id=%s is in %s, not in %s".formatted(
                    sourceId, rowEnvironment, sourceEnvironment),
                EnvironmentPromotionErrorType.SOURCE_ENVIRONMENT_MISMATCH);
        }
    }

    private List<IntegrationInstanceConfigurationWorkflow> configurationWorkflows(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        Map<String, String> workflowUuidsByWorkflowId) {

        Set<String> versionWorkflowIds = versionWorkflowIds(integrationInstanceConfiguration);

        List<IntegrationInstanceConfigurationWorkflow> configurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                Objects.requireNonNull(integrationInstanceConfiguration.getId(), "id"));

        return configurationWorkflows.stream()
            .filter(configurationWorkflow -> versionWorkflowIds.contains(configurationWorkflow.getWorkflowId()))
            .filter(configurationWorkflow -> workflowUuidsByWorkflowId.containsKey(
                configurationWorkflow.getWorkflowId()))
            .toList();
    }

    private Map<String, IntegrationInstanceConfigurationWorkflow> configurationWorkflowsByUuid(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        Map<String, String> workflowUuidsByWorkflowId) {

        Map<String, IntegrationInstanceConfigurationWorkflow> configurationWorkflowsByUuid = new HashMap<>();

        for (IntegrationInstanceConfigurationWorkflow configurationWorkflow : configurationWorkflows(
            integrationInstanceConfiguration, workflowUuidsByWorkflowId)) {

            configurationWorkflowsByUuid.put(
                workflowUuidsByWorkflowId.get(configurationWorkflow.getWorkflowId()), configurationWorkflow);
        }

        return configurationWorkflowsByUuid;
    }

    private IntegrationInstanceConfiguration createTarget(
        IntegrationInstanceConfiguration source, Environment targetEnvironment) {

        IntegrationInstanceConfiguration counterpart = new IntegrationInstanceConfiguration();

        counterpart.setAuthorizationType(source.getAuthorizationType());
        counterpart.setDescription(source.getDescription());
        counterpart.setEnabled(false);
        counterpart.setEnvironment(targetEnvironment);
        counterpart.setIntegrationId(source.getIntegrationId());
        counterpart.setIntegrationVersion(source.getIntegrationVersion());
        counterpart.setName(source.getName());
        counterpart.setPermissionExpression(source.getPermissionExpression());
        counterpart.setTagIds(source.getTagIds());
        counterpart.setUuid(source.getUuid());

        return integrationInstanceConfigurationService.create(counterpart);
    }

    private static Map<Long, Long> existingTargetBindings(
        List<SourceBinding> sourceBindings,
        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid) {

        Map<NodeBindingKey, Long> targetConnectionIdsByNode = targetConnectionIdsByNode(targetWorkflowsByUuid);
        Map<Long, Long> existingTargetBindings = new HashMap<>();

        for (SourceBinding sourceBinding : sourceBindings) {
            Long targetConnectionId = targetConnectionIdsByNode.get(
                new NodeBindingKey(
                    Objects.requireNonNull(sourceBinding.workflowUuid()), sourceBinding.nodeName(),
                    sourceBinding.key()));

            if (targetConnectionId != null) {
                existingTargetBindings.putIfAbsent(sourceBinding.connectionId(), targetConnectionId);
            }
        }

        return existingTargetBindings;
    }

    private Optional<IntegrationInstanceConfiguration> fetchTarget(
        IntegrationInstanceConfiguration source, Environment targetEnvironment) {

        return integrationInstanceConfigurationService.fetchIntegrationInstanceConfiguration(
            Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment);
    }

    private static int getIntegrationVersion(IntegrationInstanceConfiguration source) {
        Integer integrationVersion = source.getIntegrationVersion();

        if (integrationVersion == null) {
            throw new ConfigurationException(
                "Integration instance configuration id=%s has no integration version, so it cannot be promoted"
                    .formatted(source.getId()),
                EnvironmentPromotionErrorType.SOURCE_VERSION_MISSING);
        }

        return integrationVersion;
    }

    private List<String> inputWarnings(
        List<IntegrationInstanceConfigurationWorkflow> sourceWorkflows, Map<String, String> workflowUuidsByWorkflowId,
        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid, String environmentName) {

        List<String> warnings = new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflow sourceWorkflow : sourceWorkflows) {
            IntegrationInstanceConfigurationWorkflow targetWorkflow = targetWorkflowsByUuid.get(
                workflowUuidsByWorkflowId.get(sourceWorkflow.getWorkflowId()));

            Map<String, ?> keptInputs = targetWorkflow == null ? Map.of() : targetWorkflow.getInputs();

            Workflow workflow = workflowService.getWorkflow(sourceWorkflow.getWorkflowId());

            for (Workflow.Input input : workflow.getInputs()) {
                if (!keptInputs.containsKey(input.name())) {
                    warnings.add(
                        "Workflow \"%s\" input \"%s\" needs a value in %s.".formatted(
                            EmbeddedPromotionSupport.workflowLabel(workflow), input.name(), environmentName));
                }
            }

            boolean enabled = targetWorkflow == null ? sourceWorkflow.isEnabled() : targetWorkflow.isEnabled();
            String missingInternalOnlyInputName = InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(
                workflow, keptInputs);

            String internalOnlyInputWarning = EmbeddedPromotionSupport.internalOnlyInputWarning(
                workflow, targetWorkflow == null, enabled, missingInternalOnlyInputName, environmentName);

            if (internalOnlyInputWarning != null) {
                warnings.add(internalOnlyInputWarning);
            }
        }

        return warnings;
    }

    private @Nullable String missingInternalOnlyInputName(
        IntegrationInstanceConfigurationWorkflow configurationWorkflow) {

        Workflow workflow = workflowService.getWorkflow(configurationWorkflow.getWorkflowId());

        return InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(workflow, configurationWorkflow.getInputs());
    }

    private IntegrationInstanceConfiguration loadSource(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        IntegrationInstanceConfiguration source =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(sourceId);

        checkSourceEnvironment(sourceId, source.getEnvironment(), sourceEnvironment);

        if (source.getEnvironment() == targetEnvironment) {
            throw new ConfigurationException(
                "Integration instance configuration id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    private Map<String, IntegrationInstanceConfigurationWorkflow> allConfigurationWorkflowsByUuid(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        Map<String, String> workflowUuidsByWorkflowId) {

        Map<String, IntegrationInstanceConfigurationWorkflow> configurationWorkflowsByUuid = new LinkedHashMap<>();

        for (IntegrationInstanceConfigurationWorkflow configurationWorkflow : integrationInstanceConfigurationWorkflowService
            .getIntegrationInstanceConfigurationWorkflows(
                Objects.requireNonNull(integrationInstanceConfiguration.getId(), "id"))) {

            String workflowUuid = workflowUuidsByWorkflowId.get(configurationWorkflow.getWorkflowId());

            if (workflowUuid != null) {
                configurationWorkflowsByUuid.put(workflowUuid, configurationWorkflow);
            }
        }

        return configurationWorkflowsByUuid;
    }

    private Map<Long, Long> connectedUserCountsByConfigurationWorkflowId(long configurationId) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
            configurationId);

        if (integrationInstances.isEmpty()) {
            return Map.of();
        }

        List<Long> integrationInstanceIds = integrationInstances.stream()
            .map(IntegrationInstance::getId)
            .toList();

        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows =
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceIds);

        return integrationInstanceWorkflows.stream()
            .collect(
                Collectors.groupingBy(
                    IntegrationInstanceWorkflow::getIntegrationInstanceConfigurationWorkflowId,
                    Collectors.counting()));
    }

    private List<PromotionChange> predictChanges(
        IntegrationInstanceConfiguration source, List<IntegrationInstanceConfigurationWorkflow> sourceWorkflows,
        Map<String, String> workflowUuidsByWorkflowId, @Nullable IntegrationInstanceConfiguration existingTarget) {

        String parentName = source.getName();
        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid = existingTarget == null
            ? Map.of() : allConfigurationWorkflowsByUuid(existingTarget, workflowUuidsByWorkflowId);
        List<PromotionChange> changes = new ArrayList<>();
        Set<String> sourceWorkflowUuids = new LinkedHashSet<>();

        if (existingTarget != null && overwritesDifferingField(source, existingTarget)) {
            changes.add(
                new PromotionChange(
                    Kind.UPDATE, RESOURCE_TYPE, String.valueOf(existingTarget.getUuid()), null, parentName,
                    false));
        }

        for (IntegrationInstanceConfigurationWorkflow sourceWorkflow : sourceWorkflows) {
            String workflowUuid = workflowUuidsByWorkflowId.get(sourceWorkflow.getWorkflowId());

            sourceWorkflowUuids.add(workflowUuid);

            Workflow workflow = workflowService.getWorkflow(sourceWorkflow.getWorkflowId());
            IntegrationInstanceConfigurationWorkflow targetWorkflow = targetWorkflowsByUuid.get(workflowUuid);

            if (targetWorkflow == null) {
                changes.add(
                    new PromotionChange(
                        Kind.CREATE, ASSET_TYPE, workflowUuid, parentName, EmbeddedPromotionSupport.workflowLabel(
                            workflow),
                        false));
            } else if (!WorkflowDefinitions.sameDefinition(
                workflow, workflowService.getWorkflow(targetWorkflow.getWorkflowId()))) {

                changes.add(
                    new PromotionChange(
                        Kind.UPDATE, ASSET_TYPE, workflowUuid, parentName, EmbeddedPromotionSupport.workflowLabel(
                            workflow),
                        false));
            }
        }

        if (existingTarget == null) {
            return changes;
        }

        Map<Long, Long> connectedUserCounts = connectedUserCountsByConfigurationWorkflowId(
            Objects.requireNonNull(existingTarget.getId(), "id"));

        for (Map.Entry<String, IntegrationInstanceConfigurationWorkflow> entry : targetWorkflowsByUuid.entrySet()) {
            if (sourceWorkflowUuids.contains(entry.getKey())) {
                continue;
            }

            IntegrationInstanceConfigurationWorkflow targetWorkflow = entry.getValue();

            Workflow workflow = workflowService.getWorkflow(targetWorkflow.getWorkflowId());

            long connectedUserCount = connectedUserCounts.getOrDefault(
                Objects.requireNonNull(targetWorkflow.getId(), "id"), 0L);

            changes.add(
                new PromotionChange(
                    Kind.DELETE, ASSET_TYPE, entry.getKey(), parentName,
                    "%s (removes it from %s connected users)".formatted(
                        EmbeddedPromotionSupport.workflowLabel(workflow), connectedUserCount),
                    true));
        }

        return changes;
    }

    private static boolean overwritesDifferingField(
        IntegrationInstanceConfiguration source, IntegrationInstanceConfiguration target) {

        return !Objects.equals(source.getName(), target.getName()) ||
            !Objects.equals(source.getDescription(), target.getDescription()) ||
            !Objects.equals(source.getPermissionExpression(), target.getPermissionExpression()) ||
            !Objects.equals(source.getIntegrationVersion(), target.getIntegrationVersion()) ||
            !Objects.equals(new HashSet<>(source.getTagIds()), new HashSet<>(target.getTagIds()));
    }

    private static IntegrationInstanceConfigurationWorkflow promotedWorkflow(
        long targetId, IntegrationInstanceConfigurationWorkflow sourceWorkflow, String workflowUuid,
        @Nullable IntegrationInstanceConfigurationWorkflow targetWorkflow, Map<Long, Long> connectionMappings,
        Map<Long, Long> suggestedMappings, Set<Long> unresolvedConnectionIds) {

        Map<NodeBindingKey, Long> targetConnectionIdsByNode = targetWorkflow == null
            ? Map.of() : targetConnectionIdsByNode(Map.of(workflowUuid, targetWorkflow));
        List<IntegrationInstanceConfigurationWorkflowConnection> connections = new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflowConnection sourceConnection : sourceWorkflow.getConnections()) {
            Long targetConnectionId = connectionMappings.get(sourceConnection.getConnectionId());

            if (targetConnectionId == null) {
                targetConnectionId = targetConnectionIdsByNode.get(
                    new NodeBindingKey(
                        workflowUuid, sourceConnection.getWorkflowNodeName(),
                        sourceConnection.getWorkflowConnectionKey()));
            }

            if (targetConnectionId == null) {
                targetConnectionId = suggestedMappings.get(sourceConnection.getConnectionId());
            }

            if (targetConnectionId == null) {
                unresolvedConnectionIds.add(sourceConnection.getConnectionId());
            } else {
                connections.add(
                    new IntegrationInstanceConfigurationWorkflowConnection(
                        targetConnectionId, sourceConnection.getWorkflowConnectionKey(),
                        sourceConnection.getWorkflowNodeName()));
            }
        }

        IntegrationInstanceConfigurationWorkflow promotedWorkflow = new IntegrationInstanceConfigurationWorkflow();

        promotedWorkflow.setConnections(connections);
        promotedWorkflow.setEnabled(targetWorkflow == null ? sourceWorkflow.isEnabled() : targetWorkflow.isEnabled());
        promotedWorkflow.setInputs(targetWorkflow == null ? Map.of() : targetWorkflow.getInputs());
        promotedWorkflow.setIntegrationInstanceConfigurationId(targetId);
        promotedWorkflow.setWorkflowId(sourceWorkflow.getWorkflowId());

        return promotedWorkflow;
    }

    private List<SourceBinding> sourceBindings(
        List<IntegrationInstanceConfigurationWorkflow> sourceWorkflows,
        Map<String, String> workflowUuidsByWorkflowId) {

        List<SourceBinding> sourceBindings = new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflow sourceWorkflow : sourceWorkflows) {
            List<IntegrationInstanceConfigurationWorkflowConnection> connections = sourceWorkflow.getConnections();

            if (connections.isEmpty()) {
                continue;
            }

            Workflow workflow = workflowService.getWorkflow(sourceWorkflow.getWorkflowId());

            for (IntegrationInstanceConfigurationWorkflowConnection connection : connections) {
                sourceBindings.add(
                    new SourceBinding(
                        workflowUuidsByWorkflowId.get(sourceWorkflow.getWorkflowId()),
                        EmbeddedPromotionSupport.workflowLabel(workflow),
                        connection.getWorkflowNodeName(), connection.getWorkflowConnectionKey(),
                        connection.getConnectionId()));
            }
        }

        return sourceBindings;
    }

    private static Map<NodeBindingKey, Long> targetConnectionIdsByNode(
        Map<String, IntegrationInstanceConfigurationWorkflow> targetWorkflowsByUuid) {

        Map<NodeBindingKey, Long> targetConnectionIdsByNode = new HashMap<>();

        for (Map.Entry<String, IntegrationInstanceConfigurationWorkflow> entry : targetWorkflowsByUuid.entrySet()) {
            IntegrationInstanceConfigurationWorkflow targetWorkflow = entry.getValue();

            for (IntegrationInstanceConfigurationWorkflowConnection connection : targetWorkflow.getConnections()) {
                targetConnectionIdsByNode.put(
                    new NodeBindingKey(
                        entry.getKey(), connection.getWorkflowNodeName(), connection.getWorkflowConnectionKey()),
                    connection.getConnectionId());
            }
        }

        return targetConnectionIdsByNode;
    }

    private IntegrationInstanceConfigurationDTO updatedConfiguration(
        IntegrationInstanceConfiguration source, int integrationVersion, IntegrationInstanceConfiguration target,
        List<IntegrationInstanceConfigurationWorkflowDTO> workflowDTOs) {

        List<Long> sourceTagIds = source.getTagIds();
        List<Tag> tags = sourceTagIds.isEmpty() ? List.of() : tagService.getTags(sourceTagIds);

        return IntegrationInstanceConfigurationDTO.builder()
            .authorizationType(target.getAuthorizationType())
            .connectionParameters(target.getConnectionParameters())
            .description(source.getDescription())
            .enabled(target.isEnabled())
            .environment(target.getEnvironment())
            .id(target.getId())
            .integrationId(Objects.requireNonNull(target.getIntegrationId(), "integrationId"))
            .integrationInstanceConfigurationWorkflows(workflowDTOs)
            .integrationVersion(integrationVersion)
            .name(source.getName())
            .permissionExpression(source.getPermissionExpression())
            .tags(tags)
            .version(target.getVersion())
            .build();
    }

    private Set<String> versionWorkflowIds(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        Integer integrationVersion = integrationInstanceConfiguration.getIntegrationVersion();

        if (integrationVersion == null) {
            return Set.of();
        }

        Set<String> versionWorkflowIds = new LinkedHashSet<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            Objects.requireNonNull(integrationInstanceConfiguration.getIntegrationId(), "integrationId"))) {

            if (integrationWorkflow.getIntegrationVersion() == integrationVersion) {
                versionWorkflowIds.add(integrationWorkflow.getWorkflowId());
            }
        }

        return versionWorkflowIds;
    }

    private Map<String, String> workflowUuidsByWorkflowId(IntegrationInstanceConfiguration source) {
        Map<String, String> workflowUuidsByWorkflowId = new HashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            Objects.requireNonNull(source.getIntegrationId(), "integrationId"))) {

            workflowUuidsByWorkflowId.put(integrationWorkflow.getWorkflowId(), integrationWorkflow.getUuidAsString());
        }

        return workflowUuidsByWorkflowId;
    }
}
