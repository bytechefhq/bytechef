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

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class DataTableNames {

    private static final int MAX_NAME_LENGTH = 256;

    private DataTableNames() {
    }

    public static String normalize(@Nullable String name) {
        if (name == null || name.isBlank()) {
            throw new DataTableException("Data table name must not be empty",
                DataTableErrorType.DATA_TABLE_NAME_INVALID);
        }

        if (name.length() > MAX_NAME_LENGTH) {
            throw new DataTableException(
                "Data table name must not be longer than " + MAX_NAME_LENGTH + " characters",
                DataTableErrorType.DATA_TABLE_NAME_INVALID);
        }

        String normalizedName = name.toLowerCase(Locale.ROOT);

        if (normalizedName.startsWith("dt_") || !normalizedName.matches("[a-z_][a-z0-9_]*")) {
            throw new DataTableException("Invalid data table name: " + name,
                DataTableErrorType.DATA_TABLE_NAME_INVALID);
        }

        return normalizedName;
    }
}
