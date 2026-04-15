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

package com.bytechef.graphql.error;

import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ConfigurationException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Global GraphQL exception resolver that extracts meaningful error messages from ByteChef exceptions instead of
 * returning the default sanitized "INTERNAL_ERROR" message.
 *
 * <p>
 * Exception \u2192 error-type mapping:
 * <ul>
 * <li>{@link ConfigurationException} \u2192 {@code BAD_REQUEST}. Represents a precondition/validation failure caused by
 * caller input (e.g., {@code ALREADY_MEMBER}, {@code LAST_ADMIN_PROTECTED}, {@code INVALID_ROLE}). Surfaces as a
 * 4xx-equivalent classification so clients can treat it as a user error rather than a server bug.</li>
 * <li>Any other {@link AbstractException} \u2192 {@code INTERNAL_ERROR}. Typically indicates a runtime failure that the
 * caller cannot correct (e.g., {@code ExecutionException}).</li>
 * <li>{@link GraphQlBadRequestException} \u2192 {@code BAD_REQUEST}. Controller-thrown input-shape validation.</li>
 * <li>{@link AccessDeniedException} \u2192 {@code FORBIDDEN}. Raised by Spring Security {@code @PreAuthorize} denials.
 * Returns a generic message (never the exception detail) so error shape alone cannot be used to enumerate resources the
 * caller does not own. The permission-audit aspect has already recorded the denial; the client only needs to know the
 * classification.</li>
 * <li>{@link AuthenticationCredentialsNotFoundException} \u2192 {@code UNAUTHORIZED}. Raised when a protected GraphQL
 * mutation is invoked without a SecurityContext. Mapped distinctly from {@code FORBIDDEN} so clients can prompt for
 * re-authentication rather than signalling a permission error.</li>
 * </ul>
 *
 * <p>
 * For {@code AbstractException} subclasses, the structured {@code entityClass}, {@code errorKey}, and {@code errorCode}
 * fields are forwarded to the GraphQL error {@code extensions} map so clients can discriminate between error variants
 * (e.g., {@code ALREADY_MEMBER} vs {@code LAST_ADMIN_PROTECTED}) without string-matching on message text.
 *
 * @author Ivica Cardic
 */
