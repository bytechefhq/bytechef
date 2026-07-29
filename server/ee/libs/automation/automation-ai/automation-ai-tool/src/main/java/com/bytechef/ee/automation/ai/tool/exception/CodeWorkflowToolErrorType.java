/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.tool.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowToolErrorType extends AbstractErrorType {

    public static final CodeWorkflowToolErrorType CREATE = new CodeWorkflowToolErrorType(100);
    public static final CodeWorkflowToolErrorType UPDATE_SOURCE = new CodeWorkflowToolErrorType(101);
    public static final CodeWorkflowToolErrorType GET_SOURCE = new CodeWorkflowToolErrorType(102);
    public static final CodeWorkflowToolErrorType LIST = new CodeWorkflowToolErrorType(103);
    public static final CodeWorkflowToolErrorType UNSUPPORTED_LANGUAGE = new CodeWorkflowToolErrorType(104);

    private CodeWorkflowToolErrorType(int errorKey) {
        super(CodeWorkflowToolErrorType.class, errorKey);
    }
}
