/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;

/**
 * Turns a failed remote call into the GraphQL error the same failure would give when it happens locally.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GrpcStatusDataFetcherExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GrpcStatusDataFetcherExceptionResolver.class);

    @Override
    protected @Nullable GraphQLError resolveToSingleError(Throwable throwable, DataFetchingEnvironment environment) {
        if (!(throwable instanceof StatusRuntimeException statusRuntimeException)) {
            return null;
        }

        Status status = statusRuntimeException.getStatus();

        return switch (status.getCode()) {
            case PERMISSION_DENIED -> GraphqlErrorBuilder.newError(environment)
                .message("Access denied")
                .errorType(ErrorType.FORBIDDEN)
                .extensions(Map.of("errorCode", "ACCESS_DENIED"))
                .build();
            case UNAUTHENTICATED -> GraphqlErrorBuilder.newError(environment)
                .message("Authentication required")
                .errorType(ErrorType.UNAUTHORIZED)
                .extensions(Map.of("errorCode", "AUTHENTICATION_REQUIRED"))
                .build();
            case INVALID_ARGUMENT -> GraphqlErrorBuilder.newError(environment)
                .message(getDescription(status))
                .errorType(ErrorType.BAD_REQUEST)
                .build();
            case NOT_FOUND -> GraphqlErrorBuilder.newError(environment)
                .message(getDescription(status))
                .errorType(ErrorType.NOT_FOUND)
                .build();
            default -> {
                log.error("Remote call failed with {}", status, statusRuntimeException);

                yield GraphqlErrorBuilder.newError(environment)
                    .message(getDescription(status))
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .build();
            }
        };
    }

    private static String getDescription(Status status) {
        String description = status.getDescription();

        if (description != null) {
            return description;
        }

        Status.Code code = status.getCode();

        return code.name();
    }
}
