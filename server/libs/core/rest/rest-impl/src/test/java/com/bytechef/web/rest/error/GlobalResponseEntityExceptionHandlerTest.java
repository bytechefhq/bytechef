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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.EOFException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Verifies that {@link GlobalResponseEntityExceptionHandler#handleAnyException} silently drops client disconnects and
 * cannot recurse when producing the error response itself fails.
 */
class GlobalResponseEntityExceptionHandlerTest {

    private final GlobalResponseEntityExceptionHandler exceptionHandler = new GlobalResponseEntityExceptionHandler();

    @Test
    void testHandleAnyExceptionReturnsNullOnBrokenPipe() {
        Throwable throwable = new IOException("Broken pipe");

        assertNull(exceptionHandler.handleAnyException(throwable, newRequest()));
    }

    @Test
    void testHandleAnyExceptionReturnsNullOnWrappedBrokenPipe() {

        // Mirrors the production stack: ClientAbortException -> IOException: Broken pipe. Spring's
        // DisconnectedClientHelper inspects the most specific cause, so the wrapper does not hide the disconnect.

        Throwable throwable = new IllegalStateException("write failed", new IOException("Broken pipe"));

        assertNull(exceptionHandler.handleAnyException(throwable, newRequest()));
    }

    @Test
    void testHandleAnyExceptionReturnsNullOnEndOfFile() {
        Throwable throwable = new EOFException();

        assertNull(exceptionHandler.handleAnyException(throwable, newRequest()));
    }

    @Test
    void testHandleAnyExceptionReturnsProblemDetailOnGenericFailure() {
        Throwable throwable = new RuntimeException("unexpected failure");

        ResponseEntity<ProblemDetail> responseEntity = exceptionHandler.handleAnyException(throwable, newRequest());

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    void testHandleAnyExceptionDoesNotRecurseWhenRenderingErrorResponseFails() {

        // A single request whose error response failed to render must not be handled twice: the second pass through
        // the handler (as would happen if the failed write were routed back here) has to stop instead of looping.

        WebRequest webRequest = newRequest();

        ResponseEntity<ProblemDetail> firstResponse =
            exceptionHandler.handleAnyException(new RuntimeException("first failure"), webRequest);

        assertNotNull(firstResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, firstResponse.getStatusCode());

        ResponseEntity<ProblemDetail> secondResponse =
            exceptionHandler.handleAnyException(new RuntimeException("failure while writing first response"),
                webRequest);

        assertNull(secondResponse);
    }

    private static WebRequest newRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }
}
