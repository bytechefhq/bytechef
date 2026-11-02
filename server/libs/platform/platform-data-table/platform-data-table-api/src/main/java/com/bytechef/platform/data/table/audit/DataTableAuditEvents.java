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

package com.bytechef.platform.data.table.audit;

/**
 * @author Ivica Cardic
 */
public final class DataTableAuditEvents {

    public static final String DATA_TABLE_COLUMN_ADDED = "DATA_TABLE_COLUMN_ADDED";
    public static final String DATA_TABLE_COLUMN_REMOVED = "DATA_TABLE_COLUMN_REMOVED";
    public static final String DATA_TABLE_COLUMN_RENAMED = "DATA_TABLE_COLUMN_RENAMED";
    public static final String DATA_TABLE_CREATED = "DATA_TABLE_CREATED";
    public static final String DATA_TABLE_DELETED = "DATA_TABLE_DELETED";
    public static final String DATA_TABLE_DUPLICATED = "DATA_TABLE_DUPLICATED";
    public static final String DATA_TABLE_RENAMED = "DATA_TABLE_RENAMED";

    private DataTableAuditEvents() {
    }
}
