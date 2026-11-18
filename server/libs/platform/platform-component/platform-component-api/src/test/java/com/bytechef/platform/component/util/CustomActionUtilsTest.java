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

package com.bytechef.platform.component.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CustomActionUtilsTest {

    @Test
    void testToValueListsAcceptsTheDeclaredArrayValues() {
        Map<String, List<String>> valueLists = CustomActionUtils.toValueLists(
            Map.of("pageSize", List.of("50"), "fields", List.of("Name", "Status")));

        assertThat(valueLists)
            .containsEntry("pageSize", List.of("50"))
            .containsEntry("fields", List.of("Name", "Status"));
    }

    @Test
    void testToValueListsAcceptsSingleValues() {
        Map<String, List<String>> valueLists = CustomActionUtils.toValueLists(
            Map.of("Accept", "application/json", "limit", 10));

        assertThat(valueLists)
            .containsEntry("Accept", List.of("application/json"))
            .containsEntry("limit", List.of("10"));
    }

    @Test
    void testToValueListsSkipsMissingValues() {
        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("offset", null);
        parameters.put("sort", Arrays.asList(null, "Name"));
        parameters.put("empty", Arrays.asList((Object) null));

        assertThat(CustomActionUtils.toValueLists(parameters))
            .containsOnlyKeys("sort")
            .containsEntry("sort", List.of("Name"));
    }
}
