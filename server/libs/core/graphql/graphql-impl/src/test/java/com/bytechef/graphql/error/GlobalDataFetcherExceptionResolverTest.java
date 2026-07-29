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

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.graphql.execution.ErrorType;

/**
 * Resolver-level tests for {@link GlobalDataFetcherExceptionResolver}, pinning the two behaviors that matter for the
 * error-workflow validation fix: a {@link GraphQlBadRequestException} is mapped to {@code BAD_REQUEST} with its message
 * forwarded verbatim, while an unrelated bare {@link IllegalArgumentException} (a programmer error from some other code
 * path, not wrapped by a controller) is left unmapped -- confirming the fix is scoped to
 * {@link GraphQlBadRequestException} rather than reclassifying every {@link IllegalArgumentException} in the codebase.
 *
 * @author Ivica Cardic
 */
class GlobalDataFetcherExceptionResolverTest {

    private final GlobalDataFetcherExceptionResolver resolver = new GlobalDataFetcherExceptionResolver();

    @Test
    void testResolveToSingleErrorMapsGraphQlBadRequestExceptionToBadRequest() {
        GraphQlBadRequestException exception = new GraphQlBadRequestException(
            "A workflow cannot be its own error workflow",
            new IllegalArgumentException("A workflow cannot be its own error workflow"));

        GraphQLError error = resolver.resolveToSingleError(exception, newEnvironment());

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).isEqualTo("A workflow cannot be its own error workflow");
        assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @Test
    void testResolveToSingleErrorLeavesUnrelatedIllegalArgumentExceptionUnmapped() {
        IllegalArgumentException exception = new IllegalArgumentException("some unrelated programmer error");

        GraphQLError error = resolver.resolveToSingleError(exception, newEnvironment());

        assertThat(error).isNull();
    }

    private static DataFetchingEnvironment newEnvironment() {
        DataFetchingEnvironment environment = Mockito.mock(DataFetchingEnvironment.class);

        Mockito.when(environment.getField())
            .thenReturn(new Field("someField"));
        Mockito.when(environment.getExecutionStepInfo())
            .thenReturn(Mockito.mock(ExecutionStepInfo.class));

        return environment;
    }
}
