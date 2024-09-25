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

package com.bytechef.embedded.unified.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link CursorPageSlice}
 *
 * @author Davide Pedone
 */
class CursorPageSliceTest {

    @Test
    @DisplayName("Reject null content")
    void rejectNullContent() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CursorPageSlice<>(null, 0, "notarealtoken");
        });
    }

    @Test
    @DisplayName("Provide proper information when continuationToken is null")
    void handleNullToken() {
        CursorPageSlice<String> cursorPageSlice = new CursorPageSlice<>(Arrays.asList("1", "2", "3"),
            10, null);

        assertNull(cursorPageSlice.getContinuationToken());
        assertEquals(3, cursorPageSlice.getNumberOfElements());
        assertFalse(cursorPageSlice.hasNext());
        assertTrue(cursorPageSlice.hasContent());
        assertEquals(Arrays.asList("1", "2", "3"), cursorPageSlice.getContent());
        assertEquals(10, cursorPageSlice.getSize());
        assertNotNull(cursorPageSlice.iterator());
    }
}
