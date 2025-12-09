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

package com.bytechef.component.aitable.constant;

/**
 * @author Monika Domiter
 */
public enum FieldType {

    SINGLE_TEXT("SingleText"),
    TEXT("Text"),
    SINGLE_SELECT("SingleSelect"),
    MULTI_SELECT("MultiSelect"),
    NUMBER("Number"),
    CURRENCY("Currency"),
    PERCENT("Percent"),
    DATE_TIME("DateTime"),
    ATTACHMENT("Attachment"),
    MEMBER("Member"),
    CHECKBOX("Checkbox"),
    RATING("Rating"),
    URL("URL"),
    PHONE("Phone"),
    EMAIL("Email"),
    WORK_DOC("WorkDoc"),
    ONE_WAY_LINK("OneWayLink"),
    TWO_WAY_LINK("TwoWayLink"),
    MAGIC_LOOK_UP("MagicLookUp"),
    FORMULA("Formula"),
    AUTO_NUMBER("AutoNumber"),
    CREATED_TIME("CreatedTime"),
    LAST_MODIFIED_TIME("LastModifiedTime"),
    CREATED_BY("CreatedBy"),
    LAST_MODIFIED_BY("LastModifiedBy"),
    BUTTON("Button"),
    CASCADER("Cascader");

    private final String name;

    FieldType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static FieldType fromString(String text) {
        for (FieldType fieldType : values()) {
            if (fieldType.name.equalsIgnoreCase(text)) {
                return fieldType;
            }
        }

        throw new IllegalArgumentException("Not supported field type: " + text);
    }
}
