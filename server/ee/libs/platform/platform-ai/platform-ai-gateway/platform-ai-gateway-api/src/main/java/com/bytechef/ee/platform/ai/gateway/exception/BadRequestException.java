/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Structured 400-class failure that carries two distinct messages: a {@link #getSafeClientMessage() safe message}
 * surfaced to the client in the HTTP body, and the standard {@link #getMessage() message} used server-side for logs.
 * Mapped to HTTP 400 by the gateway's exception handler — only {@code safeClientMessage} reaches the wire.
 *
 * <p>
 * Motivation: the gateway's {@code IllegalArgumentException} handler historically echoes {@code getMessage()} to the
 * client. That convention is enforced only by Javadoc — a future caller throwing {@code IllegalArgumentException} with
 * internal entity ids, workspace ids, or column names would silently leak via the 400 body. Throwing
 * {@link BadRequestException} forces a deliberate split: the wire-safe message is wrapped in {@link SafeMessage} (a
 * value type whose factory runs a defense-in-depth screen against internal-context markers), the diagnostic detail is a
 * plain {@code String}. Reviewers see the distinction at the throw site.
 *
 * <p>
 * Marked {@code final} to satisfy SpotBugs CT_CONSTRUCTOR_THROW (the constructor's defensive {@code safeClientMessage}
 * blank check throws {@link IllegalArgumentException}; without {@code final}, a subclass partially-constructed via the
 * thrown ctor remains observable via finalization). The exception's role is a wire-shape contract, not a hierarchy
 * root, so disallowing subclassing costs nothing.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public final class BadRequestException extends RuntimeException {

    private final SafeMessage safeClientMessage;

    /**
     * @param safeClientMessage non-null wrapped message safe for client surfacing (no internal ids, no column names, no
     *                          caller-content echoing). Use {@link SafeMessage#of(String)} at the throw site to run the
     *                          defensive screen, or {@link SafeMessage#raw(String)} for the documented escape hatch.
     * @param internalDetail    server-side diagnostic message used for logging — may include internal context the
     *                          {@code safeClientMessage} cannot expose.
     */
    public BadRequestException(SafeMessage safeClientMessage, String internalDetail) {
        this(safeClientMessage, internalDetail, null);
    }

    public BadRequestException(SafeMessage safeClientMessage, String internalDetail, Throwable cause) {
        super(internalDetail, cause);

        if (safeClientMessage == null) {
            throw new IllegalArgumentException("safeClientMessage must not be null");
        }

        this.safeClientMessage = safeClientMessage;
    }

    public String getSafeClientMessage() {
        return safeClientMessage.value();
    }
}
