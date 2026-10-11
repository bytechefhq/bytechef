/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * Thrown when a connected user copies an automation workflow template they have already copied. Carries the uuid of the
 * existing copy so a client can open it instead. Mapped to 409.
 *
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
