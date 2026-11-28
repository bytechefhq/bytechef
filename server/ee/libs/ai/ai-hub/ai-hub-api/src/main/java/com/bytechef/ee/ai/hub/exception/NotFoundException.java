/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class NotFoundException extends RuntimeException {

    private final @Nullable String resourceType;
    private final @Nullable Object resourceId;

    public NotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }

    public NotFoundException(String resourceType, Object resourceId) {
        super(resourceType + " not found: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public NotFoundException(String resourceType, Object resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public @Nullable String getResourceType() {
        return resourceType;
    }

    public @Nullable Object getResourceId() {
        return resourceId;
    }
}
