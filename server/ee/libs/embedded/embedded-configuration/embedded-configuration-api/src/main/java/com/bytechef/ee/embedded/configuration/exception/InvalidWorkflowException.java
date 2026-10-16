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
public class InvalidWorkflowException extends RuntimeException {

    private final int errorCount;

    public InvalidWorkflowException(String workflowUuid, int errorCount) {
        super("Workflow %s has %d validation error(s) and cannot be enabled".formatted(workflowUuid, errorCount));

        this.errorCount = errorCount;
    }

    public int getErrorCount() {
        return errorCount;
    }
}
