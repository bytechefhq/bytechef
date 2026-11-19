/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeCalculator;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnEEVersion
class IntegrationVersionWorkflowChangeFacadeImpl implements IntegrationVersionWorkflowChangeFacade {

    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowDefinitionDiffService workflowDefinitionDiffService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    IntegrationVersionWorkflowChangeFacadeImpl(
        IntegrationWorkflowService integrationWorkflowService,
        WorkflowDefinitionDiffService workflowDefinitionDiffService, WorkflowService workflowService) {

        this.integrationWorkflowService = integrationWorkflowService;
        this.workflowDefinitionDiffService = workflowDefinitionDiffService;
        this.workflowService = workflowService;
    }

    @Override
    public boolean hasUnpublishedChanges(Integration integration) {
        int draftIntegrationVersion = integration.getLastIntegrationVersion();

        List<Integer> versions = CollectionUtils.map(
            integration.getIntegrationVersions(), IntegrationVersion::getVersion);

        Integer baseVersion = VersionWorkflowChangeCalculator.resolveBaseVersion(versions, draftIntegrationVersion);

        if (baseVersion == null) {
            return true;
        }

        long integrationId = integration.getId();

        List<IntegrationWorkflow> baseIntegrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integrationId, baseVersion);
        List<IntegrationWorkflow> draftIntegrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integrationId, draftIntegrationVersion);

        Map<String, Workflow> workflowsById = getWorkflowsById(baseIntegrationWorkflows, draftIntegrationWorkflows);

        return VersionWorkflowChangeCalculator.hasChanges(
            fetchWorkflows(baseIntegrationWorkflows, workflowsById),
            fetchWorkflows(draftIntegrationWorkflows, workflowsById), workflowDefinitionDiffService);
    }

    private static Map<String, @Nullable Workflow> fetchWorkflows(
        List<IntegrationWorkflow> integrationWorkflows, Map<String, Workflow> workflowsById) {

        Map<String, @Nullable Workflow> workflows = new LinkedHashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflows) {
            workflows.put(integrationWorkflow.getUuidAsString(),
                workflowsById.get(integrationWorkflow.getWorkflowId()));
        }

        return workflows;
    }

    private Map<String, Workflow> getWorkflowsById(
        List<IntegrationWorkflow> baseIntegrationWorkflows, List<IntegrationWorkflow> draftIntegrationWorkflows) {

        Set<String> workflowIds = new LinkedHashSet<>();

        for (IntegrationWorkflow integrationWorkflow : baseIntegrationWorkflows) {
            workflowIds.add(integrationWorkflow.getWorkflowId());
        }

        for (IntegrationWorkflow integrationWorkflow : draftIntegrationWorkflows) {
            workflowIds.add(integrationWorkflow.getWorkflowId());
        }

        Map<String, Workflow> workflowsById = new HashMap<>();

        for (Workflow workflow : workflowService.getWorkflows(List.copyOf(workflowIds))) {
            workflowsById.put(Validate.notNull(workflow.getId(), "id"), workflow);
        }

        return workflowsById;
    }
}
