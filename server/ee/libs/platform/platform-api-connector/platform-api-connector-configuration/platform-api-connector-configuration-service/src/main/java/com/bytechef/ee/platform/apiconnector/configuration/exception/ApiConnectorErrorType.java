/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.exception;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiConnectorErrorType extends AbstractErrorType {

    public static final ApiConnectorErrorType INVALID_API_CONNECTOR_DEFINITION = new ApiConnectorErrorType(100);
    public static final ApiConnectorErrorType API_CONNECTOR_NAME_ALREADY_EXISTS = new ApiConnectorErrorType(101);
    public static final ApiConnectorErrorType API_CONNECTOR_VERSION_CONFLICT = new ApiConnectorErrorType(102);

    public ApiConnectorErrorType(int errorKey) {
        super(ApiConnector.class, errorKey);
    }
}
