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

package com.bytechef.component.salesforce.util;

/**
 * @author Monika Kušter
 */
public enum FieldType {

    BOOLEAN("boolean"),
    DATE("date"),
    DATETIME("dateTime"),
    DOUBLE("double"),
    INT("int"),
    LONG("long"),
    STRING("string"),
    TIME("time"),
    ADDRESS("address"),
    EMAIL("email"),
    PERCENT("percent"),
    PHONE("phone"),
    PICKLIST("picklist"),
    TEXTAREA("textarea"),
    URL("url");

    private final String name;

    FieldType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FieldType getFieldType(String name) {
        for (FieldType value : values()) {
            if (name.equals(value.name)) {
                return value;
            }
        }

        return null;
    }
}
