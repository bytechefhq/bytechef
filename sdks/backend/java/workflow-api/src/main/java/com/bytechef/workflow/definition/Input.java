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

package com.bytechef.workflow.definition;

/**
 * An input a {@link WorkflowDefinition} accepts when it is executed.
 *
 * <p>
 * A declared input is what the platform prompts for in test configuration and what a caller's payload is checked
 * against; at run time it arrives under {@code input} in {@link TaskContext#input()}.
 *
 * @author Ivica Cardic
 */
public interface Input {

    /**
     * Returns the display label, or {@code null} to fall back to the name.
     */
    String getLabel();

    /**
     * Returns the name the value is passed under.
     */
    String getName();

    /**
     * Returns whether the workflow refuses to run without this input.
     */
    boolean isRequired();

    /**
     * Returns the input's type — {@code STRING}, {@code INTEGER}, {@code BOOLEAN}, {@code ARRAY}, {@code OBJECT} and
     * the rest of the platform's property types.
     */
    String getType();
}
