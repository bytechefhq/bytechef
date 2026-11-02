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

package com.bytechef.commons.data.jdbc.wrapper;

import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
public record TypedValue(Object value, String classname) {

    private static final List<String> ALLOWED_CLASSNAME_PREFIXES = List.of("java.", "com.bytechef.");

    public TypedValue(Object value) {
        this(value, getClassname(value));
    }

    public static TypedValue read(ObjectMapper objectMapper, String json) {
        TypedValue typedValue = objectMapper.readValue(json, TypedValue.class);

        String classname = typedValue.classname();

        if (ALLOWED_CLASSNAME_PREFIXES.stream()
            .noneMatch(classname::startsWith)) {

            throw new IllegalArgumentException("Disallowed class in a stored value: " + classname);
        }

        try {
            return new TypedValue(objectMapper.convertValue(typedValue.value(), Class.forName(classname)), classname);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalArgumentException(
                "Unknown class in a stored value: " + classname, classNotFoundException);
        }
    }

    public String write(ObjectMapper objectMapper) {
        return objectMapper.writeValueAsString(this);
    }

    private static String getClassname(Object value) {
        Class<?> valueClass = value.getClass();

        return valueClass.getName();
    }
}
