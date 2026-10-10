/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import com.bytechef.exception.ErrorType;
import com.bytechef.exception.ExecutionException;
import org.slf4j.Logger;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 * @author Ivica Cardic
 */
final class EmbeddedToolFailures {

    private EmbeddedToolFailures() {
    }

    static ExecutionException toExecutionException(
        Logger log, String invocation, String failureMessage, RuntimeException exception, ErrorType errorType) {

        if (exception instanceof ExecutionException executionException) {
            return executionException;
        }

        if (exception instanceof AccessDeniedException) {
            log.warn("{}: access denied: {}", invocation, exception.getMessage());

            return new ExecutionException(
                failureMessage + ": not permitted, the current user lacks permission for this operation", exception,
                errorType);
        }

        log.error("{}: {}", invocation, failureMessage, exception);

        return new ExecutionException(failureMessage + ": " + describe(exception), exception, errorType);
    }

    private static String describe(RuntimeException exception) {
        String message = exception.getMessage();

        if (message != null) {
            return message;
        }

        Class<? extends RuntimeException> exceptionClass = exception.getClass();

        return exceptionClass.getSimpleName();
    }
}
