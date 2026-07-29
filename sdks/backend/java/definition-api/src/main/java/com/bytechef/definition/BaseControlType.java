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

/**
 * Defines the type of user-interface control used to render and edit a property value, such as a text input, a select
 * box, a date picker, or a code editor.
 *
 * @author Ivica Cardic
 */
public interface BaseControlType {

    /**
     * Returns the unique name identifying this control type.
     *
     * @return the control type name
     */
    String name();
}
