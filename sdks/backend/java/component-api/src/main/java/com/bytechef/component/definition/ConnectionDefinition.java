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

package com.bytechef.component.definition;

import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Optional;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
public interface ConnectionDefinition {

    /**
     *
     */
    String BASE_URI = "baseUri";

    /**
     *
     * @return
     */
    Optional<Boolean> getAuthorizationRequired();

    /**
     *
     * @return
     */
    Optional<List<? extends Authorization>> getAuthorizations();

    /**
     *
     * @return
     */
    Optional<BaseUriFunction> getBaseUri();

    /**
     * Returns the optional function used to process HTTP error responses and map them to {@link ProviderException}
     * instances. This hook is invoked when an HTTP response returns an error status code, allowing components to
     * provide custom error handling logic specific to their API.
     *
     * @return an {@link Optional} containing the {@link ProcessErrorResponseFunction} if defined, or empty otherwise
     */
    Optional<ProcessErrorResponseFunction> getProcessErrorResponse();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

    /**
     * TODO
     *
     * @return
     */
    Optional<TestConsumer> getTest();

    /**
     *
     * @return
     */
    int getVersion();

    /**
     *
     */
    @FunctionalInterface
    interface BaseUriFunction {

        /**
         * @param connectionParameters
         * @param context
         * @return
         */
        String apply(Parameters connectionParameters, Context context);
    }

    /**
     *
     */
    @FunctionalInterface
    interface TestConsumer {

        /**
         * @param connectionParameters
         * @param context
         */
        void accept(Parameters connectionParameters, Context context);
    }

    /**
     * A functional interface for processing HTTP error responses and mapping them to {@link ProviderException}
     * instances. Implementations can extract error details from the response body and status code to create meaningful
     * exception messages.
     */
    @FunctionalInterface
    interface ProcessErrorResponseFunction {

        /**
         * Maps a failed HTTP response to a {@link ProviderException}.
         *
         * @param statusCode the HTTP status code of the error response
         * @param body       the response body associated with the error
         * @param context    the invocation context for the current call
         * @return a {@link ProviderException} (or subclass) representing the mapped remote error
         * @throws Exception if the error response cannot be mapped to a {@link ProviderException}
         */
        ProviderException apply(int statusCode, Object body, Context context) throws Exception;

    }
}
