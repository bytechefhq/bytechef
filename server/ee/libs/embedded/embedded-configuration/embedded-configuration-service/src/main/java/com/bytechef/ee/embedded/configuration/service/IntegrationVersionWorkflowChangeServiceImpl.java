/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeCalculator;
import com.bytechef.platform.configuration.dto.VersionWorkflowChangeSummary;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
class IntegrationVersionWorkflowChangeServiceImpl implements IntegrationVersionWorkflowChangeService {

    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowDefinitionDiffService workflowDefinitionDiffService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    IntegrationVersionWorkflowChangeServiceImpl(
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

        VersionWorkflowChanges versionWorkflowChanges = VersionWorkflowChangeCalculator.summarize(
            baseVersion, draftIntegrationVersion, getWorkflows(integrationId, baseVersion),
            getWorkflows(integrationId, draftIntegrationVersion), workflowDefinitionDiffService);

        List<VersionWorkflowChangeSummary> workflows = versionWorkflowChanges.workflows();

        return !workflows.isEmpty();
    }

    private Map<String, Workflow> getWorkflows(long integrationId, int integrationVersion) {
        Map<String, Workflow> workflows = new LinkedHashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            integrationId, integrationVersion)) {

            workflows.put(
                integrationWorkflow.getUuidAsString(),
                workflowService.getWorkflow(integrationWorkflow.getWorkflowId()));
        }

        return workflows;
    }
}
