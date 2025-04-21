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

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Option;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class PushoverUtilsTest {

    @Test
    void getMessagePriorityOptions() {
        List<Option<String>> expectedPriorityOptions = getPriorityOptions();

        List<Option<String>> priorityOptions = PushoverUtils.getMessagePriorityOptions();

        assertEquals(expectedPriorityOptions, priorityOptions);
    }

    private List<Option<String>> getPriorityOptions() {
        return List.of(
            option("Lowest Priority", "-2"),
            option("Low Priority", "-1"),
            option("Normal Priority", "0"),
            option("High Priority", "1"),
            option("Emergency", "2"));
    }
}