@Component
class GlobalDataFetcherExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDataFetcherExceptionResolver.class);

    // Allowlist of exception types whose getMessage() is safe to forward to clients. Driver and infrastructure
    // exception messages often contain SQL state, bind parameters, schema/table names, JDBC URLs, or fully
    // qualified class paths that leak implementation detail. Anything not on this list is surfaced as the
    // outer AbstractException message only — callers who need driver detail can grep server logs by request id.
    private static final Set<String> SAFE_MESSAGE_FQCNS = Set.of(
        "com.bytechef.exception.AbstractException",
        "com.bytechef.exception.ConfigurationException",
        "java.lang.IllegalArgumentException");

    @Override
    protected @Nullable GraphQLError resolveToSingleError(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof AbstractException abstractException) {
            logger.error(abstractException.getMessage(), abstractException);

            String detailMessage = getDeepestCauseMessage(abstractException);
            ErrorType errorType = abstractException instanceof ConfigurationException
                ? ErrorType.BAD_REQUEST
                : ErrorType.INTERNAL_ERROR;

            Map<String, Object> extensions = new HashMap<>();

            extensions.put("entityClass", abstractException.getEntityClass()
                .getSimpleName());
            extensions.put("errorKey", abstractException.getErrorKey());
            extensions.put("errorCode", abstractException.getErrorMessageCode());

            return GraphqlErrorBuilder
                .newError(environment)
                .message(detailMessage)
                .errorType(errorType)
                .extensions(extensions)
                .build();
        }

        // Client-side input problems should surface as BAD_REQUEST. Scoped to GraphQlBadRequestException
        // (lives in graphql-api) so we don't accidentally translate server-side programmer errors
        // (aspect IAE, SpEL evaluation IAE, etc.) into 4xx responses. Propagate the structured code
        // and extensions into the GraphQL error payload so clients can branch on code rather than
        // scraping message text.
        if (throwable instanceof GraphQlBadRequestException badRequest) {
            Map<String, Object> extensions = new LinkedHashMap<>(badRequest.getExtensions());

            extensions.putIfAbsent("code", badRequest.getCode());

            return GraphqlErrorBuilder
                .newError(environment)
                .message(throwable.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .extensions(extensions)
                .build();
        }

        // Distinguish unauthenticated (401/UNAUTHORIZED) from unauthorized (403/FORBIDDEN) so clients can route
        // re-authentication prompts correctly. Both land here because Spring Security raises them from @PreAuthorize
        // / method security interceptors, and without translation Spring GraphQL would tag the response as
        // INTERNAL_ERROR and leak the exception class/stack to the caller.
        //
        // Messages stay intentionally generic so error shape alone cannot be used to enumerate resources. Clients
        // that need to discriminate between failure modes (for routing UI behavior or telemetry) should branch on
        // extensions.errorCode, which carries a stable code string, rather than parsing the message text.
        if (throwable instanceof AuthenticationCredentialsNotFoundException) {
            return GraphqlErrorBuilder
                .newError(environment)
                .message("Authentication required")
                .errorType(ErrorType.UNAUTHORIZED)
                .extensions(Map.of("errorCode", "AUTHENTICATION_REQUIRED"))
                .build();
        }

        if (throwable instanceof AccessDeniedException) {
            return GraphqlErrorBuilder
                .newError(environment)
                .message("Access denied")
                .errorType(ErrorType.FORBIDDEN)
                .extensions(Map.of("errorCode", "ACCESS_DENIED"))
                .build();
        }

        // Unmapped throwable: let Spring GraphQL apply the default INTERNAL_ERROR response, but leave a
        // server-side breadcrumb so operators can correlate a generic client error with the real cause.
        // Without this log, NullPointerException or uncaught RuntimeException inside a data fetcher would
        // surface to the client as an opaque "INTERNAL_ERROR" with no pointer to the failing exception type.
        logger.warn(
            "Unmapped throwable from data fetcher ({}): {}",
            throwable.getClass()
                .getName(),
            throwable.getMessage(),
            throwable);

        return null;
    }

    /**
     * Walks the cause chain and returns the deepest cause message whose exception type is on
     * {@link #SAFE_MESSAGE_FQCNS}. Falls back to the outer exception's own message (still a ByteChef
     * {@link AbstractException}, always safe by construction) when no deeper cause is allowlisted. Prevents raw
     * {@link java.sql.SQLException} / {@link org.springframework.dao.DataAccessException} messages — which routinely
     * contain bind parameters, schema names, and JDBC URLs — from leaking into client-visible GraphQL error payloads.
     */
    private String getDeepestCauseMessage(Throwable throwable) {
        String outerMessage = throwable.getMessage();
        String lastSafeMessage = (outerMessage != null && !outerMessage.isBlank()) ? outerMessage : null;
        Throwable currentException = throwable;

        while (currentException.getCause() != null) {
            currentException = currentException.getCause();

            String message = currentException.getMessage();

            if (message == null || message.isBlank()) {
                continue;
            }

            if (isSafeToForward(currentException)) {
                lastSafeMessage = message;
            }
        }

        if (lastSafeMessage != null) {
            return lastSafeMessage;
        }

        return throwable.toString();
    }

    private static boolean isSafeToForward(Throwable throwable) {
        Class<?> exceptionClass = throwable.getClass();

        while (exceptionClass != null && exceptionClass != Throwable.class) {
            if (SAFE_MESSAGE_FQCNS.contains(exceptionClass.getName())) {
                return true;
            }

            exceptionClass = exceptionClass.getSuperclass();
        }

        return false;
    }
}
