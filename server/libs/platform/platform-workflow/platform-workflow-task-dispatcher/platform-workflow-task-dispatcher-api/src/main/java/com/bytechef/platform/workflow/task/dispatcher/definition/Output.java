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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import com.bytechef.definition.BaseOutput;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.ModifiableValueProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record Output(List<ModifiableValueProperty<?, ?>> properties, Object sampleOutput)
    implements BaseOutput<ObjectProperty> {

    public Output(ModifiableValueProperty<?, ?>... properties) {
        this(List.of(properties), null);
    }

    public Output(List<ModifiableValueProperty<?, ?>> properties) {
        this(properties, null);
    }

    public Output {
        Objects.requireNonNull(properties, "'properties' mut not be null");
    }

    @Override
    public ObjectProperty getOutputSchema() {
        return TaskDispatcherDSL.object()
            .properties(properties);
    }

    @Override
    public Object getSampleOutput() {
        return sampleOutput;
    }
}
