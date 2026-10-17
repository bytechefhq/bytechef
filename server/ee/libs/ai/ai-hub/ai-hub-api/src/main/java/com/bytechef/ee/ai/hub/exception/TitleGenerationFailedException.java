/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

/**
 * Thrown when the upstream chat model rejects or errors on a title-generation request, so the client can distinguish
 * "model unavailable" from "model returned a blank title" — the former should toast a retryable error, the latter is
 * the silent best-effort skip handled by returning the chat row unchanged.
 *
 * <p>
 * Resolved by {@code AiHubGraphQlExceptionResolver} into a GraphQL error carrying
 * {@code extensions.errorCode = TITLE_GENERATION_FAILED}. It is thrown only on the GraphQL title-generation path; an
 * earlier version of this javadoc claimed {@code TaskApiController} mapped it to HTTP 503, which was never true — no
 * REST controller has ever referenced this type, and until that resolver existed it fell through unresolved as an
 * opaque {@code INTERNAL_ERROR}.
 * </p>
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
