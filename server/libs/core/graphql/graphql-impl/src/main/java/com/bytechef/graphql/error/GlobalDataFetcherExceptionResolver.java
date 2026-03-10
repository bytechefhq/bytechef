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

        return null;
    }

    private String getDeepestCauseMessage(Throwable throwable) {
        String message = throwable.getMessage();
        Throwable currentException = throwable;

        while (currentException.getCause() != null) {
            currentException = currentException.getCause();

            message = currentException.getMessage();
        }

        return message;
    }
}
