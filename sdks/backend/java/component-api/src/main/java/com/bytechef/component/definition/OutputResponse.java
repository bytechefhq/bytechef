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

package com.bytechef.component.definition;

import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.definition.BaseOutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record OutputResponse(ModifiableValueProperty<?, ?> outputSchema, Object sampleOutput)
    implements BaseOutputResponse<Property.ValueProperty<?>> {

    public OutputResponse(ModifiableValueProperty<?, ?> outputSchema) {
        this(outputSchema, null);
    }

    public OutputResponse {
        Objects.requireNonNull(outputSchema, "'outputSchema' mut not be null");
    }

    @Override
    public ModifiableValueProperty<?, ?> getOutputSchema() {
        return outputSchema;
    }

    @Override
    public Object getSampleOutput() {
        return sampleOutput;
    }
}
