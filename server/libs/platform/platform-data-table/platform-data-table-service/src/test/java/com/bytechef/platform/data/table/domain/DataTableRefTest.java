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
 * The ref is the only thing that names a physical table, so what it accepts is what every generated statement's
 * identifier rests on.
 *
 * @author Ivica Cardic
 */
class DataTableRefTest {

    @Test
    void testPhysicalNameIsTheEnvironmentAndTheId() {
        DataTableRef dataTableRef = new DataTableRef(1051L, 0L);

        assertThat(dataTableRef.physicalName()).isEqualTo("dt_0_1051");
    }

    @Test
    void testRejectsANonPositiveId() {
        assertThatThrownBy(() -> new DataTableRef(0L, 0L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsANegativeEnvironment() {
        assertThatThrownBy(() -> new DataTableRef(1051L, -1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
