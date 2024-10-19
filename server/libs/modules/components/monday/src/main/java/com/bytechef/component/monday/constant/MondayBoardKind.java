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

package com.bytechef.component.monday.constant;

public enum MondayBoardKind {

    PRIVATE("private", "Private"),
    PUBLIC("public", "Public"),
    SHARE("share", "Share");

    private final String name;
    private final String displayValue;

    MondayBoardKind(String name, String displayValue) {
        this.name = name;
        this.displayValue = displayValue;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static MondayBoardKind getBoardKindByName(String name) {
        for (MondayBoardKind value : values()) {
            if (name.equals(value.getName())) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown board kind: " + name);
    }

}
