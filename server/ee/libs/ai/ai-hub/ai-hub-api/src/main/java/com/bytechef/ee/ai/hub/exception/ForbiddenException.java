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
public class ForbiddenException extends RuntimeException {

    private final @Nullable String resourceType;
    private final @Nullable Object resourceId;

    public ForbiddenException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Structured constructor with an explicit message; controllers should pass an opaque user-facing string (do not
     * leak whether the resource exists) while keeping the structured fields available for audit logging.
     */
    public ForbiddenException(String resourceType, Object resourceId, String message) {
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
