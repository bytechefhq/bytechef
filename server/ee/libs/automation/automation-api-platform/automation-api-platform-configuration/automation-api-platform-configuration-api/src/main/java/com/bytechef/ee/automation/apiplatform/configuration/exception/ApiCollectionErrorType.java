/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.exception;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.exception.AbstractErrorType;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApiCollectionErrorType extends AbstractErrorType {

    public static final ApiCollectionErrorType INVALID_WORKFLOW_TRIGGER_TYPE = new ApiCollectionErrorType(100);
    public static final ApiCollectionErrorType INVALID_CONTEXT_PATH = new ApiCollectionErrorType(101);
    public static final ApiCollectionErrorType WORKFLOW_NOT_FOUND = new ApiCollectionErrorType(103);
    public static final ApiCollectionErrorType PROJECT_VERSION_INVALID = new ApiCollectionErrorType(104);
    public static final ApiCollectionErrorType ENDPOINT_WORKFLOW_REMOVED = new ApiCollectionErrorType(105);
    public static final ApiCollectionErrorType API_COLLECTION_NOT_FOUND = new ApiCollectionErrorType(106);

    private static final Map<Integer, String> NAMES = Map.of(
        INVALID_WORKFLOW_TRIGGER_TYPE.getErrorKey(), "INVALID_WORKFLOW_TRIGGER_TYPE",
        INVALID_CONTEXT_PATH.getErrorKey(), "INVALID_CONTEXT_PATH",
        WORKFLOW_NOT_FOUND.getErrorKey(), "WORKFLOW_NOT_FOUND",
        PROJECT_VERSION_INVALID.getErrorKey(), "PROJECT_VERSION_INVALID",
        ENDPOINT_WORKFLOW_REMOVED.getErrorKey(), "ENDPOINT_WORKFLOW_REMOVED",
        API_COLLECTION_NOT_FOUND.getErrorKey(), "API_COLLECTION_NOT_FOUND");

    private ApiCollectionErrorType(int errorKey) {
        super(ApiCollection.class, errorKey);
    }

    /**
     * Returns the name of the constant carrying {@code errorKey}, or {@code null} when no constant does.
     */
    public static String nameOf(int errorKey) {
        return NAMES.get(errorKey);
    }
}
