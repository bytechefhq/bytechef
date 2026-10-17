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
public class ConflictException extends RuntimeException {

    private final @Nullable String resourceType;
    private final @Nullable String resourceId;

    public ConflictException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }

    public ConflictException(String resourceType, String resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public @Nullable String getResourceType() {
        return resourceType;
    }

    public @Nullable String getResourceId() {
        return resourceId;
    }
}
