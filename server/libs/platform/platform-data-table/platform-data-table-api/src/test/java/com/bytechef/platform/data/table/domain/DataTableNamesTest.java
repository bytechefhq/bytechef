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

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableNamesTest {

    @Test
    void testNormalizeLowercasesAValidName() {
        assertThat(DataTableNames.normalize("Orders_2026")).isEqualTo("orders_2026");
    }

    @Test
    void testNormalizeRejectsABlankName() {
        assertThatThrownBy(() -> DataTableNames.normalize(" "))
            .isInstanceOf(DataTableException.class)
            .extracting("errorKey")
            .isEqualTo(DataTableErrorType.DATA_TABLE_NAME_INVALID.getErrorKey());
    }

    @Test
    void testNormalizeRejectsTheReservedPrefix() {
        assertThatThrownBy(() -> DataTableNames.normalize("dt_orders"))
            .isInstanceOf(DataTableException.class);
    }

    @Test
    void testNormalizeRejectsANameStartingWithADigit() {
        assertThatThrownBy(() -> DataTableNames.normalize("5_orders"))
            .isInstanceOf(DataTableException.class);
    }

    @Test
    void testNormalizeAcceptsANameOfTheMaximumLength() {
        String name = "a".repeat(256);

        assertThat(DataTableNames.normalize(name)).isEqualTo(name);
    }

    @Test
    void testNormalizeRejectsANameLongerThanTheMaximumLength() {
        assertThatThrownBy(() -> DataTableNames.normalize("a".repeat(257)))
            .isInstanceOf(DataTableException.class)
            .extracting("errorKey")
            .isEqualTo(DataTableErrorType.DATA_TABLE_NAME_INVALID.getErrorKey());
    }

    @Test
    void testNormalizeRejectsANonIdentifierName() {
        assertThatThrownBy(() -> DataTableNames.normalize("my orders"))
            .isInstanceOf(DataTableException.class);
    }
}
