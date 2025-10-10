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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseOutputFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.ValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class OutputDefinition implements BaseOutputDefinition {

    private TaskDispatcherDefinition.OutputFunction output;
    private OutputResponse outputResponse;

    public OutputDefinition(ValueProperty<?> outputSchema) {
        Objects.requireNonNull(outputSchema, "'outputSchema' mut not be null");

        this.outputResponse = OutputResponse.of(outputSchema);
    }

    public OutputDefinition(ValueProperty<?> outputSchema, Object sampleOutput) {
        Objects.requireNonNull(outputSchema, "'outputSchema' mut not be null");
        Objects.requireNonNull(sampleOutput, "'sampleOutput' mut not be null");

        this.outputResponse = OutputResponse.of(outputSchema, sampleOutput);
    }

    public OutputDefinition(TaskDispatcherDefinition.OutputFunction output) {
        Objects.requireNonNull(output, "'output' mut not be null");

        this.output = output;
    }

    @Override
    public Optional<BaseOutputFunction> getOutput() {
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
