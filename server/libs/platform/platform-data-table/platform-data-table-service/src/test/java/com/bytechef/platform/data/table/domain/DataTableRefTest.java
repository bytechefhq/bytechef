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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * The ref is the only thing that names a physical table, so the base name it accepts is what every generated
 * statement's identifier allowlist rests on.
 *
 * @author Ivica Cardic
 */
class DataTableRefTest {

    private static final long ENVIRONMENT_ID = 0L;

    @Test
    void testThePhysicalNameIsThePrefixAndTheBaseName() {
        DataTableRef dataTableRef = new DataTableRef("orders", ENVIRONMENT_ID);

        assertThat(dataTableRef.physicalName()).isEqualTo("dt_0_orders");
    }

    @Test
    void testAMixedCaseBaseNameIsLowercased() {
        DataTableRef dataTableRef = new DataTableRef("Orders", ENVIRONMENT_ID);

        assertThat(dataTableRef.baseName()).isEqualTo("orders");
        assertThat(dataTableRef.physicalName()).isEqualTo("dt_0_orders");
    }

    /**
     * Postgres truncates an over-long identifier rather than refusing it, so two tables whose names agree in their
     * first 63 bytes would silently become one table. A long enough base name is all it takes.
     */
    @Test
    void testANameThatWouldTruncateIsRefused() {
        assertThat(new DataTableRef("a".repeat(50), ENVIRONMENT_ID)
            .physicalName())
                .hasSizeLessThanOrEqualTo(63);

        assertThatThrownBy(
            () -> new DataTableRef("a".repeat(70), ENVIRONMENT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds 63 bytes");
    }

    @Test
    void testABaseNameSpelledAsAPhysicalNameIsRefused() {
        assertThatThrownBy(() -> new DataTableRef("dt_0_orders", ENVIRONMENT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not start with 'dt_'");
    }

    @Test
    void testABaseNameOutsideTheIdentifierAllowlistIsRefused() {
        assertThatThrownBy(() -> new DataTableRef("orders; DROP TABLE x", ENVIRONMENT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid base name");
    }
}
