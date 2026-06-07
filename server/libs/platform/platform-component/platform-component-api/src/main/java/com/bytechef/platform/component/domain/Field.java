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

package com.bytechef.platform.component.domain;

import com.bytechef.component.definition.datastream.FieldDefinition;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight transport-friendly view of {@link FieldDefinition} surfaced over GraphQL by the workflow-less
 * {@code clusterElementFields} query. The {@code type} is the simple class name of {@link FieldDefinition#type()} (e.g.
 * {@code String}, {@code Integer}, {@code Instant}) so the wizard can choose a sensible default for the indexed-field
 * type selector without needing the JVM type system.
 *
 * @author Ivica Cardic
 */
public record Field(String name, @Nullable String label, @Nullable String type) {

    public Field(FieldDefinition fieldDefinition) {
        this(
            fieldDefinition.name(), fieldDefinition.label(),
            fieldDefinition.type() != null ? fieldDefinition.type()
                .getSimpleName() : null);
    }
}
