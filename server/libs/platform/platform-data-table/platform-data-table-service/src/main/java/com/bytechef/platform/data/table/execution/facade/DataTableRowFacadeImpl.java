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

import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.data.table.execution.service.DataTableStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableRowFacadeImpl implements DataTableRowFacade {

    private static final int ESTIMATED_ROW_OVERHEAD_BYTES = 64;

    private final DataTableRowService dataTableRowService;
    private final DataTableStorageService dataTableStorageService;

    @SuppressFBWarnings("EI")
    public DataTableRowFacadeImpl(
        DataTableRowService dataTableRowService, DataTableStorageService dataTableStorageService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableStorageService = dataTableStorageService;
    }

    @Override
    public void importCsv(String baseName, String csv, long environmentId) {
        dataTableStorageService.checkLimit(csv == null ? 0 : csv.getBytes(StandardCharsets.UTF_8).length);

        dataTableRowService.importCsv(baseName, csv, environmentId);
    }

    @Override
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        dataTableStorageService.checkLimit(ESTIMATED_ROW_OVERHEAD_BYTES + getEstimatedValuesBytes(values));

        return dataTableRowService.insertRow(baseName, values, environmentId);
    }

    @Override
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        long incomingBytes = getEstimatedValuesBytes(values);

        try {
            dataTableStorageService.checkLimit(incomingBytes);
        } catch (DataTableStorageLimitExceededException dataTableStorageLimitExceededException) {
            DataTableRow currentDataTableRow = dataTableRowService.getRow(baseName, id, environmentId);

            if (currentDataTableRow == null) {
                throw dataTableStorageLimitExceededException;
            }

            Map<String, Object> currentValues = new HashMap<>();

            for (String columnName : values.keySet()) {
                currentValues.put(columnName, getValue(currentDataTableRow.values(), columnName));
            }

            long growthBytes = incomingBytes - getEstimatedValuesBytes(currentValues);

            if (growthBytes > 0) {
                dataTableStorageService.checkLimit(growthBytes);
            }
        }

        return dataTableRowService.updateRow(baseName, id, values, environmentId);
    }

    private static long getEstimatedValuesBytes(Map<String, Object> values) {
        long estimatedBytes = 0;

        for (Object value : values.values()) {
            if (value != null) {
                String stringValue = String.valueOf(value);

                estimatedBytes += stringValue.getBytes(StandardCharsets.UTF_8).length;
            }
        }

        return estimatedBytes;
    }

    private static Object getValue(Map<String, Object> values, String columnName) {
        if (values.containsKey(columnName)) {
            return values.get(columnName);
        }

        String lowerCaseColumnName = columnName.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();

            if (lowerCaseColumnName.equals(key.toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }

        return null;
    }
}
