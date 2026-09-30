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

package com.bytechef.platform.data.table.configuration.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableErrorTypeTest {

    @Test
    void testKeysAreStable() {
        assertThat(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey()).isEqualTo(100);
        assertThat(DataTableErrorType.DATA_TABLE_NAME_INVALID.getErrorKey()).isEqualTo(103);
        assertThat(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey()).isEqualTo(104);
        assertThat(DataTableErrorType.COLUMN_NOT_FOUND.getErrorKey()).isEqualTo(105);
        assertThat(DataTableErrorType.COLUMN_ALREADY_EXISTS.getErrorKey()).isEqualTo(106);
        assertThat(DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey()).isEqualTo(107);
        assertThat(DataTableErrorType.ROW_NOT_FOUND.getErrorKey()).isEqualTo(108);
        assertThat(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()).isEqualTo(109);
        assertThat(DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT.getErrorKey()).isEqualTo(110);
        assertThat(DataTableErrorType.ROW_EXTERNAL_ID_REQUIRED.getErrorKey()).isEqualTo(111);
        assertThat(DataTableErrorType.FILTER_INVALID.getErrorKey()).isEqualTo(112);
        assertThat(DataTableErrorType.SORT_INVALID.getErrorKey()).isEqualTo(113);
        assertThat(DataTableErrorType.BATCH_TOO_LARGE.getErrorKey()).isEqualTo(114);
        assertThat(DataTableErrorType.CSV_INVALID.getErrorKey()).isEqualTo(115);
        assertThat(DataTableErrorType.STORAGE_LIMIT_EXCEEDED.getErrorKey()).isEqualTo(116);
    }

    @Test
    void testExceptionCarriesKeyAndEntityClass() {
        DataTableException dataTableException = new DataTableException(
            "row 7 not found", DataTableErrorType.ROW_NOT_FOUND);

        assertThat(dataTableException.getErrorKey()).isEqualTo(108);
        assertThat(dataTableException.getEntityClass()).isEqualTo(DataTableErrorType.class);
        assertThat(dataTableException.getMessage()).isEqualTo("row 7 not found");
    }
}
