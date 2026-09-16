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

package com.bytechef.platform.data.table.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import org.junit.jupiter.api.Test;

/**
 * A base name can never begin with a digit, which is what keeps a physical name unambiguous: {@code listTables}
 * recovers a base name by stripping {@code dt_<envId>_} from the front, and a name that could itself start with an
 * environment-shaped run of digits would give some physical names two readings.
 *
 * <p>
 * Lives beside {@link DataTableServiceImpl} (same package) rather than next to {@code PhysicalTableNamingTest} because
 * {@code validateBaseName} is intentionally not public.
 *
 * @author Ivica Cardic
 */
class DataTableServiceImplBaseNameTest {

    @Test
    void testABaseNameCannotStartWithADigit() {
        assertThatThrownBy(() -> DataTableServiceImpl.validateBaseName("5_orders"))
            .isInstanceOf(DataTableException.class);
    }
}
