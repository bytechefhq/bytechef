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

    private DataTableErrorType(int errorKey) {
        super(DataTableErrorType.class, errorKey);
    }
}
