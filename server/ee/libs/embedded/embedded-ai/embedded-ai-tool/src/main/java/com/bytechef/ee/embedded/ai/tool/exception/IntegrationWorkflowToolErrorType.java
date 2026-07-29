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
public class IntegrationWorkflowToolErrorType extends AbstractErrorType {

    public static final IntegrationWorkflowToolErrorType CREATE_WORKFLOW = new IntegrationWorkflowToolErrorType(100);
    public static final IntegrationWorkflowToolErrorType DELETE_WORKFLOW = new IntegrationWorkflowToolErrorType(101);
    public static final IntegrationWorkflowToolErrorType GET_WORKFLOW = new IntegrationWorkflowToolErrorType(102);
    public static final IntegrationWorkflowToolErrorType LIST_WORKFLOWS = new IntegrationWorkflowToolErrorType(103);
    public static final IntegrationWorkflowToolErrorType SEARCH_WORKFLOWS = new IntegrationWorkflowToolErrorType(104);
    public static final IntegrationWorkflowToolErrorType UPDATE_WORKFLOW = new IntegrationWorkflowToolErrorType(105);

    private IntegrationWorkflowToolErrorType(int errorKey) {
        super(IntegrationWorkflowToolErrorType.class, errorKey);
    }
}
