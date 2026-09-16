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

package com.bytechef.platform.data.table.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PhysicalTableNamingTest {

    @Test
    void testThePhysicalNameIsThePrefixAndTheBaseName() {
        assertThat(PhysicalTableNaming.buildPhysicalName(0, "orders")).isEqualTo("dt_0_orders");
        assertThat(PhysicalTableNaming.buildPhysicalName(2, "orders")).isEqualTo("dt_2_orders");
    }

    /**
     * The {@code LIKE} scan that finds every physical instance of a base name is built from this prefix, so it has to
     * be everything up to the base name and nothing more.
     */
    @Test
    void testThePrefixIsEverythingBeforeTheBaseName() {
        assertThat(PhysicalTableNaming.prefix(1)).isEqualTo("dt_1_");
        assertThat(PhysicalTableNaming.buildPhysicalName(1, "orders"))
            .startsWith(PhysicalTableNaming.prefix(1));
    }

    @Test
    void testAMixedCaseBaseNameIsLowercased() {
        assertThat(PhysicalTableNaming.buildPhysicalName(0, "Orders")).isEqualTo("dt_0_orders");
    }
}
