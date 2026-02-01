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

package com.bytechef.component.definition.datastream;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * Interface for SOURCE and DESTINATION implementations to expose their columns/schema. Implementations decide how to
 * discover columns (static or dynamic).
 *
 * @author Ivica Cardic
 */
public interface FieldsProvider {

    /**
     * Returns the columns/schema this component exposes.
     *
     * @param inputParameters      The configured parameters for this element
     * @param connectionParameters Connection credentials if applicable
     * @param context              The cluster element context
     * @return List of column definitions
     */
    default List<FieldDefinition> getFields(
        Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context) {

        return List.of();
    }
}
