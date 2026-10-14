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
public class CodeWorkflowNotCopyableException extends RuntimeException {

    public CodeWorkflowNotCopyableException(String workflowUuid) {
        super("Catalog workflow template %s is a code workflow and can only be referenced".formatted(workflowUuid));
    }
}
