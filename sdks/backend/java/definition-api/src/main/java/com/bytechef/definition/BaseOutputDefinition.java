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

package com.bytechef.definition;

import com.bytechef.definition.BaseProperty.BaseValueProperty;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the output produced by an action or trigger, either as a statically declared response (schema plus sample
 * data) or as a function that computes the output dynamically at runtime.
 *
 * @author Ivica Cardic
 */
public interface BaseOutputDefinition {

    default Optional<? extends BaseOutputFunction> getOutput() {
        return Optional.empty();
    }

    default Optional<OutputResponse> getOutputResponse() {
        return Optional.empty();
    }

    default BaseValueProperty<?> getOutputSchema() {
        return getOutputResponse()
            .map(OutputResponse::getOutputSchema)
            .orElse(null);
    }

    default Object getSampleOutput() {
        return getOutputResponse()
            .map(OutputResponse::getSampleOutput)
            .orElse(null);
    }

    final class OutputResponse {

        private BaseValueProperty<?> outputSchema;
        private Object sampleOutput;
        private Object placeholder;

        private OutputResponse() {
        }

        private OutputResponse(BaseValueProperty<?> outputSchema, Object sampleOutput, Object placeholder) {
            this.outputSchema = outputSchema;
            this.sampleOutput = sampleOutput;
            this.placeholder = placeholder;
        }

        private OutputResponse(BaseValueProperty<?> outputSchema) {
            this(outputSchema, null, null);
        }

        private OutputResponse(Object sampleOutput) {
            this(null, sampleOutput, null);
        }

        public static OutputResponse of(BaseValueProperty<?> outputSchema) {
            return new OutputResponse(outputSchema);
        }

        public static OutputResponse of(Object sampleOutput) {
            return new OutputResponse(sampleOutput);
        }

        public static OutputResponse of(BaseValueProperty<?> outputSchema, Object sampleOutput) {
            return new OutputResponse(outputSchema, sampleOutput, null);
        }

        public static OutputResponse of(BaseValueProperty<?> outputSchema, Object sampleOutput, Object placeholder) {
            return new OutputResponse(outputSchema, sampleOutput, placeholder);
        }

        public BaseValueProperty<?> getOutputSchema() {
            return outputSchema;
        }

        public Object getSampleOutput() {
            return sampleOutput;
        }

        public Object getPlaceholder() {
            return placeholder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            var that = (OutputResponse) obj;

            return Objects.equals(this.outputSchema, that.outputSchema) &&
                Objects.equals(this.sampleOutput, that.sampleOutput) &&
                Objects.equals(this.placeholder, that.placeholder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outputSchema, sampleOutput, placeholder);
        }

        @Override
        public String toString() {
            return "OutputResponse[" +
                "outputSchema=" + outputSchema + ", " +
                "sampleOutput=" + sampleOutput + ", " +
                "placeholder=" + placeholder + ']';
        }
    }

    /**
     * Carries the property that describes the structure of an action's or trigger's output.
     *
     * @param outputSchema the property describing the structure of the output
     * @param <P>          the concrete {@link BaseValueProperty} type of the output schema
     */
    record OutputSchema<P extends BaseValueProperty<?>>(P outputSchema) {
    }

    /**
     * Carries a placeholder value shown before real output data is available.
     *
     * @param placeholder the placeholder value
     */
    record Placeholder(Object placeholder) {
    }

    /**
     * Carries a sample of the data produced as output.
     *
     * @param sampleOutput the sample output value
     */
    record SampleOutput(Object sampleOutput) {
    }
}
