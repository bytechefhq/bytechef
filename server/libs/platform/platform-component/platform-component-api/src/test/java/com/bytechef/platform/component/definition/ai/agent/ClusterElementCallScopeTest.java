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

package com.bytechef.platform.component.definition.ai.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallScopeTest {

    @Test
    void testCallWithBindsAndRestores() {
        ClusterElementCallRef outer = new ClusterElementCallRef("outer", "a_1");
        ClusterElementCallRef inner = new ClusterElementCallRef("inner", "b_1");

        assertNull(ClusterElementCallScope.current());

        String result = ClusterElementCallScope.callWith(outer, () -> {
            assertSame(outer, ClusterElementCallScope.current());

            ClusterElementCallScope.callWith(inner, () -> {
                assertSame(inner, ClusterElementCallScope.current());

                return null;
            });

            assertSame(outer, ClusterElementCallScope.current());

            ClusterElementCallScope.callWith(null, () -> {
                assertNull(ClusterElementCallScope.current());

                return null;
            });

            return "done";
        });

        assertEquals("done", result);
        assertNull(ClusterElementCallScope.current());

        assertThrows(IllegalStateException.class, () -> ClusterElementCallScope.callWith(outer, () -> {
            throw new IllegalStateException("boom");
        }));

        assertNull(ClusterElementCallScope.current());
    }
}
