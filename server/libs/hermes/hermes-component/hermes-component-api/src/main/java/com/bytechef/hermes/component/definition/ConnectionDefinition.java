/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.definition.Property.InputProperty;
import java.util.List;
import java.util.Optional;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
public interface ConnectionDefinition {

    String BASE_URI = "baseUri";

    /**
     *
     * @return
     */
    boolean containsAuthorizations();

    /**
     *
     * @param authorizationName
     * @return
     */
    Authorization getAuthorization(String authorizationName);

    Optional<Boolean> getAuthorizationRequired();

    /**
     *
     * @return
     */
    Optional<List<? extends Authorization>> getAuthorizations();

    Optional<String> getComponentDescription();

    String getComponentName();

    Optional<String> getComponentTitle();

    /**
     *
     * @return
     */
    Optional<BaseUriFunction> getBaseUri();

    /**
     *
     * @return
     */
    Optional<List<? extends InputProperty>> getProperties();

    /**
     * TODO
     *
     * @return
     */
    Optional<TestConsumer> getTest();

    /**
     * TODO
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
        String apply(ParameterMap connectionParameters, Context context);
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
        void accept(ParameterMap connectionParameters, Context context);
    }
}
