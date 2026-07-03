/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

/**
 * Thrown when the upstream chat model rejects or errors on a title-generation request. Mapped to HTTP 503 by
 * {@code TaskApiController} so the client can distinguish "model unavailable" from "model returned a blank title" — the
 * former should toast a retryable error, the latter is the silent best-effort skip.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TitleGenerationFailedException extends RuntimeException {

    public TitleGenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
