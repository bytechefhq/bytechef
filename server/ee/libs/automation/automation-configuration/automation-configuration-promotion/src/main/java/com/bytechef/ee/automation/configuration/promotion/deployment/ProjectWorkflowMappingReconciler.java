/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.deployment;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.promotion.dto.PromotionChange;
import com.bytechef.ee.automation.promotion.dto.PromotionChange.Kind;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectWorkflowMappingReconciler {

    public static final String ASSET_TYPE = "EXPOSED_WORKFLOW";

    @SuppressFBWarnings("EI")
    public record Mapping(long id, long projectDeploymentWorkflowId, Map<String, ?> parameters) {
    }

    @SuppressFBWarnings("EI")
    public record MappingPlan(
        List<StaleMapping> staleMappings, List<NewMapping> newMappings, List<ChangedMapping> changedMappings) {

        public MappingPlan {
            staleMappings = List.copyOf(staleMappings);
            newMappings = List.copyOf(newMappings);
            changedMappings = List.copyOf(changedMappings);
        }
    }

    public record StaleMapping(
        long mappingId, long projectDeploymentWorkflowId, @Nullable String workflowUuid, @Nullable String workflowId) {
    }

    @SuppressFBWarnings("EI")
    public record NewMapping(
        long sourceProjectDeploymentWorkflowId, String workflowUuid, String workflowId, Map<String, ?> parameters) {
    }

    @SuppressFBWarnings("EI")
    public record ChangedMapping(long mappingId, String workflowUuid, String workflowId, Map<String, ?> parameters) {
    }

    private record DeploymentWorkflowIdentity(String workflowUuid, String workflowId) {
    }

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowMappingReconciler(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    public MappingPlan planMappings(
        ProjectDeployment source, @Nullable ProjectDeployment target, List<Mapping> mappings,
        Map<Long, Map<String, ?>> sourceParameters) {

        Map<Long, DeploymentWorkflowIdentity> sourceIdentities = identitiesByProjectDeploymentWorkflowId(source);
        Map<Long, DeploymentWorkflowIdentity> targetIdentities =
            target == null ? Map.of() : identitiesByProjectDeploymentWorkflowId(target);

        Set<String> sourceWorkflowUuids = new HashSet<>();

        for (DeploymentWorkflowIdentity sourceIdentity : sourceIdentities.values()) {
            sourceWorkflowUuids.add(sourceIdentity.workflowUuid());
        }

        Map<String, Long> targetProjectDeploymentWorkflowIdsByUuid = new HashMap<>();

        for (Map.Entry<Long, DeploymentWorkflowIdentity> entry : targetIdentities.entrySet()) {
            DeploymentWorkflowIdentity targetIdentity = entry.getValue();

            targetProjectDeploymentWorkflowIdsByUuid.putIfAbsent(targetIdentity.workflowUuid(), entry.getKey());
        }

        List<StaleMapping> staleMappings = new ArrayList<>();
        Map<Long, Mapping> keptMappingsByProjectDeploymentWorkflowId = new HashMap<>();

        for (Mapping mapping : mappings) {
            DeploymentWorkflowIdentity targetIdentity = targetIdentities.get(mapping.projectDeploymentWorkflowId());

            if (targetIdentity == null || !sourceWorkflowUuids.contains(targetIdentity.workflowUuid())) {
                staleMappings.add(
                    new StaleMapping(
                        mapping.id(), mapping.projectDeploymentWorkflowId(),
                        targetIdentity == null ? null : targetIdentity.workflowUuid(),
                        targetIdentity == null ? null : targetIdentity.workflowId()));
            } else {
                keptMappingsByProjectDeploymentWorkflowId.putIfAbsent(mapping.projectDeploymentWorkflowId(), mapping);
            }
        }

        List<NewMapping> newMappings = new ArrayList<>();
        List<ChangedMapping> changedMappings = new ArrayList<>();

        List<Long> sourceProjectDeploymentWorkflowIds = sourceParameters.keySet()
            .stream()
            .sorted()
            .toList();

        for (Long sourceProjectDeploymentWorkflowId : sourceProjectDeploymentWorkflowIds) {
            DeploymentWorkflowIdentity sourceIdentity = sourceIdentities.get(sourceProjectDeploymentWorkflowId);

            if (sourceIdentity == null) {
                continue;
            }

            Map<String, ?> parameters = sourceParameters.get(sourceProjectDeploymentWorkflowId);
            Long targetProjectDeploymentWorkflowId =
                targetProjectDeploymentWorkflowIdsByUuid.get(sourceIdentity.workflowUuid());
            Mapping mapping = targetProjectDeploymentWorkflowId == null ? null
                : keptMappingsByProjectDeploymentWorkflowId.get(targetProjectDeploymentWorkflowId);

            if (mapping == null) {
                newMappings.add(
                    new NewMapping(
                        sourceProjectDeploymentWorkflowId, sourceIdentity.workflowUuid(), sourceIdentity.workflowId(),
                        parameters));
            } else if (!Objects.equals(mapping.parameters(), parameters)) {
                changedMappings.add(
                    new ChangedMapping(
                        mapping.id(), sourceIdentity.workflowUuid(), sourceIdentity.workflowId(), parameters));
            }
        }

        return new MappingPlan(staleMappings, newMappings, changedMappings);
    }

    public List<PromotionChange> predictChanges(MappingPlan mappingPlan, String projectName) {
        List<PromotionChange> changes = new ArrayList<>();

        for (NewMapping newMapping : mappingPlan.newMappings()) {
            changes.add(
                new PromotionChange(
                    Kind.CREATE, ASSET_TYPE, newMapping.workflowUuid(), projectName,
                    workflowLabel(newMapping.workflowId(), newMapping.workflowUuid()), false));
        }

        for (ChangedMapping changedMapping : mappingPlan.changedMappings()) {
            changes.add(
                new PromotionChange(
                    Kind.UPDATE, ASSET_TYPE, changedMapping.workflowUuid(), projectName,
                    workflowLabel(changedMapping.workflowId(), changedMapping.workflowUuid()), false));
        }

        for (StaleMapping staleMapping : mappingPlan.staleMappings()) {
            String key = staleMapping.workflowUuid() == null
                ? String.valueOf(staleMapping.projectDeploymentWorkflowId()) : staleMapping.workflowUuid();

            changes.add(
                new PromotionChange(
                    Kind.DELETE, ASSET_TYPE, key, projectName, workflowLabel(staleMapping.workflowId(), key), true));
        }

        return changes;
    }

    public void deleteStaleMappings(MappingPlan mappingPlan, LongConsumer deleteMapping) {
        for (StaleMapping staleMapping : mappingPlan.staleMappings()) {
            deleteMapping.accept(staleMapping.mappingId());
        }
    }

    public void syncMappings(
        MappingPlan mappingPlan, Map<Long, Long> workflowIdMapping, LongFunction<Long> createMapping,
        BiConsumer<Long, Map<String, ?>> updateMappingParameters) {

        for (NewMapping newMapping : mappingPlan.newMappings()) {
            Long targetProjectDeploymentWorkflowId =
                workflowIdMapping.get(newMapping.sourceProjectDeploymentWorkflowId());

            if (targetProjectDeploymentWorkflowId == null) {
                continue;
            }

            Long mappingId = createMapping.apply(targetProjectDeploymentWorkflowId);

            updateMappingParameters.accept(mappingId, newMapping.parameters());
        }

        for (ChangedMapping changedMapping : mappingPlan.changedMappings()) {
            updateMappingParameters.accept(changedMapping.mappingId(), changedMapping.parameters());
        }
    }

    private Map<Long, DeploymentWorkflowIdentity> identitiesByProjectDeploymentWorkflowId(
        ProjectDeployment projectDeployment) {

        Map<String, String> uuidsByWorkflowId = uuidsByWorkflowId(projectDeployment);
        Map<Long, DeploymentWorkflowIdentity> identities = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : getProjectDeploymentWorkflows(projectDeployment)) {
            String workflowId = projectDeploymentWorkflow.getWorkflowId();
            String workflowUuid = uuidsByWorkflowId.get(workflowId);

            if (workflowUuid != null) {
                identities.put(
                    projectDeploymentWorkflow.getId(), new DeploymentWorkflowIdentity(workflowUuid, workflowId));
            }
        }

        return identities;
    }

    private List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(ProjectDeployment projectDeployment) {
        return new ArrayList<>(
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(projectDeployment.getId(), "id")));
    }

    private Map<String, String> uuidsByWorkflowId(ProjectDeployment projectDeployment) {
        Map<String, String> uuidsByWorkflowId = new HashMap<>();

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            Objects.requireNonNull(projectDeployment.getProjectId(), "projectId"),
            projectDeployment.getProjectVersion());

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            uuidsByWorkflowId.put(projectWorkflow.getWorkflowId(), projectWorkflow.getUuidAsString());
        }

        return uuidsByWorkflowId;
    }

    private String workflowLabel(@Nullable String workflowId, String fallback) {
        if (workflowId == null) {
            return fallback;
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        String label = workflow.getLabel();

        return label == null ? fallback : label;
    }
}
