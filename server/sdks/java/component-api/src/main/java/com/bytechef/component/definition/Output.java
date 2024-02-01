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

import static com.bytechef.component.definition.ComponentDSL.object;

import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Property.ObjectProperty;
import com.bytechef.definition.BaseOutput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record Output(
    List<? extends ModifiableValueProperty<?, ?>> properties, Map<String, Object> metadata, Object sampleOutput)
    implements BaseOutput<ObjectProperty> {

    public Output(ModifiableValueProperty<?, ?>... properties) {
        this(List.of(properties), null, null);
    }

    public Output(List<ModifiableValueProperty<?, ?>> properties) {
        this(properties, null, null);
    }

    public Output {
        Objects.requireNonNull(properties, "'properties' mut not be null");
    }

    @Override
    public ObjectProperty getOutputSchema() {
        return object()
            .properties(properties)
            .metadata(metadata);
    }

    @Override
    public Object getSampleOutput() {
        return sampleOutput;
    }
}
