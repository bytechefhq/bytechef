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
public class IntegrationCodeWorkflowToolErrorType extends AbstractErrorType {

    public static final IntegrationCodeWorkflowToolErrorType CREATE = new IntegrationCodeWorkflowToolErrorType(100);
    public static final IntegrationCodeWorkflowToolErrorType UPDATE_SOURCE =
        new IntegrationCodeWorkflowToolErrorType(101);
    public static final IntegrationCodeWorkflowToolErrorType GET_SOURCE =
        new IntegrationCodeWorkflowToolErrorType(102);
    public static final IntegrationCodeWorkflowToolErrorType LIST = new IntegrationCodeWorkflowToolErrorType(103);
    public static final IntegrationCodeWorkflowToolErrorType UNSUPPORTED_LANGUAGE =
        new IntegrationCodeWorkflowToolErrorType(104);

    private IntegrationCodeWorkflowToolErrorType(int errorKey) {
        super(IntegrationCodeWorkflowToolErrorType.class, errorKey);
    }
}
