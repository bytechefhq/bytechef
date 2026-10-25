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

import com.bytechef.exception.AbstractErrorType;

/**
 * @author Igor Beslic
 */
public class DataTableErrorType extends AbstractErrorType {

    public static final DataTableErrorType DATA_TABLE_NOT_FOUND = new DataTableErrorType(100);
    public static final DataTableErrorType DATA_TABLE_NOT_CREATED = new DataTableErrorType(101);
    public static final DataTableErrorType DATA_TABLE_NOT_DUPLICATED = new DataTableErrorType(102);
    public static final DataTableErrorType DATA_TABLE_NAME_INVALID = new DataTableErrorType(103);
    public static final DataTableErrorType DATA_TABLE_ALREADY_EXISTS = new DataTableErrorType(104);
    public static final DataTableErrorType COLUMN_NOT_FOUND = new DataTableErrorType(105);
    public static final DataTableErrorType COLUMN_ALREADY_EXISTS = new DataTableErrorType(106);
    public static final DataTableErrorType COLUMN_NAME_INVALID = new DataTableErrorType(107);
    public static final DataTableErrorType ROW_NOT_FOUND = new DataTableErrorType(108);
    public static final DataTableErrorType ROW_VALUE_INVALID = new DataTableErrorType(109);
    public static final DataTableErrorType ROW_EXTERNAL_ID_CONFLICT = new DataTableErrorType(110);
    public static final DataTableErrorType ROW_EXTERNAL_ID_REQUIRED = new DataTableErrorType(111);
    public static final DataTableErrorType FILTER_INVALID = new DataTableErrorType(112);
    public static final DataTableErrorType SORT_INVALID = new DataTableErrorType(113);
    public static final DataTableErrorType BATCH_TOO_LARGE = new DataTableErrorType(114);
    public static final DataTableErrorType CSV_INVALID = new DataTableErrorType(115);
    public static final DataTableErrorType STORAGE_LIMIT_EXCEEDED = new DataTableErrorType(116);

    private DataTableErrorType(int errorKey) {
        super(DataTableErrorType.class, errorKey);
    }
}
