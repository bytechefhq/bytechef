/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.CopilotGenerationException;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
class WorkflowDescriptionCopilotFacadeImpl implements WorkflowDescriptionCopilotFacade {

    private final WorkflowDescriptionCopilotGenerator workflowDescriptionCopilotGenerator;

    WorkflowDescriptionCopilotFacadeImpl(
        Optional<WorkflowDescriptionCopilotGenerator> workflowDescriptionCopilotGenerator) {
        this.workflowDescriptionCopilotGenerator = workflowDescriptionCopilotGenerator.orElse(null);
    }

    @Override
    @PreAuthorize("hasPermission(#request.workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public WorkflowDescriptionCopilotResult generateWorkflowDescription(WorkflowDescriptionCopilotRequest request) {
        if (workflowDescriptionCopilotGenerator == null) {
            throw new IllegalStateException("Workflow Description Copilot is not enabled");
        }

        try {
            return workflowDescriptionCopilotGenerator.generate(request);
        } catch (ChatModelUnavailableException | CopilotGenerationException exception) {
            throw new GraphQlBadRequestException(exception.getMessage(), exception);
        }
    }
}
