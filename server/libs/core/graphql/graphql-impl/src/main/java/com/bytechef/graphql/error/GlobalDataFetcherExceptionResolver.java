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
 * <li>{@link ConfigurationException} \u2192 {@code BAD_REQUEST} \u2014 caller-input precondition/validation
 * failure.</li>
 * <li>Any other {@link AbstractException} \u2192 {@code INTERNAL_ERROR} \u2014 uncorrectable runtime failure.</li>
 * <li>{@link GraphQlBadRequestException} \u2192 {@code BAD_REQUEST} \u2014 controller-thrown input-shape
 * validation.</li>
 * <li>{@link AccessDeniedException} \u2192 {@code FORBIDDEN}. Returns a generic message (never the detail) so error
 * shape cannot be used to enumerate resources the caller does not own.</li>
 * <li>{@link AuthenticationCredentialsNotFoundException} \u2192 {@code UNAUTHORIZED} \u2014 protected mutation invoked
 * without a SecurityContext; distinct from {@code FORBIDDEN} so clients can prompt for re-authentication.</li>
 * </ul>
 *
 * <p>
 * For {@code AbstractException} subclasses, {@code entityClass}/{@code errorKey}/{@code errorCode} are forwarded to the
 * GraphQL error {@code extensions} map so clients can discriminate variants without string-matching message text.
 *
 * @author Ivica Cardic
 */
@Component
class GlobalDataFetcherExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GlobalDataFetcherExceptionResolver.class);

    private static final Set<String> SAFE_MESSAGE_FQCNS = Set.of(
        "com.bytechef.exception.AbstractException",
        "com.bytechef.exception.ConfigurationException",
        "java.lang.IllegalArgumentException");

    @Override
    protected @Nullable GraphQLError resolveToSingleError(Throwable throwable, DataFetchingEnvironment environment) {
        if (throwable instanceof AbstractException abstractException) {
            log.error(abstractException.getMessage(), abstractException);

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

        log.warn(
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
