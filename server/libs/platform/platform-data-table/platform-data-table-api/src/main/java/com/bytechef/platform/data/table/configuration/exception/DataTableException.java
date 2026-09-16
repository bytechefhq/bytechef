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

import com.bytechef.exception.AbstractException;

/**
 * A data-table failure a caller can act on. The {@link DataTableErrorType} key is the public contract; the message is
 * for humans.
 *
 * @author Ivica Cardic
 */
public class DataTableException extends AbstractException {

    public DataTableException(String message, DataTableErrorType errorType) {
        super(message, errorType);
    }

    public DataTableException(String message, Throwable cause, DataTableErrorType errorType) {
        super(message, cause, errorType);
    }
}
