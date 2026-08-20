/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.ai.copilot.web.graphql.facade.WorkflowDescriptionCopilotFacade;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the Workflow Description Copilot feature.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
@SuppressFBWarnings("EI")
public class WorkflowDescriptionCopilotGraphQlController {

    private final WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade;

    public WorkflowDescriptionCopilotGraphQlController(
        WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade) {

        this.workflowDescriptionCopilotFacade = workflowDescriptionCopilotFacade;
    }

    /**
     * Authorization lives on
     * {@link WorkflowDescriptionCopilotFacade#generateWorkflowDescription(WorkflowDescriptionCopilotRequest)}, which
     * resolves the owning project from the client-supplied {@code workflowId} and requires {@code WORKFLOW_VIEW} on its
     * workspace &mdash; the API facade is this codebase's authorization layer, and this controller carries no gate of
     * its own. That check used to live in this method's body, where it was invisible to any audit scanning for
     * {@code @PreAuthorize}.
     */
    @MutationMapping
    public GenerateWorkflowDescriptionPayload generateWorkflowDescription(
        @Argument GenerateWorkflowDescriptionInput input) {

        WorkflowDescriptionCopilotResult result = workflowDescriptionCopilotFacade.generateWorkflowDescription(
            new WorkflowDescriptionCopilotRequest(
                input.workflowId(), input.workflowNodeName(), input.environmentId()));

        return new GenerateWorkflowDescriptionPayload(result.value());
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionInput(String workflowId, String workflowNodeName, long environmentId) {
    }

    @SuppressFBWarnings("EI")
    public record GenerateWorkflowDescriptionPayload(String value) {
    }
}
