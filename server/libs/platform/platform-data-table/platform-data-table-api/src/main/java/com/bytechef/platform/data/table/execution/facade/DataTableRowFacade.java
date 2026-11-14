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

package com.bytechef.platform.data.table.execution.facade;

import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface DataTableRowFacade {

    int importCsv(long dataTableId, String csv, long environmentId);

    DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId);

    DataTableRow
        insertRow(long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId);

    List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId);

    DataTableRow updateRow(long dataTableId, long id, Map<String, Object> values, long environmentId);

    DataTableRow updateRow(
        long dataTableId, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId);

    UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId);
}
