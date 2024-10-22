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

package com.bytechef.component.baserow.constant;

/**
 * @author Monika Kušter
 */
public enum BaserowFieldType {

    TEXT("text"),
    BOOLEAN("boolean"),
    LONG_TEXT("long_text"),
    LINK_TO_TABLE("link_row"),
    NUMBER("number"),
    RATING("rating"),
    DATE("date"),
    LAST_MODIFIED("last_modified"),
    LAST_MODIFIED_BY("last_modified_by"),
    CREATED_ON("created_on"),
    CREATED_BY("created_by"),
    URL("url"),
    FILE("url"),
    SINGLE_SELECT("single_select"),
    MULTI_SELECT("multiple_select"),
    PHONE_NUMBER("phone_number"),
    COUNT("count"),
    ROLLUP("rollup"),
    LOOKUP("lookup"),
    MULTIPLE_COLLABORATORS("multiple_collaborators"),
    UUID("uuid"),
    AUTO_NUMBER("autonumber");

    private final String name;

    BaserowFieldType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static BaserowFieldType getBaserowFieldType(String name) {
        for (BaserowFieldType value : values()) {
            if (name.equals(value.name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown field type: " + name);
    }
}
