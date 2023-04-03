
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableComponentOptionsDataSource.class)
public sealed interface ComponentPropertiesDataSource
    extends
    com.bytechef.hermes.definition.PropertiesDataSource permits ComponentDSL.ModifiableComponentPropertiesDataSource {

    /**
     * The function that returns a list of properties.
     *
     * @return The function implementation
     */
    PropertiesFunction getProperties();

    /**
     *
     */
    @FunctionalInterface
    interface PropertiesFunction {

        /**
         *
         * @param connection
         * @param inputParameters
         * @return
         */
        List<? extends Property<?>> apply(Context.Connection connection, InputParameters inputParameters);
    }
}
