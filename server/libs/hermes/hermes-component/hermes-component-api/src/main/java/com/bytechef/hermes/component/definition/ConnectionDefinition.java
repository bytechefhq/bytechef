
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

import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Optional;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableConnectionDefinition.class)
public sealed interface ConnectionDefinition permits ComponentDSL.ModifiableConnectionDefinition {

    String BASE_URI = "baseUri";

    boolean containsAuthorizations();

    Authorization getAuthorization(String authorizationName);

    List<? extends Authorization> getAuthorizations();

    BaseUriFunction getBaseUri();

    String getComponentName();

    Display getDisplay();

    String getName();

    List<? extends Property<?>> getProperties();

    Resources getResources();

    Optional<TestConsumer> getTest();

    int getVersion();

    boolean isAuthorizationRequired();

    /**
     *
     */
    @FunctionalInterface
    interface BaseUriFunction {

        /**
         *
         * @param connection
         * @return
         */
        String apply(Connection connection);
    }

    /**
     *
     */
    @FunctionalInterface
    interface TestConsumer {

        /**
         *
         * @param connection
         */
        void accept(Connection connection);
    }
}
