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

package com.bytechef.component.pushover.util;

/**
 * @author Nikolina Spehar
 */
public enum PushoverMessagePriority {

    LOWEST("Lowest Priority", "-2"),
    LOW("Low Priority", "-1"),
    NORMAL("Normal Priority", "0"),
    HIGH("High Priority", "1"),
    EMERGENCY("Emergency", "2");

    private final String label;
    private final String value;

    PushoverMessagePriority(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

}
