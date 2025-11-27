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

package com.bytechef.component.notion.constant;

/**
 * @author Monika Kušter
 */
public enum NotionPropertyType {

    CHECKBOX("checkbox"),
    DATE("date"),
    EMAIL("email"),
    SELECT("select"),
    MULTI_SELECT("multi_select"),
    STATUS("status"),
    NUMBER("number"),
    PHONE_NUMBER("phone_number"),
    RICH_TEXT("rich_text"),
    TITLE("title"),
    URL("url"),
    FORMULA("formula"),
    RELATION("relation"),
    ROLLUP("rollup"),
    PEOPLE("people"),
    FILES("files"),
    CREATED_TIME("created_time"),
    CREATED_BY("created_by"),
    LAST_EDITED_TIME("last_edited_time"),
    LAST_EDITED_BY("last_edited_by"),;

    private final String name;

    NotionPropertyType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static NotionPropertyType getPropertyTypeByName(String name) {
        for (NotionPropertyType value : values()) {
            if (name.equals(value.getName())) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown column type: " + name);
    }
}
