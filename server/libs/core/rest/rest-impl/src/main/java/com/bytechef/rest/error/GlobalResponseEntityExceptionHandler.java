/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.rest.error;

import com.bytechef.rest.utils.HeaderUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The error response
 * follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice
public class GlobalResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalResponseEntityExceptionHandler.class);

    private final String applicationName;

    public GlobalResponseEntityExceptionHandler(@Value("${spring.applicationName}") String applicationName) {
        this.applicationName = applicationName;
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public ResponseEntity<ProblemDetail> handleAnyException(final Throwable throwable, final WebRequest request) {
        logger.error(throwable.getMessage(), throwable);

        return ResponseEntity
            .of(
                createProblemDetail(
                    throwable.getCause() == null ? (Exception) throwable : (Exception) throwable.getCause(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null, null, request))
            .build();
    }

    @ExceptionHandler
    public ResponseEntity<Object> handleBadRequestAlertException(
        final BadRequestAlertException exception, final WebRequest request) {

        return handleExceptionInternal(
            exception,
            createProblemDetail(exception, exception.getStatusCode(), exception.getMessage(), null, null, request),
            HeaderUtils.createFailureAlert(
                applicationName, true, exception.getEntityName(), exception.getErrorKey(), exception.getMessage()),
            exception.getStatusCode(), request);
    }

    @ExceptionHandler
    public ResponseEntity<ProblemDetail> handleConcurrencyFailureException(
        final ConcurrencyFailureException exception, final WebRequest request) {

        logger.error(exception.getMessage(), exception);

        return ResponseEntity
            .of(
                createProblemDetail(
                    exception, HttpStatus.CONFLICT, "Concurrency Failure", null, null, request))
            .build();
    }
}
