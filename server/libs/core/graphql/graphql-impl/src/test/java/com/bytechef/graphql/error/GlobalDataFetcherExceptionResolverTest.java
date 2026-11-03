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
import graphql.Scalars;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.MergedField;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

/**
 * @author Ivica Cardic
 */
class GlobalDataFetcherExceptionResolverTest {

    private final GlobalDataFetcherExceptionResolver globalDataFetcherExceptionResolver =
        new GlobalDataFetcherExceptionResolver();

    @Test
    void testIllegalArgumentExceptionResolvesToBadRequestWithItsMessage() {
        GraphQLError graphQLError = globalDataFetcherExceptionResolver.resolveToSingleError(
            new IllegalArgumentException(
                "The component 'infobip' does not contain the 'sendViberBotTextMessage' action."),
            createDataFetchingEnvironment());

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(graphQLError.getMessage())
            .isEqualTo("The component 'infobip' does not contain the 'sendViberBotTextMessage' action.");
    }

    @Test
    void testIllegalArgumentExceptionSubclassResolvesToBadRequest() {
        GraphQLError graphQLError = globalDataFetcherExceptionResolver.resolveToSingleError(
            new NumberFormatException("For input string: \"abc\""), createDataFetchingEnvironment());

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(graphQLError.getMessage()).isEqualTo("For input string: \"abc\"");
    }

    @Test
    void testGraphQlBadRequestExceptionKeepsItsCodeExtension() {
        GraphQLError graphQLError = globalDataFetcherExceptionResolver.resolveToSingleError(
            new GraphQlBadRequestException("ENUM_OUT_OF_RANGE", "Unknown value", Map.of()),
            createDataFetchingEnvironment());

        assertThat(graphQLError).isNotNull();
        assertThat(graphQLError.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(graphQLError.getExtensions()).containsEntry("code", "ENUM_OUT_OF_RANGE");
    }

    @Test
    void testUnmappedExceptionIsLeftToTheDefaultHandler() {
        GraphQLError graphQLError = globalDataFetcherExceptionResolver.resolveToSingleError(
            new IllegalStateException("internal detail"), createDataFetchingEnvironment());

        assertThat(graphQLError).isNull();
    }

    private static DataFetchingEnvironment createDataFetchingEnvironment() {
        MergedField mergedField = MergedField.newMergedField(new Field("workflowNodeParameterDisplayConditions"))
            .build();

        ExecutionStepInfo executionStepInfo = ExecutionStepInfo.newExecutionStepInfo()
            .type(Scalars.GraphQLString)
            .path(ResultPath.rootPath())
            .build();

        return DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
            .mergedField(mergedField)
            .executionStepInfo(executionStepInfo)
            .build();
    }
}
