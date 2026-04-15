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
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Global GraphQL exception resolver that extracts meaningful error messages from ByteChef exceptions instead of
 * returning the default sanitized "INTERNAL_ERROR" message.
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

            return GraphqlErrorBuilder
                .newError(environment)
                .message(detailMessage)
                .errorType(ErrorType.INTERNAL_ERROR)
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
