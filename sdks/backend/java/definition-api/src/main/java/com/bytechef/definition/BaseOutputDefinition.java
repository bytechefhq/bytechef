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

package com.bytechef.definition;

import com.bytechef.definition.BaseProperty.BaseValueProperty;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface BaseOutputDefinition {

    Optional<BaseOutputFunction> getOutput();

    Optional<OutputResponse> getOutputResponse();

    default BaseValueProperty<?> getOutputSchema() {
        return getOutputResponse()
            .map(OutputResponse::outputSchema)
            .orElse(null);
    }

    default Object getSampleOutput() {
        return getOutputResponse()
            .map(OutputResponse::sampleOutput)
            .orElse(null);
    }

    record OutputResponse(BaseValueProperty<?> outputSchema, Object sampleOutput) {

        public OutputResponse(BaseValueProperty<?> outputSchema) {
            this(outputSchema, null);
        }

        public OutputResponse(Object sampleOutput) {
            this(null, sampleOutput);
        }
    }

    /**
     * @param outputSchema
     * @param <P>
     */
    record OutputSchema<P extends BaseValueProperty<?>>(P outputSchema) {
    }

    /**
     * @param sampleOutput
     */
    record SampleOutput(Object sampleOutput) {
    }
}
