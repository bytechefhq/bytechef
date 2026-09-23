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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * @author Ivica Cardic
 */
class GlobalDataFetcherExceptionResolverTest {

    private DataFetchingEnvironment environment;
    private GlobalDataFetcherExceptionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GlobalDataFetcherExceptionResolver();

        ResultPath resultPath = ResultPath.rootPath()
            .segment("workspaceConnection");

        ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(resultPath)
            .build();

        MergedField mergedField = MergedField.newMergedField(Field.newField("workspaceConnection")
            .build())
            .build();

        environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .executionStepInfo(executionStepInfo)
            .mergedField(mergedField)
            .build();
    }

    @Test
    void testAccessDeniedMapsToForbiddenWithAGenericMessage() {
        GraphQLError graphQLError = resolver.resolveToSingleError(
            new AccessDeniedException("Connection 42 in workspace 1051 is not accessible"), environment);

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType()).isEqualTo(org.springframework.graphql.execution.ErrorType.FORBIDDEN);
        assertThat(graphQLError.getMessage()).isEqualTo("Access denied");
        assertThat(graphQLError.getExtensions()).containsEntry("errorCode", "ACCESS_DENIED");
        assertThat(graphQLError.getPath()).isEqualTo(List.of("workspaceConnection"));
    }

    @Test
    void testAccessDeniedSubclassMapsToForbidden() {
        AccessDeniedException accessDeniedException = new AccessDeniedException("Project 9 denied") {};

        GraphQLError graphQLError = resolver.resolveToSingleError(accessDeniedException, environment);

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType()).isEqualTo(org.springframework.graphql.execution.ErrorType.FORBIDDEN);
        assertThat(graphQLError.getMessage()).isEqualTo("Access denied");
        assertThat(graphQLError.getExtensions()).containsEntry("errorCode", "ACCESS_DENIED");
    }

    @Test
    void testWrappedAccessDeniedMapsToForbidden() {
        // An asynchronous data fetcher can deliver the denial as the cause of another exception.
        RuntimeException wrappedException = new IllegalStateException(
            "fetch failed", new AccessDeniedException("Connection 42 in workspace 1051 is not accessible"));

        GraphQLError graphQLError = resolver.resolveToSingleError(wrappedException, environment);

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType()).isEqualTo(org.springframework.graphql.execution.ErrorType.FORBIDDEN);
        assertThat(graphQLError.getMessage()).isEqualTo("Access denied");
        assertThat(graphQLError.getExtensions()).containsEntry("errorCode", "ACCESS_DENIED");
    }

    @Test
    void testAnExceptionWithoutADenialInItsCauseChainIsNotMapped() {
        assertThat(resolver.resolveToSingleError(new IllegalStateException("unrelated"), environment)).isNull();
    }

    @Test
    void testMissingCredentialsMapToUnauthorized() {
        GraphQLError graphQLError = resolver.resolveToSingleError(
            new AuthenticationCredentialsNotFoundException("No SecurityContext for user 7"), environment);

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType())
            .isEqualTo(org.springframework.graphql.execution.ErrorType.UNAUTHORIZED);
        assertThat(graphQLError.getMessage()).isEqualTo("Authentication required");
        assertThat(graphQLError.getExtensions()).containsEntry("errorCode", "AUTHENTICATION_REQUIRED");
    }

    @Test
    void testConfigurationExceptionMapsToBadRequestWithErrorKey() {
        ErrorType errorType = new TestErrorType();

        GraphQLError graphQLError = resolver.resolveToSingleError(
            new ConfigurationException("Already a member", errorType), environment);

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType())
            .isEqualTo(org.springframework.graphql.execution.ErrorType.BAD_REQUEST);
        assertThat(graphQLError.getMessage()).isEqualTo("Already a member");
        assertThat(graphQLError.getExtensions())
            .containsEntry("errorKey", 123)
            .containsEntry("entityClass", "TestErrorType")
            .containsEntry("errorCode", "error.testErrorType.123");
    }

    private static final class TestErrorType implements ErrorType {

        @Override
        public Class<?> getErrorClass() {
            return TestErrorType.class;
        }

        @Override
        public int getErrorKey() {
            return 123;
        }
    }
}
