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

public enum MondayColumnType {
    AUTO_NUMBER("auto_number", "Auto number"),
    BOARD_RELATION("board_relation", "Board Relation"),
    BUTTON("button", "Button"),
    CHECKBOX("checkbox", "Checkbox"),
    COLOR_PICKER("color_picker", "Color Picker"),
    COUNTRY("country", "Country"),
    CREATION_LOG("creation_log", "Creation Log"),
    DATE("date", "Date"),
    DEPENDENCY("dependency", "Dependency"),
    DOC("doc", "Doc"),
    DROPDOWN("dropdown", "Dropdown"),
    EMAIL("email", "Email"),
    FILE("file", "File"),
    FORMULA("formula", "Formula"),
    HOUR("hour", "Hour"),
    ITEM_ASSIGNEES("item_assignees", "Item Assignees"),
    ITEM_ID("item_id", "Item ID"),
    LAST_UPDATE("last_updated", "Last Updated"),
    LINK("link", "Link"),
    LOCATION("location", "Location"),
    LONG_TEXT("long_text", "Long Text"),
    MIRROR("mirror", "Mirror"),
    NAME("name", "Name"),
    NUMBERS("numbers", "Numbers"),
    PEOPLE("people", "People"),
    PHONE("phone", "Phone"),
    PROGRESS("progress", "Progress"),
    RATING("rating", "Rating"),
    STATUS("status", "Status"),
    SUBTASKS("subtasks", "Subtasks"),
    TAGS("tags", "Tags"),
    TEAM("team", "Team"),
    TEXT("text", "Text"),
    TIMELINE("timeline", "Timeline"),
    TIME_TRACKING("time_tracking", "Time Tracking"),
    VOTE("vote", "Vote"),
    WEEK("week", "Week"),
    WORLD_CLOCK("world_clock", "World Clock"),
    UNSUPPORTED("unsupported", "Unsupported");

    private final String name;
    private final String displayValue;

    MondayColumnType(String name, String displayValue) {
        this.name = name;
        this.displayValue = displayValue;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static MondayColumnType getColumnTypeByName(String name) {
        for (MondayColumnType value : values()) {
            if (name.equals(value.getName())) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown column type: " + name);
    }
}
