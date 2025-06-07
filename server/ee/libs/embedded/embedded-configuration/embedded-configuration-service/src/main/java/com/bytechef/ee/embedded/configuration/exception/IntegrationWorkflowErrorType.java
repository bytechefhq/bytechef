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

    private IntegrationWorkflowErrorType(int errorKey) {
        super(IntegrationWorkflow.class, errorKey);
    }
}
