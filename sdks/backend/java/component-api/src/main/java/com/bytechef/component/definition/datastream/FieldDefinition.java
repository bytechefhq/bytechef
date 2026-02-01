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

package com.bytechef.component.definition.datastream;

/**
 * Describes a single column/field exposed by a SOURCE or DESTINATION component.
 *
 * @param name  Column identifier (e.g., "firstName")
 * @param label Display label (e.g., "First Name")
 * @param type  Java type (String.class, Integer.class, etc.)
 * @author Ivica Cardic
 */
public record FieldDefinition(String name, String label, Class<?> type) {

    public FieldDefinition(String name, String label) {
        this(name, label, String.class);
    }

    public FieldDefinition(String name) {
        this(name, name, String.class);
    }
}
