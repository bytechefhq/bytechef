/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

import org.jspecify.annotations.Nullable;

/**
 * Thrown by AI Hub service-layer code when an operation collides with the current state of the system (for example, a
 * thread id already bound to a different workspace). Mapped to HTTP 409 by the REST controllers'
 * {@code @ExceptionHandler} blocks.
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

    /**
     * Structured constructor with an explicit message describing the collision (existing tests / dashboards rely on the
     * message text, so we don't auto-derive one from {@code resourceType}/{@code resourceId}).
     *
     * <p>
     * {@code resourceId} is declared as {@code String} (not {@code Object}) so callers cannot accidentally hand in a
     * {@code Map} or other arbitrarily-shaped payload that would JSON-serialize unpredictably in the controller
     * envelope. Numeric ids should be stringified at the call site.
     * </p>
     */
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
