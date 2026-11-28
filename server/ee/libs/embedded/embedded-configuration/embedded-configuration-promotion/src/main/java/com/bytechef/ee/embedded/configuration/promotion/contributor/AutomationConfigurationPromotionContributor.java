/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.promotion.contributor;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
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
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectConfigurationFacade;
import com.bytechef.ee.embedded.configuration.promotion.EmbeddedConnectionCandidates;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.util.CarriedInputUtils;
import com.bytechef.ee.embedded.configuration.util.InternalOnlyInputUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
public class AutomationConfigurationPromotionContributor implements EnvironmentPromotionContributor {

    public static final String RESOURCE_TYPE = "AUTOMATION_CONFIGURATION";

    private static final String CONFIGURATION_WORKFLOW_ASSET_TYPE = "CONFIGURATION_WORKFLOW";
    private static final String WORKFLOW_INPUT_ASSET_TYPE = "WORKFLOW_INPUT";

    private final AutomationProjectConfigurationFacade automationProjectConfigurationFacade;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectionService connectionService;
    private final EmbeddedConnectionCandidates embeddedConnectionCandidates;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public AutomationConfigurationPromotionContributor(
        AutomationProjectConfigurationFacade automationProjectConfigurationFacade,
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectionService connectionService, EmbeddedConnectionCandidates embeddedConnectionCandidates,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.automationProjectConfigurationFacade = automationProjectConfigurationFacade;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectionService = connectionService;
        this.embeddedConnectionCandidates = embeddedConnectionCandidates;
        this.projectWorkflowService = projectWorkflowService;
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

        AutomationProjectConfiguration source = loadSource(sourceId, sourceEnvironment, targetEnvironment);

        List<ProjectWorkflow> sourceProjectWorkflows = projectWorkflows(source);
        Optional<AutomationProjectConfiguration> existingTarget = fetchTarget(source, targetEnvironment);
        List<PromotionChange> changes = predictChanges(source, sourceProjectWorkflows, existingTarget.orElse(null));
        List<SourceBinding> sourceBindings = sourceBindings(source, sourceProjectWorkflows);

        Map<Long, Long> existingTargetBindings = existingTarget
            .map(target -> existingTargetBindings(sourceBindings, target))
            .orElse(Map.of());

        List<PromotionConnectionMapping> connectionMappings = PromotionPreviews.connectionMappings(
            sourceBindings, existingTargetBindings, connectionService,
            connection -> embeddedConnectionCandidates.candidates(connection.getComponentName(), targetEnvironment));

        String environmentName = PromotionEnvironmentUtils.displayName(targetEnvironment);
        List<String> warnings = new ArrayList<>();

        if (isNameTaken(source, targetEnvironment)) {
            warnings.add(
                ("An automation configuration named \"%s\" already exists in %s; rename one of them before " +
                    "promoting.").formatted(source.getName(), environmentName));
        }

        if (existingTarget.isEmpty()) {
            warnings.add(
                ("The promoted automation configuration is created disabled; fill in its inputs in %s before " +
                    "enabling it.").formatted(environmentName));
        }

        warnings.addAll(
            inputWarnings(source, sourceProjectWorkflows, existingTarget.orElse(null), environmentName));
        warnings.addAll(EmbeddedPromotionSupport.unresolvedConnectionWarnings(connectionMappings, environmentName));

        return new EnvironmentPromotionPreview(
            RESOURCE_TYPE, sourceId, sourceEnvironment, targetEnvironment,
            existingTarget.map(AutomationProjectConfiguration::getId)
                .orElse(null),
            existingTarget.map(AutomationProjectConfiguration::getName)
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

        AutomationProjectConfiguration source = loadSource(sourceId, sourceEnvironment, targetEnvironment);

        List<ProjectWorkflow> sourceProjectWorkflows = projectWorkflows(source);
        List<SourceBinding> sourceBindings = sourceBindings(source, sourceProjectWorkflows);

        PromotionConnectionScope.checkMappedConnectionsBelongToSource(
            PromotionConnectionScope.sourceConnectionIds(sourceBindings), connectionMappings);

        EmbeddedPromotionSupport.checkMappedConnectionsAreCandidates(
            connectionMappings, targetEnvironment, connectionService, embeddedConnectionCandidates);

        Map<Long, Long> suggestedMappings = EmbeddedPromotionSupport.suggestedConnectionMappings(
            PromotionConnectionScope.sourceConnectionIds(sourceBindings), targetEnvironment, connectionService,
            embeddedConnectionCandidates);

        if (isNameTaken(source, targetEnvironment)) {
            throw new ConfigurationException(
                "An automation configuration named '%s' already exists in %s".formatted(
                    source.getName(), targetEnvironment),
                EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT);
        }

        Optional<AutomationProjectConfiguration> existingTarget = fetchTarget(source, targetEnvironment);

        List<PromotionChange> changes = predictChanges(source, sourceProjectWorkflows, existingTarget.orElse(null));

        PromotionChangeHashes.checkNotStale(
            expectedDestructiveChangesHash, PromotionChangeHashes.destructiveChangesHash(changes));

        boolean created = existingTarget.isEmpty();

        AutomationProjectConfiguration previousTarget = existingTarget.orElseGet(
            () -> createTarget(source, targetEnvironment));

        long targetId = Objects.requireNonNull(previousTarget.getId(), "id");
        Set<String> previousWorkflowUuids = created ? Set.of() : workflowUuids(projectWorkflows(previousTarget));

        automationProjectConfigurationFacade.updateConfiguration(
            targetId, source.getName(), source.getPermissionExpression(), source.getProjectVersion(),
            !created && previousTarget.isEnabled(), source.isAutomationHubVisible());

        AutomationProjectConfiguration updatedTarget = automationProjectConfigurationService.getConfiguration(targetId);
        Set<Long> unresolvedConnectionIds = new LinkedHashSet<>();

        for (ProjectWorkflow projectWorkflow : sourceProjectWorkflows) {
            String workflowUuid = projectWorkflow.getUuidAsString();

            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            Optional<AutomationProjectConfigurationWorkflow> targetWorkflowRow = updatedTarget.fetchWorkflow(
                projectWorkflow.getUuid());

            Map<String, Object> keptInputs = targetWorkflowRow
                .map(workflowRow -> InternalOnlyInputUtils.retainInternalOnlyInputs(
                    workflow, workflowRow.getProvidedInputs()))
                .orElse(Map.of());
            Map<String, Long> currentConnections = targetWorkflowRow
                .map(AutomationProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of());

            boolean newToTarget = !previousWorkflowUuids.contains(workflowUuid);

            boolean enabled = newToTarget
                ? source.isWorkflowEnabled(projectWorkflow.getUuid())
                : previousTarget.isWorkflowEnabled(projectWorkflow.getUuid());

            if (EmbeddedPromotionSupport.isCreatedDisabled(
                newToTarget, enabled,
                InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(workflow, keptInputs))) {

                enabled = false;
            }

            Map<String, Long> promotedConnections = promotedConnections(
                sourceProvidedConnections(source, projectWorkflow), currentConnections, connectionMappings,
                suggestedMappings, unresolvedConnectionIds);

            boolean currentlyEnabled = updatedTarget.isWorkflowEnabled(projectWorkflow.getUuid());

            if (!enabled && currentlyEnabled) {
                automationProjectConfigurationFacade.enableConfigurationWorkflow(targetId, workflowUuid, false);
            }

            automationProjectConfigurationFacade.updateConfigurationWorkflow(
                targetId, workflowUuid, promotedConnections, keptInputs);

            if (enabled && !currentlyEnabled) {
                automationProjectConfigurationFacade.enableConfigurationWorkflow(targetId, workflowUuid, true);
            }
        }

        return new EnvironmentPromotionResult(targetId, created, null, new ArrayList<>(unresolvedConnectionIds));
    }

    private AutomationProjectConfiguration createTarget(
        AutomationProjectConfiguration source, Environment targetEnvironment) {

        return automationProjectConfigurationService.create(
            source.getProjectId(), targetEnvironment.ordinal(), source.getName(), source.getPermissionExpression(),
            source.getProjectVersion(), false, source.isAutomationHubVisible(),
            Objects.requireNonNull(source.getUuid(), "uuid"));
    }

    private static Map<Long, Long> existingTargetBindings(
        List<SourceBinding> sourceBindings, AutomationProjectConfiguration target) {

        Map<Long, Long> existingTargetBindings = new HashMap<>();

        for (SourceBinding sourceBinding : sourceBindings) {
            Optional<AutomationProjectConfigurationWorkflow> targetWorkflowOptional = target.fetchWorkflow(
                UUID.fromString(Objects.requireNonNull(sourceBinding.workflowUuid())));

            Map<String, Long> targetConnections = targetWorkflowOptional
                .map(AutomationProjectConfigurationWorkflow::getProvidedConnections)
                .orElse(Map.of());

            Long targetConnectionId = targetConnections.get(sourceBinding.key());

            if (targetConnectionId != null) {
                existingTargetBindings.putIfAbsent(sourceBinding.connectionId(), targetConnectionId);
            }
        }

        return existingTargetBindings;
    }

    private Optional<AutomationProjectConfiguration> fetchTarget(
        AutomationProjectConfiguration source, Environment targetEnvironment) {

        return automationProjectConfigurationService.fetchConfiguration(
            Objects.requireNonNull(source.getUuid(), "uuid"), targetEnvironment.ordinal());
    }

    private List<String> inputWarnings(
        AutomationProjectConfiguration source, List<ProjectWorkflow> sourceProjectWorkflows,
        @Nullable AutomationProjectConfiguration existingTarget, String environmentName) {

        Map<String, String> previousWorkflowIdsByUuid = existingTarget == null
            ? Map.of() : workflowIdsByUuid(projectWorkflows(existingTarget));
        AutomationProjectConfiguration existingTargetOrSource = existingTarget == null ? source : existingTarget;
        List<String> warnings = new ArrayList<>();

        for (ProjectWorkflow projectWorkflow : sourceProjectWorkflows) {
            String workflowUuid = projectWorkflow.getUuidAsString();

            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            String previousWorkflowId = previousWorkflowIdsByUuid.get(workflowUuid);
            Map<String, Object> keptInputs = previousWorkflowId == null
                ? Map.of() : predictedKeptInputs(existingTargetOrSource, projectWorkflow, previousWorkflowId, workflow);

            for (String inputName : new TreeSet<>(InternalOnlyInputUtils.getInternalOnlyInputNames(workflow))) {
                if (!keptInputs.containsKey(inputName)) {
                    warnings.add(
                        "Workflow \"%s\" input \"%s\" needs a value in %s.".formatted(
                            EmbeddedPromotionSupport.workflowLabel(workflow), inputName, environmentName));
                }
            }

            boolean enabled = previousWorkflowId == null
                ? source.isWorkflowEnabled(projectWorkflow.getUuid())
                : existingTargetOrSource.isWorkflowEnabled(projectWorkflow.getUuid());
            String missingInternalOnlyInputName = InternalOnlyInputUtils.findMissingRequiredInternalOnlyInput(
                workflow, keptInputs);

            String internalOnlyInputWarning = EmbeddedPromotionSupport.internalOnlyInputWarning(
                workflow, previousWorkflowId == null, enabled, missingInternalOnlyInputName, environmentName);

            if (internalOnlyInputWarning != null) {
                warnings.add(internalOnlyInputWarning);
            }
        }

        return warnings;
    }

    private boolean isNameTaken(AutomationProjectConfiguration source, Environment targetEnvironment) {
        String name = StringUtils.trim(source.getName());

        List<AutomationProjectConfiguration> targetConfigurations =
            automationProjectConfigurationService.getConfigurations(source.getProjectId(), targetEnvironment.ordinal());

        return targetConfigurations.stream()
            .filter(configuration -> !Objects.equals(configuration.getUuid(), source.getUuid()))
            .anyMatch(configuration -> Strings.CI.equals(StringUtils.trim(configuration.getName()), name));
    }

    private AutomationProjectConfiguration loadSource(
        long sourceId, Environment sourceEnvironment, Environment targetEnvironment) {

        AutomationProjectConfiguration source = automationProjectConfigurationService.getConfiguration(sourceId);

        if (source.getEnvironmentId() != sourceEnvironment.ordinal()) {
            throw new ConfigurationException(
                "Automation configuration id=%s is in environment id=%s, not in %s".formatted(
                    sourceId, source.getEnvironmentId(), sourceEnvironment),
                EnvironmentPromotionErrorType.SOURCE_ENVIRONMENT_MISMATCH);
        }

        if (source.getEnvironmentId() == targetEnvironment.ordinal()) {
            throw new ConfigurationException(
                "Automation configuration id=%s is already in %s".formatted(sourceId, targetEnvironment),
                EnvironmentPromotionErrorType.SAME_ENVIRONMENT);
        }

        return source;
    }

    private List<PromotionChange> predictChanges(
        AutomationProjectConfiguration source, List<ProjectWorkflow> sourceProjectWorkflows,
        @Nullable AutomationProjectConfiguration existingTarget) {

        if (existingTarget != null) {
            automationProjectConfigurationFacade.validateVersionChange(
                Objects.requireNonNull(existingTarget.getId(), "id"), source.getProjectVersion());
        }

        Map<String, String> previousWorkflowIdsByUuid = existingTarget == null
            ? Map.of() : workflowIdsByUuid(projectWorkflows(existingTarget));
        String parentName = source.getName();
        List<PromotionChange> changes = new ArrayList<>();

        if (existingTarget != null && overwritesDifferingField(source, existingTarget)) {
            changes.add(
                new PromotionChange(
                    Kind.UPDATE, RESOURCE_TYPE, String.valueOf(existingTarget.getUuid()), null, parentName, false));
        }

        for (ProjectWorkflow projectWorkflow : sourceProjectWorkflows) {
            String workflowUuid = projectWorkflow.getUuidAsString();

            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            String workflowLabel = EmbeddedPromotionSupport.workflowLabel(workflow);
            String previousWorkflowId = previousWorkflowIdsByUuid.get(workflowUuid);

            if (previousWorkflowId == null) {
                changes.add(
                    new PromotionChange(
                        Kind.CREATE, CONFIGURATION_WORKFLOW_ASSET_TYPE, workflowUuid, parentName, workflowLabel,
                        false));

                continue;
            }

            if (Objects.equals(previousWorkflowId, projectWorkflow.getWorkflowId())) {
                continue;
            }

            Workflow previousWorkflow = workflowService.getWorkflow(previousWorkflowId);

            if (!WorkflowDefinitions.sameDefinition(workflow, previousWorkflow)) {
                changes.add(
                    new PromotionChange(
                        Kind.UPDATE, CONFIGURATION_WORKFLOW_ASSET_TYPE, workflowUuid, parentName, workflowLabel,
                        false));
            }

            AutomationProjectConfiguration target = Objects.requireNonNull(existingTarget, "existingTarget");

            Optional<AutomationProjectConfigurationWorkflow> targetWorkflowRow = target.fetchWorkflow(
                projectWorkflow.getUuid());

            if (targetWorkflowRow.isEmpty()) {
                continue;
            }

            AutomationProjectConfigurationWorkflow workflowRow = targetWorkflowRow.get();

            for (String inputName : CarriedInputUtils.findDroppedInputs(
                previousWorkflow, workflow, workflowRow.getProvidedInputs())) {

                changes.add(
                    new PromotionChange(
                        Kind.UPDATE, WORKFLOW_INPUT_ASSET_TYPE, workflowUuid + ":" + inputName, workflowLabel,
                        inputName, true));
            }
        }

        return changes;
    }

    private static boolean overwritesDifferingField(
        AutomationProjectConfiguration source, AutomationProjectConfiguration target) {

        return !Objects.equals(source.getName(), target.getName()) ||
            !Objects.equals(source.getPermissionExpression(), target.getPermissionExpression()) ||
            source.getProjectVersion() != target.getProjectVersion() ||
            source.isAutomationHubVisible() != target.isAutomationHubVisible();
    }

    private Map<String, Object> predictedKeptInputs(
        AutomationProjectConfiguration existingTarget, ProjectWorkflow projectWorkflow, String previousWorkflowId,
        Workflow workflow) {

        Optional<AutomationProjectConfigurationWorkflow> targetWorkflowRow = existingTarget.fetchWorkflow(
            projectWorkflow.getUuid());

        if (targetWorkflowRow.isEmpty()) {
            return Map.of();
        }

        AutomationProjectConfigurationWorkflow workflowRow = targetWorkflowRow.get();

        Map<String, Object> carriedInputs = workflowRow.getProvidedInputs();

        if (!Objects.equals(previousWorkflowId, projectWorkflow.getWorkflowId())) {
            carriedInputs = CarriedInputUtils.retainInputsOfUnchangedType(
                workflowService.getWorkflow(previousWorkflowId), workflow, carriedInputs);
        }

        return InternalOnlyInputUtils.retainInternalOnlyInputs(workflow, carriedInputs);
    }

    private static Map<String, Long> promotedConnections(
        Map<String, Long> sourceConnections, Map<String, Long> currentConnections, Map<Long, Long> connectionMappings,
        Map<Long, Long> suggestedMappings, Set<Long> unresolvedConnectionIds) {

        Map<String, Long> promotedConnections = new HashMap<>();

        for (Map.Entry<String, Long> sourceConnection : sourceConnections.entrySet()) {
            String componentName = sourceConnection.getKey();

            Long targetConnectionId = connectionMappings.get(sourceConnection.getValue());

            if (targetConnectionId == null) {
                targetConnectionId = currentConnections.get(componentName);
            }

            if (targetConnectionId == null) {
                targetConnectionId = suggestedMappings.get(sourceConnection.getValue());
            }

            if (targetConnectionId == null) {
                unresolvedConnectionIds.add(sourceConnection.getValue());
            } else {
                promotedConnections.put(componentName, targetConnectionId);
            }
        }

        return promotedConnections;
    }

    private List<ProjectWorkflow> projectWorkflows(AutomationProjectConfiguration configuration) {
        return projectWorkflowService.getProjectWorkflows(
            configuration.getProjectId(), configuration.getProjectVersion());
    }

    private List<SourceBinding> sourceBindings(
        AutomationProjectConfiguration source, List<ProjectWorkflow> sourceProjectWorkflows) {

        List<SourceBinding> sourceBindings = new ArrayList<>();

        for (ProjectWorkflow projectWorkflow : sourceProjectWorkflows) {
            Map<String, Long> providedConnections = sourceProvidedConnections(source, projectWorkflow);

            if (providedConnections.isEmpty()) {
                continue;
            }

            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            for (String componentName : new TreeSet<>(providedConnections.keySet())) {
                sourceBindings.add(
                    new SourceBinding(
                        projectWorkflow.getUuidAsString(), EmbeddedPromotionSupport.workflowLabel(workflow),
                        componentName, componentName, providedConnections.get(componentName)));
            }
        }

        return sourceBindings;
    }

    private static Map<String, Long> sourceProvidedConnections(
        AutomationProjectConfiguration source, ProjectWorkflow projectWorkflow) {

        Optional<AutomationProjectConfigurationWorkflow> sourceWorkflowOptional = source.fetchWorkflow(
            projectWorkflow.getUuid());

        return sourceWorkflowOptional
            .map(AutomationProjectConfigurationWorkflow::getProvidedConnections)
            .orElse(Map.of());
    }

    private static Map<String, String> workflowIdsByUuid(List<ProjectWorkflow> projectWorkflows) {
        return projectWorkflows.stream()
            .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, ProjectWorkflow::getWorkflowId));
    }

    private static Set<String> workflowUuids(List<ProjectWorkflow> projectWorkflows) {
        return projectWorkflows.stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());
    }
}
