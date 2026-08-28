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

import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Column names the platform owns on every {@code dt_*} physical table. A reserved name cannot be created, cannot be
 * renamed, cannot be renamed to, and is filtered out of every column listing so it never reaches the grid, the
 * generated row schema, or a CSV round trip.
 *
 * @author Ivica Cardic
 */
public final class ReservedColumns {

    public static final String ID = "id";
    public static final String OWNER_ID = "owner_id";
    public static final String OWNER_TYPE = "owner_type";

    private static final Set<String> ALL = Set.of(ID, OWNER_ID, OWNER_TYPE);
    private static final Set<String> HIDDEN = Set.of(OWNER_ID, OWNER_TYPE);

    private ReservedColumns() {
    }

    public static Set<String> all() {
        return ALL;
    }

    public static boolean isReserved(@Nullable String columnName) {
        if (columnName == null) {
            return false;
        }

        return ALL.contains(columnName.toLowerCase(Locale.ROOT));
    }

    /**
     * The narrower predicate: a hidden column is one a caller may not even name. {@code id} is reserved but not hidden
     * -- it is returned on every row and taken by {@code getRow}/{@code updateRow}/{@code deleteRow}, so a query may
     * filter and sort on it. The owner columns are the platform's alone.
     */
    public static boolean isHidden(@Nullable String columnName) {
        if (columnName == null) {
            return false;
        }

        return HIDDEN.contains(columnName.toLowerCase(Locale.ROOT));
    }
}
