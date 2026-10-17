/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by AI Hub service-layer code when a requested entity does not exist. Mapped to HTTP 404 by the REST
 * controllers' {@code @ExceptionHandler} blocks.
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

    /**
     * Structured constructor: builds a default message of the form {@code "<resourceType> not found: <resourceId>"} and
     * exposes the components for controllers and dashboards.
     */
    public NotFoundException(String resourceType, Object resourceId) {
        super(resourceType + " not found: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Structured constructor with an explicit message; use when the default {@code "<resourceType> not found:"}
     * phrasing is wrong (e.g. "Artifact not found" with an opaque message used to deny ID enumeration).
     */
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
