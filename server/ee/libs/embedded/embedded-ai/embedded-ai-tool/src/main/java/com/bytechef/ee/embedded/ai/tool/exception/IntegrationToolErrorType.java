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
public class IntegrationToolErrorType extends AbstractErrorType {

    public static final IntegrationToolErrorType CREATE_INTEGRATION = new IntegrationToolErrorType(100);
    public static final IntegrationToolErrorType DELETE_INTEGRATION = new IntegrationToolErrorType(101);
    public static final IntegrationToolErrorType GET_INTEGRATION = new IntegrationToolErrorType(102);
    public static final IntegrationToolErrorType LIST_INTEGRATIONS = new IntegrationToolErrorType(103);
    public static final IntegrationToolErrorType PUBLISH_INTEGRATION = new IntegrationToolErrorType(104);
    public static final IntegrationToolErrorType SEARCH_INTEGRATIONS = new IntegrationToolErrorType(105);
    public static final IntegrationToolErrorType UPDATE_INTEGRATION = new IntegrationToolErrorType(106);

    private IntegrationToolErrorType(int errorKey) {
        super(IntegrationToolErrorType.class, errorKey);
    }
}
