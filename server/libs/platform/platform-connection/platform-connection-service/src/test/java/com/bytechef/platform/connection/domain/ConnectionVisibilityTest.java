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

package com.bytechef.platform.connection.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionVisibilityTest {

    /**
     * {@link ConnectionVisibility} is persisted as an INT ordinal on {@code connection.visibility}. Reordering the enum
     * constants or inserting a new value anywhere other than the end would silently corrupt every row on upgrade. This
     * test pins the ordinals so any accidental reorder fails loud at build time — new values MUST be appended at the
     * end of the enum declaration.
     */
    @Test
    void testConnectionVisibilityOrdinalsAreStableForJdbcPersistence() {
        assertEquals(0, ConnectionVisibility.PRIVATE.ordinal());
        assertEquals(1, ConnectionVisibility.PROJECT.ordinal());
        assertEquals(2, ConnectionVisibility.WORKSPACE.ordinal());
        assertEquals(3, ConnectionVisibility.ORGANIZATION.ordinal());
    }

    @Test
    void testConnectionVisibilityNames() {
        assertEquals("PRIVATE", ConnectionVisibility.PRIVATE.name());
        assertEquals("PROJECT", ConnectionVisibility.PROJECT.name());
        assertEquals("WORKSPACE", ConnectionVisibility.WORKSPACE.name());
    }

    @Test
    void testConnectionVisibilityValueOf() {
        assertEquals(ConnectionVisibility.PRIVATE, ConnectionVisibility.valueOf("PRIVATE"));
        assertEquals(ConnectionVisibility.PROJECT, ConnectionVisibility.valueOf("PROJECT"));
        assertEquals(ConnectionVisibility.WORKSPACE, ConnectionVisibility.valueOf("WORKSPACE"));
    }

    @Test
    void testConnectionVisibilityValues() {
        ConnectionVisibility[] values = ConnectionVisibility.values();

        assertEquals(4, values.length);
        assertEquals(ConnectionVisibility.PRIVATE, values[0]);
        assertEquals(ConnectionVisibility.PROJECT, values[1]);
        assertEquals(ConnectionVisibility.WORKSPACE, values[2]);
        assertEquals(ConnectionVisibility.ORGANIZATION, values[3]);
    }

}
