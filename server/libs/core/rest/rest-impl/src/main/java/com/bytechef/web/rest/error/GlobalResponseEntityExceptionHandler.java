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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.DisconnectedClientHelper;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The error response
 * follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalResponseEntityExceptionHandler extends AbstractResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalResponseEntityExceptionHandler.class);

    /**
     * Request-scoped marker signalling that this handler has already produced (or attempted to produce) an error
     * response for the current request. It guards against unbounded recursion: rendering the error response can itself
     * fail, and routing that second failure back into this handler would otherwise loop forever.
     */
    private static final String ERROR_HANDLED_ATTRIBUTE =
        GlobalResponseEntityExceptionHandler.class.getName() + ".ERROR_HANDLED";

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

        log.error(exception.getMessage(), exception);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.NOT_FOUND, exception.getMessage(), null, null, request))
            .build();
    }

    /**
     * This method takes care of unhandled exceptions that were risen up during the request procession. Method takes
     * special care if the client closed the connection before the response is being sent. This method has infinite loop
     * protection. About client disconnection A client that has gone away (a browser that navigated off a page while a
     * static asset was still streaming, a dropped SSE connection, ...) is not a server error. There is no live socket
     * left to answer, so attempting to write a body would fail and only add noise. DisconnectedClientHelper recognizes
     * the disconnect exceptions of every supported servlet container, so this stays independent of the container
     * (Tomcat, Jetty, ...) and does not match exception types by name. Returning null tells Spring the exception was
     * handled without a body. Recursion guard: Producing the error response can itself throw (broken pipe mid-write, no
     * message converter for the negotiated content type, ...). Feeding that second failure back into this handler would
     * spin forever. Marking the request the first time means any subsequent pass for the same request stops here
     * instead of attempting another write. The marker lives on the request, so it is released with the request and
     * survives internal re-dispatches - unlike thread- or call-stack-based state.
     *
     * @param throwable
     * @param request
     * @return
     */
    @ExceptionHandler(Throwable.class)
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public ResponseEntity<ProblemDetail> handleAnyException(final Throwable throwable, final WebRequest request) {
        if (DisconnectedClientHelper.isClientDisconnectedException(throwable)) {
            log.debug("Client disconnected before the response was written: {}", throwable.getMessage());

            return null;
        }

        if (isErrorAlreadyHandled(request)) {
            log.warn(
                "Skipping repeated error handling for a request whose error response already failed: {}",
                throwable.getMessage());

            return null;
        }

        markErrorHandled(request);

        Exception exception = getCauseException((Exception) throwable);

        if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentExceptionException((IllegalArgumentException) exception, request);
        }

        log.error(throwable.getMessage(), throwable);

        return ResponseEntity
            .of(createProblemDetail(
                exception, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), null, null, request))
            .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentExceptionException(
        final IllegalArgumentException illegalArgumentException, final WebRequest request) {

        log.error(illegalArgumentException.getMessage(), illegalArgumentException);

        Exception exception = getCauseException(illegalArgumentException);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), null, null, request))
            .build();
    }

    @ExceptionHandler
    public ResponseEntity<ProblemDetail> handleConcurrencyFailureException(
        final ConcurrencyFailureException exception, final WebRequest request) {

        log.error(exception.getMessage(), exception);

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

    private static boolean isErrorAlreadyHandled(WebRequest request) {
        var errorHandledObject = request.getAttribute(ERROR_HANDLED_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (errorHandledObject == null) {
            return false;
        }

        return (Boolean) errorHandledObject;
    }

    private static void markErrorHandled(WebRequest request) {
        request.setAttribute(ERROR_HANDLED_ATTRIBUTE, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
    }

}
