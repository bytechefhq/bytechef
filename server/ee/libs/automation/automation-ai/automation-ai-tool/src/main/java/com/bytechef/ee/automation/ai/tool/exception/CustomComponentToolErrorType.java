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
public class CustomComponentToolErrorType extends AbstractErrorType {

    public static final CustomComponentToolErrorType CREATE = new CustomComponentToolErrorType(100);
    public static final CustomComponentToolErrorType UPDATE_SOURCE = new CustomComponentToolErrorType(101);
    public static final CustomComponentToolErrorType DELETE = new CustomComponentToolErrorType(102);
    public static final CustomComponentToolErrorType GET_SOURCE = new CustomComponentToolErrorType(103);
    public static final CustomComponentToolErrorType LIST = new CustomComponentToolErrorType(104);
    public static final CustomComponentToolErrorType PUBLISH = new CustomComponentToolErrorType(105);

    private CustomComponentToolErrorType(int errorKey) {
        super(CustomComponentToolErrorType.class, errorKey);
    }
}
