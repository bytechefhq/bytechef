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

package com.bytechef.component.notion.util;

/**
 * @author Monika Kušter
 */
public enum NotionBlockType {

    BOOKMARK("bookmark"),
    BREADCRUMB("breadcrumb"),
    BULLETED_LIST_ITEM("bulleted_list_item"),
    CALLOUT("callout"),
    CHILD_DATABASE("child_database"),
    CHILD_PAGE("child_page"),
    CODE("code"),
    COLUMN_LIST("column_list"),
    DIVIDER("divider"),
    EMBED("embed"),
    EQUATION("equation"),
    FILE("file"),
    HEADING_1("heading_1"),
    HEADING_2("heading_2"),
    HEADING_3("heading_3"),
    IMAGE("image"),
    LINK_PREVIEW("link_preview"),
    NUMBERED_LIST_ITEM("numbered_list_item"),
    PARAGRAPH("paragraph"),
    PDF("pdf"),
    QUOTE("quote"),
    SYNCED_BLOCK("synced_block"),
    TABLE("table"),
    TABLE_OF_CONTENTS("table_of_contents"),
    TABLE_ROW("table_row"),
    TO_DO("to_do"),
    TOGGLE("toggle"),
    VIDEO("video");

    private final String name;

    NotionBlockType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static NotionBlockType getPropertyTypeByName(String name) {
        for (NotionBlockType value : values()) {
            if (name.equals(value.getName())) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown column type: " + name);
    }
}
