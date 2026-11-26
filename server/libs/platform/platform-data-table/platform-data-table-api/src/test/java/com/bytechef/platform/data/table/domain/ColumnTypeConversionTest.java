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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Ivica Cardic
 */
class ColumnTypeConversionTest {

    @ParameterizedTest
    @CsvSource({
        "STRING, STRING", "INTEGER, STRING", "NUMBER, STRING", "BOOLEAN, STRING", "DATE, STRING",
        "DATE_TIME, STRING", "BOOLEAN, INTEGER", "INTEGER, NUMBER", "BOOLEAN, NUMBER", "DATE, DATE_TIME",
        "LINK, LINK"
    })
    void testConversionIsLossless(ColumnType fromColumnType, ColumnType toColumnType) {
        assertThat(ColumnTypeConversion.of(fromColumnType, toColumnType)).isEqualTo(ColumnTypeConversion.LOSSLESS);
    }

    @ParameterizedTest
    @CsvSource({
        "NUMBER, INTEGER", "STRING, INTEGER", "STRING, NUMBER", "STRING, BOOLEAN", "INTEGER, BOOLEAN",
        "NUMBER, BOOLEAN", "STRING, DATE", "DATE_TIME, DATE", "STRING, DATE_TIME"
    })
    void testConversionIsLossy(ColumnType fromColumnType, ColumnType toColumnType) {
        assertThat(ColumnTypeConversion.of(fromColumnType, toColumnType)).isEqualTo(ColumnTypeConversion.LOSSY);
    }

    @ParameterizedTest
    @CsvSource({
        "DATE, INTEGER", "DATE_TIME, INTEGER", "DATE, NUMBER", "DATE_TIME, NUMBER", "DATE, BOOLEAN",
        "DATE_TIME, BOOLEAN", "INTEGER, DATE", "NUMBER, DATE", "BOOLEAN, DATE", "INTEGER, DATE_TIME",
        "NUMBER, DATE_TIME", "BOOLEAN, DATE_TIME", "LINK, STRING", "STRING, LINK", "INTEGER, LINK"
    })
    void testConversionIsUnsupported(ColumnType fromColumnType, ColumnType toColumnType) {
        assertThat(ColumnTypeConversion.of(fromColumnType, toColumnType))
            .isEqualTo(ColumnTypeConversion.UNSUPPORTED);
    }
}
