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

package com.bytechef.platform.data.table.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ReservedColumnsTest {

    @Test
    void testIsReservedMatchesIdCaseInsensitively() {
        assertTrue(ReservedColumns.isReserved("id"));
        assertTrue(ReservedColumns.isReserved("ID"));
        assertTrue(ReservedColumns.isReserved("Id"));
    }

    @Test
    void testIsReservedRejectsOrdinaryColumnNames() {
        assertFalse(ReservedColumns.isReserved("identifier"));
        assertFalse(ReservedColumns.isReserved("name"));
    }

    @Test
    void testIsReservedTreatsNullAsNotReserved() {
        assertFalse(ReservedColumns.isReserved(null));
    }

    @Test
    void testIsReservedMatchesOwnerColumns() {
        assertTrue(ReservedColumns.isReserved("owner_id"));
        assertTrue(ReservedColumns.isReserved("OWNER_TYPE"));
    }

    @Test
    void testAllReturnsEveryReservedName() {
        assertEquals(Set.of("id", "owner_id", "owner_type"), ReservedColumns.all());
    }
}
