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

import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseOutputFunction;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the output produced by an action, trigger, or cluster element. An output can be defined either lazily
 * through a {@link BaseOutputFunction} that computes the schema at runtime, or eagerly through an
 * {@link OutputResponse} built from a static output schema, a sample output value, and an optional placeholder.
 *
 * @author Ivica Cardic
 */
public final class OutputDefinition implements BaseOutputDefinition {

    private BaseOutputFunction output;
    private OutputResponse outputResponse;

    private OutputDefinition() {
    }

    private OutputDefinition(BaseOutputFunction output) {
        this.output = output;
    }

    private OutputDefinition(ValueProperty<?> outputSchema, Object sampleOutput, Object placeholder) {
        this.outputResponse = OutputResponse.of(outputSchema, sampleOutput, placeholder);
    }

    /**
     * Creates an empty output definition that carries neither an output function nor a static output response.
     *
     * @return a new empty {@link OutputDefinition}
     */
    public static OutputDefinition of() {
        return new OutputDefinition();
    }

    /**
     * Creates an output definition from a static output schema.
     *
     * @param outputSchema the property describing the shape of the output; must not be {@code null}
     * @return a new {@link OutputDefinition} carrying the given schema
     */
    public static OutputDefinition of(ValueProperty<?> outputSchema) {
        Objects.requireNonNull(outputSchema, "'outputSchema' must not be null");

        return of(outputSchema, null, null);
    }

    /**
     * Creates an output definition from a sample output value, whose schema can be inferred from the sample.
     *
     * @param sampleOutput a representative example of the output value; must not be {@code null}
     * @return a new {@link OutputDefinition} carrying the given sample output
     */
    public static OutputDefinition of(Object sampleOutput) {
        Objects.requireNonNull(sampleOutput, "'sampleOutput' must not be null");

        return of(null, sampleOutput, null);
    }

    /**
     * Creates an output definition from a static output schema together with a representative sample output value.
     *
     * @param outputSchema the property describing the shape of the output; must not be {@code null}
     * @param sampleOutput a representative example of the output value; must not be {@code null}
     * @return a new {@link OutputDefinition} carrying the given schema and sample
     */
    public static OutputDefinition of(ValueProperty<?> outputSchema, Object sampleOutput) {
        Objects.requireNonNull(outputSchema, "'outputSchema' must not be null");
        Objects.requireNonNull(sampleOutput, "'sampleOutput' must not be null");

        return of(outputSchema, sampleOutput, null);
    }

    /**
     * Creates an output definition from a static output schema, a sample output value, and a placeholder shown before
     * an output value is available.
     *
     * @param outputSchema the property describing the shape of the output
     * @param sampleOutput a representative example of the output value
     * @param placeholder  a placeholder value displayed while no actual output is present
     * @return a new {@link OutputDefinition} carrying the given schema, sample, and placeholder
     */
    public static OutputDefinition of(ValueProperty<?> outputSchema, Object sampleOutput, Object placeholder) {
        return new OutputDefinition(outputSchema, sampleOutput, placeholder);
    }

    /**
     * Creates an output definition backed by an action output function that computes the output schema at runtime.
     *
     * @param output the action output function; must not be {@code null}
     * @return a new {@link OutputDefinition} backed by the given function
     */
    public static OutputDefinition of(ActionDefinition.BaseOutputFunction output) {
        Objects.requireNonNull(output, "'output' must not be null");

        return new OutputDefinition(output);
    }

    /**
     * Creates an output definition backed by a cluster element output function that computes the output schema at
     * runtime.
     *
     * @param output the cluster element output function; must not be {@code null}
     * @return a new {@link OutputDefinition} backed by the given function
     */
    public static OutputDefinition of(ClusterElementDefinition.OutputFunction output) {
        Objects.requireNonNull(output, "'output' must not be null");

        return new OutputDefinition(output);
    }

    /**
     * Creates an output definition backed by a trigger output function that computes the output schema at runtime.
     *
     * @param output the trigger output function; must not be {@code null}
     * @return a new {@link OutputDefinition} backed by the given function
     */
    public static OutputDefinition of(TriggerDefinition.OutputFunction output) {
        Objects.requireNonNull(output, "'output' must not be null");

        return new OutputDefinition(output);
    }

    /**
     * Returns the output function that lazily computes the output schema, when this definition is function-backed.
     *
     * @return an {@link Optional} containing the output function, or an empty {@link Optional} if none was set
     */
    @Override
    public Optional<? extends BaseOutputFunction> getOutput() {
        return Optional.ofNullable(output);
    }

    /**
     * Returns the statically defined output response, when this definition was built from an eager schema or sample.
     *
     * @return an {@link Optional} containing the output response, or an empty {@link Optional} if none was set
     */
    @Override
    public Optional<OutputResponse> getOutputResponse() {
        return Optional.ofNullable(outputResponse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof OutputDefinition that)) {
            return false;
        }

        return Objects.equals(output, that.output) && Objects.equals(outputResponse, that.outputResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(output, outputResponse);
    }

    @Override
    public String toString() {
        return "OutputDefinition{" +
            ", output=" + output +
            ", outputResponse=" + outputResponse +
            '}';
    }

}
