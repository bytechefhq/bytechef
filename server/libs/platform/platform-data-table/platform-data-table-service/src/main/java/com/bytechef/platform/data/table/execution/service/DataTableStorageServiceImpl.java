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

package com.bytechef.platform.data.table.execution.service;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.internal.DataTableDialect;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class DataTableStorageServiceImpl implements DataTableStorageService {

    static final String TABLE_NAMES_SQL =
        "SELECT tablename FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    static final String USAGE_SQL =
        "SELECT COALESCE(SUM(pg_total_relation_size(" +
            "(quote_ident(current_schema()) || '.' || quote_ident(tablename))::regclass)), 0) " +
            "FROM pg_tables WHERE schemaname = current_schema() AND tablename LIKE 'dt\\_%' ESCAPE '\\'";

    private static final Pattern PHYSICAL_TABLE_NAME_PATTERN = Pattern.compile("dt_[a-z0-9_]+");

    private final ApplicationProperties applicationProperties;
    private final DataTableDialect dataTableDialect = new DataTableDialect();
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI")
    public DataTableStorageServiceImpl(ApplicationProperties applicationProperties, JdbcTemplate jdbcTemplate) {
        this.applicationProperties = applicationProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DataTableStorageUsage getUsage() {
        long limit = getLimit();
        long used = getUsedBytes(limit, 0);

        boolean unlimited = limit <= 0;
        double percentage = unlimited ? 0.0 : used * 100.0 / limit;

        return new DataTableStorageUsage(used, limit, percentage, unlimited);
    }

    @Override
    public long getTableBytes(String physicalTableName) {
        if (dataTableDialect.isH2(jdbcTemplate)) {
            return 0;
        }

        return liveTableBytes(physicalTableName);
    }

    @Override
    public void checkWithinLimit(long incomingBytes) {
        long limit = getLimit();

        if (limit <= 0) {
            return;
        }

        long used = getUsedBytes(limit, incomingBytes);

        if (used + incomingBytes > limit) {
            throw new DataTableStorageLimitExceededException(used, limit);
        }
    }

    private long getLimit() {
        return applicationProperties.getDataTable()
            .getMaxSizeBytes();
    }

    private long getUsedBytes(long limit, long incomingBytes) {
        long allocatedBytes = allocatedUsageBytes();

        if (limit <= 0 || allocatedBytes + incomingBytes <= limit) {
            return allocatedBytes;
        }

        return Math.min(allocatedBytes, liveUsageBytes());
    }

    private long allocatedUsageBytes() {
        if (dataTableDialect.isH2(jdbcTemplate)) {
            return 0;
        }

        Long used = jdbcTemplate.queryForObject(USAGE_SQL, Long.class);

        return used == null ? 0 : used;
    }

    private long liveUsageBytes() {
        List<String> physicalTableNames = jdbcTemplate.queryForList(TABLE_NAMES_SQL, String.class);

        return physicalTableNames.stream()
            .mapToLong(this::liveTableBytes)
            .sum();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private long liveTableBytes(String physicalTableName) {
        if (!PHYSICAL_TABLE_NAME_PATTERN.matcher(physicalTableName)
            .matches()) {

            throw new IllegalArgumentException("Invalid data table name: " + physicalTableName);
        }

        Long liveBytes = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(pg_column_size(t.*)), 0) FROM \"" + physicalTableName + "\" t", Long.class);

        return liveBytes == null ? 0 : liveBytes;
    }
}
