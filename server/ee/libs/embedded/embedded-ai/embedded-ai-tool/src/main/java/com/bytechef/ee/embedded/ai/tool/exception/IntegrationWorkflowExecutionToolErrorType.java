/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationWorkflowExecutionToolErrorType extends AbstractErrorType {

    public static final IntegrationWorkflowExecutionToolErrorType GET_WORKFLOW_EXECUTION =
        new IntegrationWorkflowExecutionToolErrorType(100);
    public static final IntegrationWorkflowExecutionToolErrorType LIST_WORKFLOW_EXECUTIONS =
        new IntegrationWorkflowExecutionToolErrorType(101);

    private IntegrationWorkflowExecutionToolErrorType(int errorKey) {
        super(IntegrationWorkflowExecutionToolErrorType.class, errorKey);
    }
}
