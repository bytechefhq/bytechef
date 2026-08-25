/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.exception;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 */
public class VariableErrorType extends AbstractErrorType {

    public static final VariableErrorType VARIABLE_NAME_INVALID = new VariableErrorType(100);
    public static final VariableErrorType VARIABLE_VALUE_TOO_LONG = new VariableErrorType(101);
    public static final VariableErrorType VARIABLE_NAME_ALREADY_EXISTS = new VariableErrorType(102);
    public static final VariableErrorType VARIABLE_NOT_FOUND = new VariableErrorType(103);
    public static final VariableErrorType VARIABLE_VALUE_REQUIRED = new VariableErrorType(104);

    public VariableErrorType(int errorKey) {
        super(Variable.class, errorKey);
    }
}
