/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.exception;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiCollectionErrorType extends AbstractErrorType {

    public static final ApiCollectionErrorType INVALID_WORKFLOW_TRIGGER_TYPE = new ApiCollectionErrorType(100);
    public static final ApiCollectionErrorType INVALID_CONTEXT_PATH = new ApiCollectionErrorType(101);

    private ApiCollectionErrorType(int errorKey) {
        super(ApiCollection.class, errorKey);
    }
}
