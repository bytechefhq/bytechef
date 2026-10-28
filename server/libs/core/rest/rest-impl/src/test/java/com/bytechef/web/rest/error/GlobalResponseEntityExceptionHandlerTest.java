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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @Test
    void testHandleAnyExceptionTerminatesOnCyclicCauseChain() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second", first);

        first.initCause(second);

        ResponseEntity<ProblemDetail> responseEntity = assertTimeoutPreemptively(
            Duration.ofSeconds(5), () -> exceptionHandler.handleAnyException(first, newRequest()));

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    void testHandleAnyExceptionFindsAccessDeniedInCyclicCauseChain() {
        RuntimeException first = new RuntimeException("first");
        AccessDeniedException accessDeniedException = new AccessDeniedException("denied", first);

        first.initCause(accessDeniedException);

        ResponseEntity<ProblemDetail> responseEntity = assertTimeoutPreemptively(
            Duration.ofSeconds(5), () -> exceptionHandler.handleAnyException(first, newRequest()));

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
    }

    @Nested
    class AccessDenied {

        private MockMvc mockMvc;

        @BeforeEach
        void beforeEach() {
            mockMvc = MockMvcBuilders.standaloneSetup(new AccessDeniedTestController())
                .setControllerAdvice(new GlobalResponseEntityExceptionHandler())
                .addFilters(new ExceptionTranslationFilter(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testAuthenticatedCallerAccessDeniedReturnsForbiddenProblemDetail() throws Exception {
            setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

            mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        void testAuthenticatedCallerAuthorizationDeniedReturnsForbiddenProblemDetail() throws Exception {
            setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

            mockMvc.perform(get("/test/authorization-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        void testAuthenticatedCallerWrappedAccessDeniedReturnsForbiddenProblemDetail() throws Exception {
            setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

            mockMvc.perform(get("/test/wrapped-access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        void testAuthenticatedCallerDeeplyWrappedAccessDeniedReturnsForbiddenProblemDetail() throws Exception {
            setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

            mockMvc.perform(get("/test/deeply-wrapped-access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        void testNullAuthenticationAccessDeniedReturnsForbiddenProblemDetail() throws Exception {
            setAuthentication(null);

            mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Access denied"))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        @Test
        void testAnonymousCallerWrappedAccessDeniedReturnsUnauthorized() throws Exception {
            setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

            mockMvc.perform(get("/test/wrapped-access-denied"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void testAnonymousCallerAccessDeniedReturnsUnauthorized() throws Exception {
            setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

            mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void testAnonymousCallerAuthorizationDeniedReturnsUnauthorized() throws Exception {
            setAuthentication(
                new AnonymousAuthenticationToken(
                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

            mockMvc.perform(get("/test/authorization-denied"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void testRememberMeCallerAccessDeniedReturnsUnauthorized() throws Exception {
            setAuthentication(
                new RememberMeAuthenticationToken("key", "user", AuthorityUtils.createAuthorityList("ROLE_USER")));

            mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void testUnexpectedExceptionStillReturnsInternalServerError() throws Exception {
            setAuthentication(UsernamePasswordAuthenticationToken.authenticated("user", null, List.of()));

            mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    private static WebRequest newRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @RestController
    @RequestMapping("/test")
    static class AccessDeniedTestController {

        @GetMapping("/access-denied")
        public void accessDenied() {
            throw new AccessDeniedException("Integration instance 123 is not owned by the connected user");
        }

        @GetMapping("/authorization-denied")
        public void authorizationDenied() {
            throw new AuthorizationDeniedException("test authorization denied");
        }

        @GetMapping("/wrapped-access-denied")
        public void wrappedAccessDenied() {
            throw new IllegalStateException("wrapper", new AccessDeniedException("test access denied"));
        }

        @GetMapping("/deeply-wrapped-access-denied")
        public void deeplyWrappedAccessDenied() {
            throw new RuntimeException(
                new AccessDeniedException("test access denied", new IOException("disk read failed")));
        }

        @GetMapping("/unexpected")
        public void unexpected() {
            throw new IllegalStateException("unexpected failure");
        }
    }
}
