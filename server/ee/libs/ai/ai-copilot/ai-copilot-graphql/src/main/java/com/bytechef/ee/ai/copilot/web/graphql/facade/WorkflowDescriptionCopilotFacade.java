/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;

/**
 * Facade for the {@code generateWorkflowDescription} GraphQL mutation. Hosts the {@code WORKFLOW_VIEW} authorization
 * guard on the client-supplied {@code workflowId} so it applies to every caller of the facade rather than only the
 * GraphQL entry point, and keeps it off the shared {@code WorkflowDescriptionCopilotGenerator}, which is a
 * prompt-building collaborator rather than an authorization boundary.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkflowDescriptionCopilotFacade {

    WorkflowDescriptionCopilotResult generateWorkflowDescription(WorkflowDescriptionCopilotRequest request);
}
