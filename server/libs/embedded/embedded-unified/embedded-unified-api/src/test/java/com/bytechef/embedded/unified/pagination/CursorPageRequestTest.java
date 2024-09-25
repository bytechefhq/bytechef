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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * Unit test for {@link CursorPageRequest}
 *
 * @author Davide Pedone
 */
class CursorPageRequestTest {

    @Test
    void allowNullContinuationToken() {
        CursorPageRequest cursorPageRequest = new CursorPageRequest() {};

        cursorPageRequest.setContinuationToken(null);

        assertNull(cursorPageRequest.getContinuationToken());
    }

    @Test
    void allowNullSortField() {
        CursorPageRequest cursorPageRequest = new CursorPageRequest() {};

        cursorPageRequest.setSort(null);

        assertNull(cursorPageRequest.getSort());
    }

    @Test
    void ofSize() {
        CursorPageRequest cursorPageRequest = CursorPageRequest.of(10);

        assertNull(cursorPageRequest.getSort());
        assertNull(cursorPageRequest.getContinuationToken());
        assertEquals(Sort.Direction.DESC, cursorPageRequest.getDirection());
        assertEquals(10, cursorPageRequest.getSize());
    }

    @Test
    void ofTokenAndSize() {
        CursorPageRequest cursorPageRequest = CursorPageRequest.of("testToken", 10);

        assertNull(cursorPageRequest.getSort());
        assertEquals(Sort.Direction.DESC, cursorPageRequest.getDirection());
        assertEquals(10, cursorPageRequest.getSize());
        assertEquals("testToken", cursorPageRequest.getContinuationToken());
    }

    @Test
    void ofSizeAndSortAndDirection() {
        CursorPageRequest cursorPageRequest = CursorPageRequest.of(11, "sortBy", Sort.Direction.ASC);

        assertNull(cursorPageRequest.getContinuationToken());
        assertEquals("sortBy", cursorPageRequest.getSort());
        assertEquals(Sort.Direction.ASC, cursorPageRequest.getDirection());
        assertEquals(11, cursorPageRequest.getSize());
    }

    @Test
    void ofTokenAndSizeAndSort() {
        CursorPageRequest cursorPageRequest = CursorPageRequest.of("testToken", 12, "sortBy");

        assertEquals("testToken", cursorPageRequest.getContinuationToken());
        assertEquals("sortBy", cursorPageRequest.getSort());
        assertEquals(Sort.Direction.DESC, cursorPageRequest.getDirection());
        assertEquals(12, cursorPageRequest.getSize());
    }

    @Test
    void ofTokenAndSizeAndSortAndDirection() {
        CursorPageRequest cursorPageRequest = CursorPageRequest.of("testToken", 12, "sortBy", Sort.Direction.ASC);

        assertEquals("testToken", cursorPageRequest.getContinuationToken());
        assertEquals("sortBy", cursorPageRequest.getSort());
        assertEquals(Sort.Direction.ASC, cursorPageRequest.getDirection());
        assertEquals(12, cursorPageRequest.getSize());
    }
}
