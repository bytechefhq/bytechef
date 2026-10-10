/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationWorkflowErrorType extends AbstractErrorType {

    public static final IntegrationWorkflowErrorType INTEGRATION_WORKFLOW_NOT_FOUND =
        new IntegrationWorkflowErrorType(100);
    public static final IntegrationWorkflowErrorType CREATE_WORKFLOW = new IntegrationWorkflowErrorType(101);
    public static final IntegrationWorkflowErrorType DELETE_WORKFLOW = new IntegrationWorkflowErrorType(102);
    public static final IntegrationWorkflowErrorType GET_WORKFLOW = new IntegrationWorkflowErrorType(103);
    public static final IntegrationWorkflowErrorType LIST_WORKFLOWS = new IntegrationWorkflowErrorType(104);
    public static final IntegrationWorkflowErrorType SEARCH_WORKFLOWS = new IntegrationWorkflowErrorType(105);
    public static final IntegrationWorkflowErrorType UPDATE_WORKFLOW = new IntegrationWorkflowErrorType(106);

    private IntegrationWorkflowErrorType(int errorKey) {
        super(IntegrationWorkflow.class, errorKey);
    }
}
