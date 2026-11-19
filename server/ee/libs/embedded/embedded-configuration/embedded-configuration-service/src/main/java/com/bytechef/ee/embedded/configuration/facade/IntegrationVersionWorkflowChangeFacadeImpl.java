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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        return VersionWorkflowChangeCalculator.hasChanges(
            fetchWorkflows(integrationId, baseVersion), fetchWorkflows(integrationId, draftIntegrationVersion),
            workflowDefinitionDiffService);
    }

    private Map<String, @Nullable Workflow> fetchWorkflows(long integrationId, int integrationVersion) {
        Map<String, @Nullable Workflow> workflows = new LinkedHashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            integrationId, integrationVersion)) {

            Optional<Workflow> workflowOptional = workflowService.fetchWorkflow(integrationWorkflow.getWorkflowId());

            workflows.put(integrationWorkflow.getUuidAsString(), workflowOptional.orElse(null));
        }

        return workflows;
    }
}
