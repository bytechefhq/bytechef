/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowTemplateAlreadyCopiedException extends RuntimeException {

    private final String copiedWorkflowUuid;

    public WorkflowTemplateAlreadyCopiedException(String workflowTemplateUuid, String copiedWorkflowUuid) {
        super(
            "Automation workflow template %s is already copied as workflow %s".formatted(
                workflowTemplateUuid, copiedWorkflowUuid));

        this.copiedWorkflowUuid = copiedWorkflowUuid;
    }

    public String getCopiedWorkflowUuid() {
        return copiedWorkflowUuid;
    }
}
