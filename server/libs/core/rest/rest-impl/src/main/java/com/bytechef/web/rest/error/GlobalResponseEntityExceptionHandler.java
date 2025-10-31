/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.web.rest.error;

import com.bytechef.exception.AbstractException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The error response
 * follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalResponseEntityExceptionHandler extends AbstractResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalResponseEntityExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(AbstractException.class)
    public ResponseEntity<ProblemDetail> handleAbstractException(
        final AbstractException exception, final WebRequest request) {

        return ResponseEntity
            .of(
                createProblemDetail(
                    exception, HttpStatus.BAD_REQUEST, Map.of("inputParameters", exception.getInputParameters()),
                    request))
            .build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(
        final NoSuchElementException exception, final WebRequest request) {

        logger.error(exception.getMessage(), exception);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.NOT_FOUND, exception.getMessage(), null, null, request))
            .build();
    }

    @ExceptionHandler(Throwable.class)
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public ResponseEntity<ProblemDetail> handleAnyException(final Throwable throwable, final WebRequest request) {
        logger.error(throwable.getMessage(), throwable);

        Exception exception = getCauseException((Exception) throwable);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentExceptionException((IllegalArgumentException) exception, request);
        }

        return ResponseEntity
            .of(createProblemDetail(exception, httpStatus, exception.getMessage(), null, null, request))
            .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentExceptionException(
        final IllegalArgumentException illegalArgumentException, final WebRequest request) {

        logger.error(illegalArgumentException.getMessage(), illegalArgumentException);

        Exception exception = getCauseException(illegalArgumentException);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), null, null, request))
            .build();
    }

    @ExceptionHandler
    public ResponseEntity<ProblemDetail> handleConcurrencyFailureException(
        final ConcurrencyFailureException exception, final WebRequest request) {

        logger.error(exception.getMessage(), exception);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.CONFLICT, "Concurrency Failure", null, null, request))
            .build();
    }

    private static Exception getCauseException(Exception throwable) {
        Exception exception = throwable;

        while (exception.getCause() != null && exception.getCause() instanceof Exception cause) {
            exception = cause;
        }

        return exception;
    }
}
