/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationErrorType extends AbstractErrorType {

    public static final IntegrationErrorType INTEGRATION_VERSION_NOT_FOUND = new IntegrationErrorType(100);
    public static final IntegrationErrorType INTEGRATION_VERSION_NOT_PUBLISHED = new IntegrationErrorType(101);

    private IntegrationErrorType(int errorKey) {
        super(Integration.class, errorKey);
    }
}
