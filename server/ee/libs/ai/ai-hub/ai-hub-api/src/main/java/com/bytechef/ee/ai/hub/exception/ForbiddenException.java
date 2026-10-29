/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by AI Hub service-layer code when the caller is authenticated but does not own (or otherwise has no permission
 * to access) the targeted entity. Mapped to HTTP 403 by the REST controllers' {@code @ExceptionHandler} blocks.
 *
 * <p>
 * Carries optional structured fields ({@code resourceType}, {@code resourceId}) so the controller layer can render a
 * stable error envelope without parsing the message string. New call sites should prefer the structured constructor;
 * the message-only constructors are retained for backward compatibility with the dozens of existing throw sites.
 * </p>
 *
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
