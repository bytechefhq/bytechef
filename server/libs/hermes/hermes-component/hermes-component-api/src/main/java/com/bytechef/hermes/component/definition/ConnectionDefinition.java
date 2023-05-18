
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.hermes.definition.Property;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
public sealed interface ConnectionDefinition permits ModifiableConnectionDefinition {

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
     *
     * @return
     */
    Optional<List<? extends Property<?>>> getProperties();

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
     * @return
     */
    boolean isAuthorizationRequired();

    /**
     *
     */
    @FunctionalInterface
    interface BaseUriFunction {

        /**
         *
         * @param connectionInputParameters
         * @return
         */
        String apply(Map<String, Object> connectionInputParameters);
    }

    /**
     *
     */
    @FunctionalInterface
    interface TestConsumer {

        /**
         *
         * @param connectionInputParameters
         */
        void accept(Map<String, Object> connectionInputParameters);
    }
}
