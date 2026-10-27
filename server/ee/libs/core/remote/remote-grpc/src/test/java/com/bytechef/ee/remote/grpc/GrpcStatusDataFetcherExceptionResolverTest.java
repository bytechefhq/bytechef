/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import io.grpc.Status;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GrpcStatusDataFetcherExceptionResolverTest {

    private final GrpcStatusDataFetcherExceptionResolver grpcStatusDataFetcherExceptionResolver =
        new GrpcStatusDataFetcherExceptionResolver();

    @Test
    void testPermissionDeniedIsForbiddenLikeALocalDenial() {
        GraphQLError graphQLError = resolve(Status.PERMISSION_DENIED.withDescription("Only the creator can edit"));

        assertThat(graphQLError.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
        assertThat(graphQLError.getMessage()).isEqualTo("Access denied");
        assertThat(graphQLError.getExtensions()).containsEntry("errorCode", "ACCESS_DENIED");
    }

    @Test
    void testStatusesMapToGraphQlErrorTypes() {
        assertThat(resolve(Status.UNAUTHENTICATED).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
        assertThat(resolve(Status.INVALID_ARGUMENT.withDescription("Bad name")).getErrorType())
            .isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(resolve(Status.NOT_FOUND).getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(resolve(Status.UNAVAILABLE).getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
    }

    @Test
    void testInvalidArgumentKeepsItsReason() {
        assertThat(resolve(Status.INVALID_ARGUMENT.withDescription("Bad name")).getMessage()).isEqualTo("Bad name");
    }

    @Test
    void testOtherExceptionsAreLeftToTheNextResolver() {
        assertThat(
            grpcStatusDataFetcherExceptionResolver.resolveToSingleError(
                new IllegalStateException("boom"), dataFetchingEnvironment()))
                    .isNull();
    }

    private GraphQLError resolve(Status status) {
        GraphQLError graphQLError = grpcStatusDataFetcherExceptionResolver.resolveToSingleError(
            status.asRuntimeException(), dataFetchingEnvironment());

        assertThat(graphQLError).isNotNull();

        return graphQLError;
    }

    private static DataFetchingEnvironment dataFetchingEnvironment() {
        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(
                ExecutionStepInfo.newExecutionStepInfo()
                    .type(Scalars.GraphQLString)
                    .path(ResultPath.rootPath())
                    .build())
            .mergedField(MergedField.newMergedField(List.of(new Field("connections")))
                .build())
            .build();
    }
}
