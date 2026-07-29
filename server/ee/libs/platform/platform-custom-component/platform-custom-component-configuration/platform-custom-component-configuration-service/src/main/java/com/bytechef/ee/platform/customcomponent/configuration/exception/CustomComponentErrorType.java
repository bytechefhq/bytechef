/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.exception;

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomComponentErrorType extends AbstractErrorType {

    public static final CustomComponentErrorType JAVA_CUSTOM_COMPONENT_UPLOAD_DISABLED =
        new CustomComponentErrorType(100);

    public static final CustomComponentErrorType JAVA_SOURCE_NOT_EDITABLE = new CustomComponentErrorType(101);

    public static final CustomComponentErrorType SOURCE_RENAME_UNSUPPORTED = new CustomComponentErrorType(102);

    public static final CustomComponentErrorType LANGUAGE_NOT_SUPPORTED = new CustomComponentErrorType(103);

    public static final CustomComponentErrorType COMPONENT_ALREADY_EXISTS = new CustomComponentErrorType(104);

    public static final CustomComponentErrorType INVALID_COMPONENT_NAME = new CustomComponentErrorType(105);

    private CustomComponentErrorType(int errorKey) {
        super(CustomComponent.class, errorKey);
    }
}
