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

package com.bytechef.platform.component.handler.loader;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Ivica Cardic
 */
public enum ComponentHandlerLoaderKind {

    DEFAULT("default"), JDBC("jdbc"), OPEN_API("openapi");

    private final String value;

    ComponentHandlerLoaderKind(String value) {
        this.value = value;
    }

    @JsonCreator
    public static ComponentHandlerLoaderKind of(String value) {
        for (ComponentHandlerLoaderKind componentHandlerLoaderKind : values()) {
            if (componentHandlerLoaderKind.value.equals(value)) {
                return componentHandlerLoaderKind;
            }
        }

        throw new IllegalArgumentException("Unknown component loader kind: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
