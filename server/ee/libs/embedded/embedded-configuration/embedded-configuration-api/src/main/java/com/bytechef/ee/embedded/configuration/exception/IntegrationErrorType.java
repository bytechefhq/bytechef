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
 * @author Ivica Cardic
 */
public class IntegrationErrorType extends AbstractErrorType {

    public static final IntegrationErrorType CREATE_INTEGRATION = new IntegrationErrorType(100);
    public static final IntegrationErrorType DELETE_INTEGRATION = new IntegrationErrorType(101);
    public static final IntegrationErrorType GET_INTEGRATION = new IntegrationErrorType(102);
    public static final IntegrationErrorType LIST_INTEGRATIONS = new IntegrationErrorType(103);
    public static final IntegrationErrorType PUBLISH_INTEGRATION = new IntegrationErrorType(104);
    public static final IntegrationErrorType SEARCH_INTEGRATIONS = new IntegrationErrorType(105);
    public static final IntegrationErrorType UPDATE_INTEGRATION = new IntegrationErrorType(106);

    private IntegrationErrorType(int errorKey) {
        super(Integration.class, errorKey);
    }
}
