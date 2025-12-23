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

import com.bytechef.component.definition.ActionDefinition.BaseOutputFunction;
import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.definition.BaseOutputDefinition;
import java.util.Objects;
import java.util.Optional;

/**
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

    public static OutputDefinition of() {
        return new OutputDefinition();
    }

    public static OutputDefinition of(ValueProperty<?> outputSchema) {
        Objects.requireNonNull(outputSchema, "'outputSchema' mut not be null");

        return of(outputSchema, null, null);
    }

    public static OutputDefinition of(Object sampleOutput) {
        Objects.requireNonNull(sampleOutput, "'sampleOutput' mut not be null");

        return of(null, sampleOutput, null);
    }

    public static OutputDefinition of(ValueProperty<?> outputSchema, Object sampleOutput) {
        Objects.requireNonNull(outputSchema, "'outputSchema' mut not be null");
        Objects.requireNonNull(sampleOutput, "'sampleOutput' mut not be null");

        return of(outputSchema, sampleOutput, null);
    }

    public static OutputDefinition of(ValueProperty<?> outputSchema, Object sampleOutput, Object placeholder) {
        return new OutputDefinition(outputSchema, sampleOutput, placeholder);
    }

    public static OutputDefinition of(BaseOutputFunction output) {
        Objects.requireNonNull(output, "'output' mut not be null");

        return new OutputDefinition(output);
    }

    public static OutputDefinition of(OutputFunction output) {
        Objects.requireNonNull(output, "'output' mut not be null");

        return new OutputDefinition(output);
    }

    @Override
    public Optional<? extends BaseOutputFunction> getOutput() {
        return Optional.ofNullable(output);
    }

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
