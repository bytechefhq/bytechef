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

package com.bytechef.component.neon.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;

public class NeonConstants {

    public static final String FILTERS = "filters";
    public static final String KEY_ID = "keyId";
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";
    public static final String ORDER = "order";
    public static final String ORDER_BY_COLUMN = "orderByColumn";
    public static final String ORDER_DIRECTION = "orderDirection";
    public static final String PRIVATE_KEY = "privateKey";
    public static final String ROW_DATA = "rowData";
    public static final String SELECT = "select";
    public static final String SUBJECT = "subject";
    public static final String TABLE = "table";

    private NeonConstants() {
    }

    public static ModifiableArrayProperty criteriaFilterArrayProperty(boolean required) {
        return array(FILTERS)
            .label("Filters")
            .description(
                "Column filters using PostgREST operator syntax, one per entry, e.g. \"id=eq.1\" or " +
                    "\"price=gt.10\". Multiple entries for the same column are combined.")
            .placeholder("Add filter")
            .items(string())
            .required(required);
    }
}
