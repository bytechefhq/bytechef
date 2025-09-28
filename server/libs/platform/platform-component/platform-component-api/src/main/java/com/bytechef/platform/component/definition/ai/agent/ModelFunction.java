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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.model.Model;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ModelFunction {

    /**
     *
     */
    ClusterElementType MODEL = new ClusterElementType("MODEL", "model", "Model", true);

    /**
     * @param inputParameters        The input parameters for the model function.
     * @param connectionParameters   The connection parameters required to access the model.
     * @param responseFormatRequired If {@code true}, the model's response should be formatted according to a specific
     *                               response format; if {@code false}, the raw response can be returned. Set to
     *                               {@code true} when a structured or standardized output is required from the model,
     *                               otherwise set to {@code false}.
     * @return The result of the model function.
     * @throws Exception If an error occurs during model execution.
     */
    Model<?, ?> apply(Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired)
        throws Exception;
}
