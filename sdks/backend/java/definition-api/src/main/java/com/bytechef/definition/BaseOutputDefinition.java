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

    /**
     * Returns the function that computes the output dynamically at runtime, if one is defined.
     *
     * @return an {@link Optional} containing the output function, or an empty {@link Optional} if the output is
     *         declared statically
     */
    Optional<? extends BaseOutputFunction> getOutput();

    /**
     * Returns the statically declared output response containing the output schema and sample data, if one is defined.
     *
     * @return an {@link Optional} containing the output response, or an empty {@link Optional} if none is defined
     */
    Optional<OutputResponse> getOutputResponse();

    /**
     * Returns the output schema from the declared {@link OutputResponse}, if present.
     *
     * @return the output schema property, or {@code null} if no output response or schema is defined
     */
    default BaseValueProperty<?> getOutputSchema() {
        return getOutputResponse()
            .map(OutputResponse::getOutputSchema)
            .orElse(null);
    }

    /**
     * Returns the sample output value from the declared {@link OutputResponse}, if present.
     *
     * @return the sample output, or {@code null} if no output response or sample is defined
     */
    default Object getSampleOutput() {
        return getOutputResponse()
            .map(OutputResponse::getSampleOutput)
            .orElse(null);
    }

    /**
     * Represents a statically declared output response, bundling together the output schema, a sample output value, and
     * an optional placeholder used before real data is available.
     */
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

        /**
         * Creates an output response describing only the output schema.
         *
         * @param outputSchema the property describing the structure of the output
         * @return a new {@link OutputResponse} carrying the given schema
         */
        public static OutputResponse of(BaseValueProperty<?> outputSchema) {
            return new OutputResponse(outputSchema);
        }

        /**
         * Creates an output response describing only a sample output value.
         *
         * @param sampleOutput a sample of the data produced as output
         * @return a new {@link OutputResponse} carrying the given sample output
         */
        public static OutputResponse of(Object sampleOutput) {
            return new OutputResponse(sampleOutput);
        }

        /**
         * Creates an output response describing both the output schema and a sample output value.
         *
         * @param outputSchema the property describing the structure of the output
         * @param sampleOutput a sample of the data produced as output
         * @return a new {@link OutputResponse} carrying the given schema and sample output
         */
        public static OutputResponse of(BaseValueProperty<?> outputSchema, Object sampleOutput) {
            return new OutputResponse(outputSchema, sampleOutput, null);
        }

        /**
         * Creates an output response describing the output schema, a sample output value, and a placeholder.
         *
         * @param outputSchema the property describing the structure of the output
         * @param sampleOutput a sample of the data produced as output
         * @param placeholder  a placeholder value shown before real output data is available
         * @return a new {@link OutputResponse} carrying the given schema, sample output, and placeholder
         */
        public static OutputResponse of(BaseValueProperty<?> outputSchema, Object sampleOutput, Object placeholder) {
            return new OutputResponse(outputSchema, sampleOutput, placeholder);
        }

        /**
         * Returns the property describing the structure of the output.
         *
         * @return the output schema, or {@code null} if none is set
         */
        public BaseValueProperty<?> getOutputSchema() {
            return outputSchema;
        }

        /**
         * Returns the sample of the data produced as output.
         *
         * @return the sample output, or {@code null} if none is set
         */
        public Object getSampleOutput() {
            return sampleOutput;
        }

        /**
         * Returns the placeholder value shown before real output data is available.
         *
         * @return the placeholder, or {@code null} if none is set
         */
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
