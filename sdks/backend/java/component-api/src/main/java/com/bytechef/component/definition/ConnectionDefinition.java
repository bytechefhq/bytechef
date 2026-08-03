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
import java.util.Map;
import java.util.Optional;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
public interface ConnectionDefinition {

    int VERSION_1 = 1;

    /**
     * Parameter key holding the base URI used as the prefix for the connection's HTTP requests.
     */
    String BASE_URI = "baseUri";

    /**
     * Returns whether an authorization must be configured for this connection to be usable.
     *
     * @return {@code true} if authorization is required, {@code false} if it is optional
     */
    default boolean getAuthorizationRequired() {
        return true;
    }

    /**
     * Returns the authorization schemes supported by this connection.
     *
     * @return the list of authorizations, or an empty list if none are defined
     */
    default List<? extends Authorization> getAuthorizations() {
        return List.of();
    }

    /**
     * Returns the optional function that resolves the base URI used as the prefix for the connection's HTTP requests.
     *
     * @return an {@code Optional} containing the {@link BaseUriFunction} if defined, or an empty {@code Optional}
     *         otherwise
     */
    default Optional<BaseUriFunction> getBaseUri() {
        return Optional.empty();
    }

    /**
     * Returns the optional {@link Help} content that provides additional guidance to users configuring the connection.
     *
     * @return an {@code Optional} containing the help content if defined, or an empty {@code Optional} otherwise
     */
    default Optional<Help> getHelp() {
        return Optional.empty();
    }

    /**
     * Returns the optional function used to process HTTP error responses and map them to {@link ProviderException}
     * instances. This hook is invoked when an HTTP response returns an error status code, allowing components to
     * provide custom error handling logic specific to their API.
     *
     * @return an {@link Optional} containing the {@link ProcessErrorResponseFunction} if defined, or empty otherwise
     */
    default Optional<ProcessErrorResponseFunction> getProcessErrorResponse() {
        return Optional.empty();
    }

    /**
     * Returns the input properties that define the parameters collected from the user to configure the connection.
     *
     * @return the list of properties, or an empty list if none are defined
     */
    default List<? extends Property> getProperties() {
        return List.of();
    }

    /**
     * TODO
     *
     * @return
     */
    default Optional<TestConsumer> getTest() {
        return Optional.empty();
    }

    /**
     * Returns the version of the connection definition, used to distinguish successive revisions.
     *
     * @return the connection version
     */
    default int getVersion() {
        return VERSION_1;
    }

    /**
     * Functional interface that resolves the base URI used as the prefix for a connection's HTTP requests, based on the
     * connection parameters.
     */
    @FunctionalInterface
    interface BaseUriFunction {

        /**
         * Resolves the base URI for the connection.
         *
         * @param connectionParameters the parameters of the connection
         * @param context              the context for the current call
         * @return the resolved base URI
         */
        String apply(Parameters connectionParameters, Context context);
    }

    /**
     * Functional interface that verifies a connection's parameters by performing a test call against the external
     * service, throwing an exception if the connection is not valid.
     */
    @FunctionalInterface
    interface TestConsumer {

        /**
         * Tests the connection using the given parameters.
         *
         * @param connectionParameters the parameters of the connection to test
         * @param context              the context for the current call
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
        ProviderException apply(int statusCode, Object body, Map<String, List<String>> headers, Context context)
            throws Exception;

    }
}
