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

package com.bytechef.component.neon.util;

import static com.bytechef.component.neon.constant.NeonConstants.FILTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NeonUtilsTest {

    @Test
    void testGetFilterQueryParameters() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(FILTERS, List.of("status=eq.active", "price=gt.10", "price=lt.100")));

        assertEquals(
            Map.of(
                "status", List.of("eq.active"),
                "price", List.of("gt.10", "lt.100")),
            NeonUtils.getFilterQueryParameters(inputParameters));
    }

    @Test
    void testGetFilterQueryParametersWithNoFilters() {
        Parameters inputParameters = MockParametersFactory.create(Map.of());

        assertEquals(Map.of(), NeonUtils.getFilterQueryParameters(inputParameters));
    }

    @Test
    void testGetFilterQueryParametersWithInvalidFilter() {
        Parameters inputParameters = MockParametersFactory.create(Map.of(FILTERS, List.of("invalid-filter")));

        assertThrows(IllegalArgumentException.class, () -> NeonUtils.getFilterQueryParameters(inputParameters));
    }

    @Test
    void testGetFirstRowWithList() {
        assertEquals("row", NeonUtils.getFirstRow(List.of("row", "other")));
    }

    @Test
    void testGetFirstRowWithEmptyList() {
        assertEquals(null, NeonUtils.getFirstRow(List.of()));
    }

    @Test
    void testGetFirstRowWithNonList() {
        assertEquals("row", NeonUtils.getFirstRow("row"));
    }
}
